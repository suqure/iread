package ltd.finelink.read.ui.main.store

import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.viewModels
import cn.hutool.core.codec.Base64
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseFragment
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.ui.association.ImportLocalTtsDialog
import ltd.finelink.read.ui.association.ImportSpeakerDialog
import ltd.finelink.read.ui.book.read.config.SpeakEngineDialog
import ltd.finelink.read.ui.book.read.config.VoiceEditDialog
import ltd.finelink.read.ui.file.HandleFileContract
import ltd.finelink.read.ui.main.MainFragmentInterface
import ltd.finelink.read.ui.qrcode.QrCodeResult
import ltd.finelink.read.ui.store.cache.TTSCacheActivity
import ltd.finelink.read.ui.widget.dialog.TextDialog
import ltd.finelink.read.ui.widget.dialog.WaitDialog
import ltd.finelink.read.utils.inputStream
import ltd.finelink.read.utils.isContentScheme
import ltd.finelink.read.utils.launch
import ltd.finelink.read.utils.postEvent
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.startActivity
import ltd.finelink.read.utils.toastOnUi
import splitties.init.appCtx
import java.io.BufferedReader
import java.io.InputStreamReader


abstract class BaseStoreFragment(layoutId: Int) : VMBaseFragment<StoreViewModel>(layoutId),
    MainFragmentInterface {

    override val position: Int? get() = arguments?.getInt("position")

    override val viewModel by viewModels<StoreViewModel>()



    private val importDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            if(uri.isContentScheme()){
                val documentFile = DocumentFile.fromSingleUri(requireContext(), uri)
                documentFile?.name?.let { file->
                    if(file.endsWith(".ird")){
                        importFromText(uri)
                    }else if(file.endsWith(".irz")||file.endsWith(".zip")){
                        waitDialog.show()
                        viewModel.importFromZip(uri){
                            waitDialog.dismiss()
                        }
                    }else{
                        toastOnUi(R.string.data_Illegal)
                    }
                }
            }else{
                uri.path?.let {
                    if(it.endsWith(".ird")){
                        importFromText(uri)
                    }else if(it.endsWith(".irz")||it.endsWith(".zip")){
                        waitDialog.show()
                        viewModel.importFromZip(uri){
                            waitDialog.dismiss()
                        }
                    }else{
                        toastOnUi(R.string.data_Illegal)
                    }
                }

            }
        }
    }

    private fun importFromText(uri: Uri){
        uri.inputStream(requireContext()).getOrThrow().use { inputS ->
            val sb = StringBuilder()
            val br = BufferedReader(InputStreamReader(inputS))
            while (true) {
                val line = br.readLine()
                if (line != null) {
                    sb.append(line)
                } else {
                    break
                }
            }
            br.close()
            var text = sb.toString().trim()
            if(text.startsWith("IRM:")){
                showDialogFragment(ImportLocalTtsDialog(handleScanText(text)))
            }else if(text.startsWith("IRV:")){
                showDialogFragment(ImportSpeakerDialog(handleScanText(text)))
            }else{
                toastOnUi(R.string.data_Illegal)
            }
        }
    }

    private val waitDialog by lazy {
        WaitDialog(requireContext()).apply {
            setOnCancelListener {
                viewModel.addStoreJob?.cancel()
            }
        }
    }

    private fun handleScanText(text:String):String{
        var base64 = text.substring(4)
        if (Base64.isBase64(base64)) {
            return Uri.decode(Base64.decodeStr(base64))
        }
        return base64
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_store, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        when (item.itemId) {
            R.id.menu_update_toc -> postEvent(EventBus.UP_STORE, "")
            R.id.menu_engine_config-> showDialogFragment(SpeakEngineDialog(object: SpeakEngineDialog.CallBack {
                override fun upSpeakEngineSummary() {

                }
            }))
            R.id.menu_import_local -> importDoc.launch {
                mode = HandleFileContract.FILE
                allowExtensions = arrayOf("irz","zip")
            }
            R.id.menu_add_speaker ->{
                showDialogFragment(VoiceEditDialog(0))
            }

            R.id.menu_help -> {
                showHelp()
            }
        }


    }



    fun showHelp() {
        val text = String(requireContext().assets.open(getString(R.string.help_store_file)).readBytes())
        showDialogFragment(TextDialog(getString(R.string.help), text, TextDialog.Mode.MD))
    }


    override fun observeLiveBus() {
        viewModel.addStoreProgressLiveData.observe(this) { count ->
            if (count < 0) {
                waitDialog.dismiss()
            } else {
                waitDialog.setText("添加中... ($count)")
            }
        }
    }


}