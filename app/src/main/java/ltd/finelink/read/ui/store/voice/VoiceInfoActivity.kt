package ltd.finelink.read.ui.store.voice

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseActivity
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.constant.IntentAction
import ltd.finelink.read.constant.Theme
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.TTSSpeaker
import ltd.finelink.read.databinding.ActivityVoiceInfoBinding
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.lib.theme.backgroundColor
import ltd.finelink.read.lib.theme.bottomBackground
import ltd.finelink.read.lib.theme.getPrimaryTextColor
import ltd.finelink.read.model.BookCover
import ltd.finelink.read.ui.widget.dialog.WaitDialog
import ltd.finelink.read.utils.ColorUtils
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.externalFiles
import ltd.finelink.read.utils.gone
import ltd.finelink.read.utils.observeEvent
import ltd.finelink.read.utils.postEvent
import ltd.finelink.read.utils.startService
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import ltd.finelink.read.utils.visible
import splitties.init.appCtx
import java.io.File

class VoiceInfoActivity :
    VMBaseActivity<ActivityVoiceInfoBinding, VoiceInfoViewModel>(toolBarTheme = Theme.Dark) {

    private val waitDialog by lazy { WaitDialog(this) }

    override val binding by viewBinding(ActivityVoiceInfoBinding::inflate)
    override val viewModel by viewModels<VoiceInfoViewModel>()
    private val speakerDir: File by lazy {
        FileUtils.createFolderIfNotExist(appCtx.externalFiles.absolutePath + File.separator + "speaker" + File.separator)
    }

    @SuppressLint("PrivateResource")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.setBackgroundResource(R.color.transparent)
        binding.refreshLayout?.setColorSchemeColors(accentColor)
        binding.arcView.setBgColor(backgroundColor)
        binding.llInfo.setBackgroundColor(backgroundColor)
        binding.flAction.setBackgroundColor(bottomBackground)
        binding.tvShelf.setTextColor(getPrimaryTextColor(ColorUtils.isColorLight(bottomBackground)))

        viewModel.modelData.observe(this) { showVoice(it) }
        viewModel.waitDialogData.observe(this) { upWaitDialogStatus(it) }
        viewModel.initData(intent)
        initViewEvent()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.voice_info, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }


    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> {
                refreshSpeaker()
            }
            R.id.menu_aloud_config -> {
                viewModel.getModel()?.let { voice->
                    if(voice.download){
                        alert(
                            titleResource = R.string.draw,
                            messageResource = R.string.set_voice_confirm
                        ) {
                            yesButton {

                                setEngineVoice(voice)
                                toastOnUi(R.string.success)
                            }
                            noButton()
                        }
                    }else{
                        toastOnUi(R.string.download_model_tips)
                    }

                }
            }
            R.id.menu_clear_cache -> alert(
                titleResource = R.string.draw,
                messageResource = R.string.sure_del_voice_cache
            ) {
                yesButton {
                    viewModel.clearCache()
                }
                noButton()
            }


        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun setEngineVoice(speaker:TTSSpeaker){
        appDb.localTTSDao.fineByType(speaker.type).forEach { localTTS ->
            localTTS.speakerId = speaker.id
            localTTS.speakerName = speaker.name
            appDb.localTTSDao.update(localTTS)
            postEvent(EventBus.UP_STORE_MODEL, localTTS.id)
        }
    }

    override fun observeLiveBus() {
        observeEvent<TTSSpeaker>(EventBus.UP_VOICE_DOWNLOAD) {
            var current = viewModel.modelData.value
            if (it.id == current?.id) {
                binding.ivProgressBar.progress = it.progress
                if(it.status==1){
                    binding.tvShelf.text = getString(R.string.starting_download)
                }
                if (it.download) {
                    binding.tvShelf.text = getString(R.string.remove_model)
                }
            }
        }
        observeEvent<Long>(EventBus.UP_STORE_VOICE) {
            var current = viewModel.modelData.value
            if(it==current?.id){
                refreshSpeaker()
            }
        }

    }

    private fun refreshSpeaker() {
        viewModel.getModel()?.let {
            viewModel.refreshBook(it)
        }
    }


    private fun showVoice(model: TTSSpeaker) = binding.run {
        showCover(model)
        tvName.text = model.name
        tvOrigin.text = getString(R.string.category_show, model.categroy())
        ivProgressBar.progress = model.progress
        tvIntro.text = model.description

        tvShelf.text = getString(R.string.action_download)
        if (model.download) {
            tvShelf.text = getString(R.string.remove_voice)
        }
        if(model.path==""||model.path=="asset"){
            tvShelf.gone()
        }
        val kinds = emptyArray<String>()
        if (kinds.isEmpty()) {
            lbKind.gone()
        } else {
            lbKind.visible()
            lbKind.setLabels(kinds)
        }
    }

    private fun showCover(book: TTSSpeaker) {
        binding.ivCover.load(book.cover, book.name, book.description, false, null)
        if (!AppConfig.isEInkMode) {
            BookCover.loadBlur(this, book.cover)
                .into(binding.bgBook)
        }
    }


    private fun initViewEvent() = binding.run {
        tvShelf.setOnClickListener {
            viewModel.getModel()?.let { speaker ->
                if (speaker.download) {
                    alert(
                        titleResource = R.string.draw,
                        messageResource = R.string.sure_del
                    ) {
                        yesButton {
                            deleteModel()
                        }
                        noButton()
                    }

                }
            }
        }

        refreshLayout?.setOnRefreshListener {
            refreshLayout.isRefreshing = false
            refreshSpeaker()
        }
    }


    @SuppressLint("InflateParams")
    private fun deleteModel() {

        viewModel.getModel()?.let {
            var name = it.speakerFile()
            var file = File("${speakerDir}/${name}")
            if(file.exists()){
                file.delete()
            }
            it.download = false
            it.progress = 0
            it.status = 0
            appDb.ttsSpeakerDao.update(it)
            refreshSpeaker()
            postEvent(EventBus.UP_STORE_VOICE, it.id)
        }
    }


    private fun upWaitDialogStatus(isShow: Boolean) {
        val showText = "Loading....."
        if (isShow) {
            waitDialog.run {
                setText(showText)
                show()
            }
        } else {
            waitDialog.dismiss()
        }
    }

}