package ltd.finelink.read.ui.store.model

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
import ltd.finelink.read.data.entities.LocalTTS
import ltd.finelink.read.databinding.ActivityModelInfoBinding
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.lib.theme.backgroundColor
import ltd.finelink.read.lib.theme.bottomBackground
import ltd.finelink.read.lib.theme.getPrimaryTextColor
import ltd.finelink.read.model.BookCover
import ltd.finelink.read.model.ReadAloud
import ltd.finelink.read.model.ReadBook
import ltd.finelink.read.ui.book.read.config.LocalTtsEditDialog
import ltd.finelink.read.ui.widget.dialog.WaitDialog
import ltd.finelink.read.utils.ColorUtils
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.externalFiles
import ltd.finelink.read.utils.gone
import ltd.finelink.read.utils.observeEvent
import ltd.finelink.read.utils.postEvent
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.startService
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import ltd.finelink.read.utils.visible
import splitties.init.appCtx
import java.io.File

class ModelInfoActivity :
    VMBaseActivity<ActivityModelInfoBinding, ModelInfoViewModel>(toolBarTheme = Theme.Dark) {

    private val waitDialog by lazy { WaitDialog(this) }

    override val binding by viewBinding(ActivityModelInfoBinding::inflate)
    override val viewModel by viewModels<ModelInfoViewModel>()
    private val modelsDir: File by lazy {
        FileUtils.createFolderIfNotExist(appCtx.externalFiles.absolutePath + File.separator + "model" + File.separator)
    }

    @SuppressLint("PrivateResource")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.setBackgroundResource(R.color.transparent)
        binding.refreshLayout?.setColorSchemeColors(accentColor)
        binding.arcView.setBgColor(backgroundColor)
        binding.llInfo.setBackgroundColor(backgroundColor)
        binding.flAction.setBackgroundColor(bottomBackground)
        binding.tvShelf.setTextColor(getPrimaryTextColor(ColorUtils.isColorLight(bottomBackground)))

        viewModel.modelData.observe(this) { showBook(it) }
        viewModel.waitDialogData.observe(this) { upWaitDialogStatus(it) }
        viewModel.initData(intent)
        initViewEvent()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.model_info, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }


    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> {
                refreshBook()
            }

            R.id.menu_clear_cache -> alert(
                titleResource = R.string.draw,
                messageResource = R.string.sure_del_model_cache
            ) {
                yesButton {
                    viewModel.clearCache()
                }
                noButton()
            }
            R.id.menu_aloud_config -> {
                viewModel.getModel()?.let { model->
                    if(model.download){
                        alert(
                            titleResource = R.string.draw,
                            messageResource = R.string.set_engine_confirm
                        ) {
                            yesButton {
                                    setReadAloudEngine(model.id.toString())
                                    toastOnUi(R.string.success)
                                }

                            noButton()
                        }
                    }else{
                        toastOnUi(R.string.download_model_tips)
                    }
                }

            }

        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun setReadAloudEngine(ttsEngine:String){
        ReadBook.book?.setTtsEngine(ttsEngine)
        AppConfig.ttsEngine = ttsEngine
        ReadAloud.upReadAloudClass()
    }

    override fun observeLiveBus() {
        observeEvent<LocalTTS>(EventBus.UP_MODEL_DOWNLOAD) {
            var current = viewModel.modelData.value
            if (it.id == current?.id) {
                binding.ivProgressBar.progress = it.progress
                if(it.status==1){
                    binding.tvShelf.text = getString(R.string.starting_download)
                }
                if (it.download) {
                    binding.tvShelf.text = getString(R.string.remove_model)
                    viewModel.modelData.postValue(it)
                }
            }
        }
        observeEvent<Long>(EventBus.UP_STORE_MODEL) {
            var current = viewModel.modelData.value
            if(it==current?.id){
                refreshBook()
            }
        }

    }

    private fun refreshBook() {
        viewModel.getModel()?.let {
            viewModel.refreshBook(it)
        }
    }


    private fun showBook(model: LocalTTS) = binding.run {
        showCover(model)
        tvName.text = model.name
        tvAuthor.text = getString(R.string.speaker_show, model.speakerName)
        tvOrigin.text = getString(R.string.category_show, model.categroy())
        ivProgressBar.progress = model.progress
        tvTtsTopKValue.text = model.topK.toString()
        tvTtsTopPValue.text = model.topP.toString()
        tvTtsTemperatureValue.text = model.temperature.toString()
        tvTtsSpeedValue.text = model.speed.toString()
        if (model.type == 0) {
            llTtsSpeechRate.gone()
            llTtsRefer.gone()
            tvIntro.text = getString(R.string.model_gpt_intro)
        } else if (model.type == 1) {
            llTtsSpeechTopK.gone()
            llTtsSpeechTopP.gone()
            llTtsTemperature.gone()
            llTtsRefer.gone()
            tvIntro.text = getString(R.string.model_tts_intro)
        } else if (model.type == 2) {

            appDb.localTTSDao.get(model.refId?:0)?.let {
                tvTtsReferValue.text = it.name
            }
            llTtsSpeechRate.gone()
            llTtsSpeechTopK.gone()
            llTtsSpeechTopP.gone()
            tvIntro.text = getString(R.string.model_convert_intro)
        } else if (model.type == 3) {

            llTtsRefer.gone()
            llTtsSpeechRate.gone()
            tvIntro.text = getString(R.string.model_chat_intro)
        }else if (model.type == 4) {
            llTtsSpeechTopK.gone()
            llTtsSpeechTopP.gone()
            llTtsRefer.gone()
            llTtsSpeechRate.gone()
            tvIntro.text = getString(R.string.model_cos_intro)
        }else if (model.type == 5) {
            llTtsSpeechTopK.gone()
            llTtsRefer.gone()
            llTtsSpeechRate.gone()
            tvIntro.text = getString(R.string.model_fish_intro)
        }
        tvShelf.text = getString(R.string.action_download)
        if (model.download) {
            tvShelf.text = getString(R.string.remove_model)
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

    private fun showCover(book: LocalTTS) {
        binding.ivCover.load(book.cover, book.name, book.speakerName, false, null)
        if (!AppConfig.isEInkMode) {
            BookCover.loadBlur(this, book.cover)
                .into(binding.bgBook)
        }
    }


    private fun initViewEvent() = binding.run {
        tvSetting.setOnClickListener {
            viewModel.getModel()?.let { model ->
                if(model.download){
                    showDialogFragment(LocalTtsEditDialog(model.id))
                }else{
                    toastOnUi(R.string.download_model_tips)
                }
            }
        }
        tvShelf.setOnClickListener {
            viewModel.getModel()?.let { book ->
                if (book.download) {
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
            refreshBook()
        }
    }


    @SuppressLint("InflateParams")
    private fun deleteModel() {
        viewModel.getModel()?.let {
            var dir = File("${modelsDir}/${it.id}")
            if (dir.exists()) {
                for (file in dir.listFiles()) {
                    file.delete()
                }
                dir.delete()
            }
            it.download = false
            it.progress = 0
            it.status = 0
            appDb.localTTSDao.update(it)
            refreshBook()
            postEvent(EventBus.UP_STORE_MODEL, it.id)
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