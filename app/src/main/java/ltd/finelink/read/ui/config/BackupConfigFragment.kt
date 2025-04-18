package ltd.finelink.read.ui.config

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.activityViewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import ltd.finelink.read.R
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.constant.PreferKey
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.help.config.LocalConfig
import ltd.finelink.read.help.coroutine.Coroutine
import ltd.finelink.read.help.storage.Backup
import ltd.finelink.read.help.storage.BackupConfig
import ltd.finelink.read.help.storage.ImportOldData
import ltd.finelink.read.help.storage.Restore
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.permission.Permissions
import ltd.finelink.read.lib.permission.PermissionsCompat
import ltd.finelink.read.lib.prefs.fragment.PreferenceFragment
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.ui.about.AppLogDialog
import ltd.finelink.read.ui.file.HandleFileContract
import ltd.finelink.read.ui.widget.dialog.TextDialog
import ltd.finelink.read.ui.widget.dialog.WaitDialog
import ltd.finelink.read.utils.applyTint
import ltd.finelink.read.utils.checkWrite
import ltd.finelink.read.utils.getPrefString
import ltd.finelink.read.utils.isContentScheme
import ltd.finelink.read.utils.launch
import ltd.finelink.read.utils.setEdgeEffectColor
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.toastOnUi
import splitties.init.appCtx
import kotlin.collections.set

class BackupConfigFragment : PreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    MenuProvider {

    private val viewModel by activityViewModels<ConfigViewModel>()
    private val waitDialog by lazy { WaitDialog(requireContext()) }
    private var backupJob: Job? = null

    private val selectBackupPath = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            if (uri.isContentScheme()) {
                AppConfig.backupPath = uri.toString()
            } else {
                AppConfig.backupPath = uri.path
            }
        }
    }
    private val backupDir = registerForActivityResult(HandleFileContract()) { result ->
        result.uri?.let { uri ->
            if (uri.isContentScheme()) {
                AppConfig.backupPath = uri.toString()
                Coroutine.async {
                    Backup.backup(appCtx, uri.toString())
                }.onSuccess {
                    appCtx.toastOnUi(R.string.backup_success)
                }.onError {
                    AppLog.put("备份出错\n${it.localizedMessage}", it)
                    appCtx.toastOnUi(getString(R.string.backup_fail, it.localizedMessage))
                }
            } else {
                uri.path?.let { path ->
                    AppConfig.backupPath = path
                    Coroutine.async {
                        Backup.backup(appCtx, path)
                    }.onSuccess {
                        appCtx.toastOnUi(R.string.backup_success)
                    }.onError {
                        AppLog.put("备份出错\n${it.localizedMessage}", it)
                        appCtx.toastOnUi(getString(R.string.backup_fail, it.localizedMessage))
                    }
                }
            }
        }
    }
    private val restoreDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            waitDialog.setText("恢复中…")
            waitDialog.show()
            val task = Coroutine.async {
                Restore.restore(appCtx, uri)
            }.onFinally {
                waitDialog.dismiss()
            }
            waitDialog.setOnCancelListener {
                task.cancel()
            }
        }
    }
    private val restoreOld = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            ImportOldData.importUri(appCtx, uri)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_backup)

        upPreferenceSummary(PreferKey.backupPath, getPrefString(PreferKey.backupPath))
        findPreference<ltd.finelink.read.lib.prefs.Preference>("web_dav_restore")
            ?.onLongClick {
                restoreFromLocal()
                true
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.backup_restore)
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        listView.setEdgeEffectColor(primaryColor)
        activity?.addMenuProvider(this, viewLifecycleOwner)
        if (!LocalConfig.backupHelpVersionIsLast) {
            showHelp()
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.backup_restore, menu)
        menu.applyTint(requireContext())
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_help -> {
                showHelp()
                return true
            }

            R.id.menu_log -> showDialogFragment<AppLogDialog>()
        }
        return false
    }

    private fun showHelp() {
        val text = String(requireContext().assets.open(getString(R.string.help_backup_file)).readBytes())
        showDialogFragment(TextDialog(getString(R.string.help), text, TextDialog.Mode.MD))
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PreferKey.backupPath -> upPreferenceSummary(key, getPrefString(key))
            PreferKey.webDavUrl,
            PreferKey.webDavAccount,
            PreferKey.webDavPassword,
            PreferKey.webDavDir -> listView.post {
                upPreferenceSummary(key, appCtx.getPrefString(key))
                viewModel.upWebDavConfig()
            }

            PreferKey.webDavDeviceName -> upPreferenceSummary(key, getPrefString(key))
        }
    }

    private fun upPreferenceSummary(preferenceKey: String, value: String?) {
        val preference = findPreference<Preference>(preferenceKey) ?: return
        when (preferenceKey) {
            PreferKey.webDavUrl ->
                if (value.isNullOrBlank()) {
                    preference.summary = getString(R.string.web_dav_url_s)
                } else {
                    preference.summary = value.toString()
                }

            PreferKey.webDavAccount ->
                if (value.isNullOrBlank()) {
                    preference.summary = getString(R.string.web_dav_account_s)
                } else {
                    preference.summary = value.toString()
                }

            PreferKey.webDavPassword ->
                if (value.isNullOrBlank()) {
                    preference.summary = getString(R.string.web_dav_pw_s)
                } else {
                    preference.summary = "*".repeat(value.toString().length)
                }

            PreferKey.webDavDir -> preference.summary = when (value) {
                null -> "legado"
                else -> value
            }

            else -> {
                if (preference is ListPreference) {
                    val index = preference.findIndexOfValue(value)
                    // Set the summary to reflect the new value.
                    preference.summary = if (index >= 0) preference.entries[index] else null
                } else {
                    preference.summary = value
                }
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            PreferKey.backupPath -> selectBackupPath.launch()
            PreferKey.restoreIgnore -> backupIgnore()
            "web_dav_backup" -> backup()
            "web_dav_restore" -> restoreFromLocal()
            "import_old" -> restoreOld.launch()
        }
        return super.onPreferenceTreeClick(preference)
    }

    /**
     * 备份忽略设置
     */
    private fun backupIgnore() {
        val checkedItems = BooleanArray(BackupConfig.ignoreKeys.size) {
            BackupConfig.ignoreConfig[BackupConfig.ignoreKeys[it]] ?: false
        }
        alert(R.string.restore_ignore) {
            multiChoiceItems(BackupConfig.ignoreTitle, checkedItems) { _, which, isChecked ->
                BackupConfig.ignoreConfig[BackupConfig.ignoreKeys[which]] = isChecked
            }
            onDismiss {
                BackupConfig.saveIgnoreConfig()
            }
        }
    }


    fun backup() {
        val backupPath = AppConfig.backupPath
        if (backupPath.isNullOrEmpty()) {
            backupDir.launch()
        } else {
            if (backupPath.isContentScheme()) {
                val uri = Uri.parse(backupPath)
                val doc = DocumentFile.fromTreeUri(requireContext(), uri)
                if (doc?.checkWrite() == true) {
                    waitDialog.setText("备份中…")
                    waitDialog.setOnCancelListener {
                        backupJob?.cancel()
                    }
                    waitDialog.show()
                    Coroutine.async {
                        backupJob = coroutineContext[Job]
                        Backup.backup(requireContext(), backupPath)
                    }.onSuccess {
                        appCtx.toastOnUi(R.string.backup_success)
                    }.onError {
                        AppLog.put("备份出错\n${it.localizedMessage}", it)
                        appCtx.toastOnUi(
                            appCtx.getString(
                                R.string.backup_fail,
                                it.localizedMessage
                            )
                        )
                    }.onFinally(Main) {
                        waitDialog.dismiss()
                    }
                } else {
                    backupDir.launch()
                }
            } else {
                backupUsePermission(backupPath)
            }
        }
    }

    private fun backupUsePermission(path: String) {
        PermissionsCompat.Builder()
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                waitDialog.setText("备份中…")
                waitDialog.setOnCancelListener {
                    backupJob?.cancel()
                }
                waitDialog.show()
                Coroutine.async {
                    backupJob = coroutineContext[Job]
                    AppConfig.backupPath = path
                    Backup.backup(requireContext(), path)
                }.onSuccess {
                    appCtx.toastOnUi(R.string.backup_success)
                }.onError {
                    AppLog.put("备份出错\n${it.localizedMessage}", it)
                    appCtx.toastOnUi(appCtx.getString(R.string.backup_fail, it.localizedMessage))
                }.onFinally {
                    waitDialog.dismiss()
                }
            }
            .request()
    }



    private fun restoreFromLocal() {
        restoreDoc.launch {
            title = getString(R.string.select_restore_file)
            mode = HandleFileContract.FILE
            allowExtensions = arrayOf("zip")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        waitDialog.dismiss()
    }

}