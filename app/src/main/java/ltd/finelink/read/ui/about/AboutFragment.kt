package ltd.finelink.read.ui.about

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ltd.finelink.read.R
import ltd.finelink.read.constant.AppConst.appInfo
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.help.AppUpdate
import ltd.finelink.read.help.CrashHandler
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.help.coroutine.Coroutine
import ltd.finelink.read.ui.widget.dialog.TextDialog
import ltd.finelink.read.ui.widget.dialog.WaitDialog
import ltd.finelink.read.utils.FileDoc
import ltd.finelink.read.utils.createFileIfNotExist
import ltd.finelink.read.utils.createFolderIfNotExist
import ltd.finelink.read.utils.delete
import ltd.finelink.read.utils.find
import ltd.finelink.read.utils.list
import ltd.finelink.read.utils.openInputStream
import ltd.finelink.read.utils.openOutputStream
import ltd.finelink.read.utils.openUrl
import ltd.finelink.read.utils.sendMail
import ltd.finelink.read.utils.sendToClip
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.toastOnUi
import splitties.init.appCtx
import java.io.File

class AboutFragment : PreferenceFragmentCompat() {

    private val waitDialog by lazy {
        WaitDialog(requireContext())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)
        findPreference<Preference>("update_log")?.summary =
            "${getString(R.string.version)} ${appInfo.versionName}"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "update_log" -> showMdFile(getString(R.string.update_log), "updateLog.md")
            "check_update" -> checkUpdate()
            "mail" -> requireContext().sendMail(getString(R.string.email))
            "disclaimer" -> showMdFile(getString(R.string.disclaimer), getString(R.string.disclaimer_file))
            "privacyPolicy" -> showMdFile(getString(R.string.privacy_policy), getString(R.string.privacy_policy_file))
            "gzGzh" -> requireContext().sendToClip(getString(R.string.legado_gzh))
            "crashLog" -> showDialogFragment<CrashLogsDialog>()
            "saveLog" -> saveLog()
            "createHeapDump" -> createHeapDump()
        }
        return super.onPreferenceTreeClick(preference)
    }

    @Suppress("SameParameterValue")
    private fun openUrl(@StringRes addressID: Int) {
        requireContext().openUrl(getString(addressID))
    }

    /**
     * 显示md文件
     */
    private fun showMdFile(title: String, fileName: String) {
        val mdText = String(requireContext().assets.open(fileName).readBytes())
        showDialogFragment(TextDialog(title, mdText, TextDialog.Mode.MD))
    }

    /**
     * 检测更新
     */
    private fun checkUpdate() {
        waitDialog.show()
        AppUpdate.gitHubUpdate?.run {
            check(lifecycleScope)
                .onSuccess {
                    showDialogFragment(
                        UpdateDialog(it)
                    )
                }.onError {
                    appCtx.toastOnUi("${getString(R.string.check_update)}\n${it.localizedMessage}")
                }.onFinally {
                    waitDialog.dismiss()
                }
        }
    }



    private fun saveLog() {
        Coroutine.async {
            val backupPath = AppConfig.backupPath ?: let {
                appCtx.toastOnUi("未设置备份目录")
                return@async
            }
            val doc = FileDoc.fromUri(Uri.parse(backupPath), true)
            copyLogs(doc)
            copyHeapDump(doc)
            appCtx.toastOnUi("已保存至备份目录")
        }.onError {
            AppLog.put("保存日志出错\n${it.localizedMessage}", it, true)
        }
    }

    private fun createHeapDump() {
        Coroutine.async {
            val backupPath = AppConfig.backupPath ?: let {
                appCtx.toastOnUi("未设置备份目录")
                return@async
            }
            appCtx.toastOnUi("开始创建堆转储")
            System.gc()
            CrashHandler.doHeapDump()
            val doc = FileDoc.fromUri(Uri.parse(backupPath), true)
            if (!copyHeapDump(doc)) {
                appCtx.toastOnUi("未找到堆转储文件")
            } else {
                appCtx.toastOnUi("已保存至备份目录")
            }
        }.onError {
            AppLog.put("保存堆转储失败\n${it.localizedMessage}", it)
        }
    }

    private suspend fun copyLogs(doc: FileDoc) = coroutineScope {
        val files = FileDoc.fromFile(File(appCtx.externalCacheDir, "logs")).list()
        if (files.isNullOrEmpty()) {
            return@coroutineScope
        }
        doc.find("logs")?.delete()
        val logsDoc = doc.createFolderIfNotExist("logs")
        files.forEach { file ->
            launch {
                file.openInputStream().getOrNull()?.use { input ->
                    logsDoc.createFileIfNotExist(file.name).openOutputStream().getOrNull()
                        ?.use {
                            input.copyTo(it)
                        }
                }
            }
        }
    }

    private fun copyHeapDump(doc: FileDoc): Boolean {
        val heapFile = FileDoc.fromFile(File(appCtx.externalCacheDir, "heapDump")).list()
            ?.firstOrNull() ?: return false
        doc.find("heapDump")?.delete()
        val heapDumpDoc = doc.createFolderIfNotExist("heapDump")
        heapFile.openInputStream().getOrNull()?.use { input ->
            heapDumpDoc.createFileIfNotExist(heapFile.name).openOutputStream().getOrNull()
                ?.use {
                    input.copyTo(it)
                }
        }
        return true
    }

}