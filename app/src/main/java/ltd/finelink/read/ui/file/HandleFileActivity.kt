package ltd.finelink.read.ui.file

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseActivity
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.databinding.ActivityTranslucenceBinding
import ltd.finelink.read.help.IntentData
import ltd.finelink.read.lib.dialogs.SelectItem
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.permission.Permissions
import ltd.finelink.read.lib.permission.PermissionsCompat
import ltd.finelink.read.utils.getJsonArray
import ltd.finelink.read.utils.isContentScheme
import ltd.finelink.read.utils.launch
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import java.io.File

class HandleFileActivity :
    VMBaseActivity<ActivityTranslucenceBinding, HandleFileViewModel>(),
    FilePickerDialog.CallBack {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)
    override val viewModel by viewModels<HandleFileViewModel>()
    private var mode = 0

    private val selectDocTree =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                if (uri.isContentScheme()) {
                    val modeFlags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, modeFlags)
                }
                onResult(Intent().setData(uri))
            } ?: finish()
        }

    private val selectDoc = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let {
            if (it.isContentScheme()) {
                val modeFlags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, modeFlags)
            }
            onResult(Intent().setData(it))
        } ?: finish()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        mode = intent.getIntExtra("mode", 0)
        viewModel.errorLiveData.observe(this) {
            toastOnUi(it)
            finish()
        }
        val allowExtensions = intent.getStringArrayExtra("allowExtensions")
        val selectList: ArrayList<SelectItem<Int>> = when (mode) {
            HandleFileContract.DIR_SYS -> getDirActions(true)
            HandleFileContract.DIR -> getDirActions()
            HandleFileContract.FILE -> getFileActions()
            HandleFileContract.EXPORT -> arrayListOf(
                SelectItem(getString(R.string.upload_url), 111)
            ).apply {
                addAll(getDirActions())
            }
            else -> arrayListOf()
        }
        intent.getJsonArray<SelectItem<Int>>("otherActions")?.let {
            selectList.addAll(it)
        }
        val title = intent.getStringExtra("title") ?: let {
            when (mode) {
                HandleFileContract.EXPORT -> return@let getString(R.string.export)
                HandleFileContract.DIR -> return@let getString(R.string.select_folder)
                else -> return@let getString(R.string.select_file)
            }
        }
        alert(title) {
            items(selectList) { _, item, _ ->
                when (item.value) {
                    HandleFileContract.DIR -> kotlin.runCatching {
                        selectDocTree.launch()
                    }.onFailure {
                        AppLog.put(getString(R.string.open_sys_dir_picker_error), it)
                        toastOnUi(R.string.open_sys_dir_picker_error)
                        checkPermissions {
                            FilePickerDialog.show(
                                supportFragmentManager,
                                mode = HandleFileContract.DIR
                            )
                        }
                    }
                    HandleFileContract.FILE -> kotlin.runCatching {
                        selectDoc.launch(typesOfExtensions(allowExtensions))
                    }.onFailure {
                        AppLog.put(getString(R.string.open_sys_dir_picker_error), it)
                        toastOnUi(R.string.open_sys_dir_picker_error)
                        checkPermissions {
                            FilePickerDialog.show(
                                supportFragmentManager,
                                mode = HandleFileContract.FILE,
                                allowExtensions = allowExtensions
                            )
                        }
                    }
                    10 -> checkPermissions {
                        @Suppress("DEPRECATION")
                        lifecycleScope.launchWhenResumed {
                            FilePickerDialog.show(
                                supportFragmentManager,
                                mode = HandleFileContract.DIR
                            )
                        }
                    }
                    11 -> checkPermissions {
                        @Suppress("DEPRECATION")
                        lifecycleScope.launchWhenResumed {
                            FilePickerDialog.show(
                                supportFragmentManager,
                                mode = HandleFileContract.FILE,
                                allowExtensions = allowExtensions
                            )
                        }
                    }
                    111 -> getFileData()?.let {
                        viewModel.upload(it.first, it.second, it.third) { url ->
                            val uri = Uri.parse(url)
                            setResult(RESULT_OK, Intent().setData(uri))
                            finish()
                        }
                    }
                    else -> {
                        val path = item.title
                        val uri = if (path.isContentScheme()) {
                            Uri.parse(path)
                        } else {
                            Uri.fromFile(File(path))
                        }
                        onResult(Intent().setData(uri))
                    }
                }
            }
            onCancelled {
                finish()
            }
        }
    }

    private fun getFileData(): Triple<String, Any, String>? {
        val fileName = intent.getStringExtra("fileName")
        val file = intent.getStringExtra("fileKey")?.let {
            IntentData.get<Any>(it)
        }
        val contentType = intent.getStringExtra("contentType")
        if (fileName != null && file != null && contentType != null) {
            return Triple(fileName, file, contentType)
        }
        return null
    }

    private fun getDirActions(onlySys: Boolean = false): ArrayList<SelectItem<Int>> {
        return if (onlySys) {
            arrayListOf(SelectItem(getString(R.string.sys_folder_picker), HandleFileContract.DIR))
        } else {
            arrayListOf(
                SelectItem(getString(R.string.sys_folder_picker), HandleFileContract.DIR),
                SelectItem(getString(R.string.app_folder_picker), 10)
            )
        }
    }

    private fun getFileActions(): ArrayList<SelectItem<Int>> {
        return arrayListOf(
            SelectItem(getString(R.string.sys_file_picker), HandleFileContract.FILE),
            SelectItem(getString(R.string.app_file_picker), 11)
        )
    }

    private fun checkPermissions(success: (() -> Unit)? = null) {
        PermissionsCompat.Builder()
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                success?.invoke()
            }
            .onDenied {
                finish()
            }
            .onError {
                finish()
            }
            .request()
    }

    private fun typesOfExtensions(allowExtensions: Array<String>?): Array<String> {
        val types = hashSetOf<String>()
        if (allowExtensions.isNullOrEmpty()) {
            types.add("*/*")
        } else {
            allowExtensions.forEach {
                when (it) {
                    "*" -> types.add("*/*")
                    "txt", "xml" -> types.add("text/*")
                    else -> {
                        val mime = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(it)
                            ?: "application/octet-stream"
                        types.add(mime)
                    }
                }
            }
        }
        return types.toTypedArray()
    }

    override fun onResult(data: Intent) {
        val uri = data.data
        uri ?: let {
            finish()
            return
        }
        if (mode == HandleFileContract.EXPORT) {
            getFileData()?.let { fileData ->
                viewModel.saveToLocal(uri, fileData.first, fileData.second) { savedUri ->
                    setResult(RESULT_OK, Intent().setData(savedUri))
                    finish()
                }
            }
        } else {
            data.putExtra("value", intent.getStringExtra("value"))
            setResult(RESULT_OK, data)
            finish()
        }
    }

}