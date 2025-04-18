package ltd.finelink.read.ui.book.read.config

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseDialogFragment
import ltd.finelink.read.base.SpinnerOption
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.LocalTTS
import ltd.finelink.read.databinding.DialogLocalTtsEditBinding
import ltd.finelink.read.help.coroutine.Coroutine
import ltd.finelink.read.help.tts.LocalTTSHelp
import ltd.finelink.read.lib.theme.bottomBackground
import ltd.finelink.read.lib.theme.getPrimaryTextColor
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.ui.widget.seekbar.SeekBarChangeListener
import ltd.finelink.read.utils.ColorUtils
import ltd.finelink.read.utils.WavFileUtils
import ltd.finelink.read.utils.applyTint
import ltd.finelink.read.utils.gone
import ltd.finelink.read.utils.postEvent
import ltd.finelink.read.utils.setLayout
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import ltd.finelink.read.utils.visible
import splitties.init.appCtx
import java.io.File

class LocalTtsEditDialog() : BaseDialogFragment(R.layout.dialog_local_tts_edit, true),
    Toolbar.OnMenuItemClickListener {

    constructor(id: Long) : this() {
        arguments = Bundle().apply {
            putLong("id", id)
        }
    }

    private val speakers: MutableList<SpinnerOption> = mutableListOf()
    private val langList: MutableList<String> = mutableListOf()
    private val refers: MutableList<SpinnerOption> = mutableListOf()
    private val binding by viewBinding(DialogLocalTtsEditBinding::bind)
    private val viewModel by viewModels<LocalTtsEditViewModel>()

    private var generateJob: Coroutine<*>? = null

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(requireContext()).build()
    }

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        val bg = requireContext().bottomBackground
        val isLight = ColorUtils.isColorLight(bg)
        val textColor = requireContext().getPrimaryTextColor(isLight)
        binding.run {
            ivTtsSpeechReduce.setColorFilter(textColor)
            tvTtsSpeed.setTextColor(textColor)
            tvTtsSpeedValue.setTextColor(textColor)
            ivTtsSpeechAdd.setColorFilter(textColor)

            ivTtsTopKReduce.setColorFilter(textColor)
            tvTtsTopK.setTextColor(textColor)
            tvTtsTopKValue.setTextColor(textColor)
            ivTtsTopKAdd.setColorFilter(textColor)

            ivTtsTopPReduce.setColorFilter(textColor)
            tvTtsTopP.setTextColor(textColor)
            tvTtsTopPValue.setTextColor(textColor)
            ivTtsTopPAdd.setColorFilter(textColor)

            ivTtsTemperatureReduce.setColorFilter(textColor)
            tvTtsTemperature.setTextColor(textColor)
            tvTtsTemperatureValue.setTextColor(textColor)
            ivTtsTemperatureAdd.setColorFilter(textColor)
        }
        viewModel.initData(arguments) {
            initSpeaker(it)
            initRefer(it)
            initLang(it)
            initView(localTTS = it)

        }
        initMenu()
        initEvent()
    }

    private fun initSpeaker(localTTS: LocalTTS) {
        speakers.add(SpinnerOption())
        var data = appDb.ttsSpeakerDao.findByTypeAndDownload(localTTS.type, true)
        for (sp in data) {
            speakers.add(SpinnerOption(id = sp.id, text = sp.name!!))
        }
        var speakerAdapter = ArrayAdapter<SpinnerOption>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            speakers
        )
        speakerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spSpeaker.adapter = speakerAdapter

    }

    private fun initLang(localTTS: LocalTTS){
        var langs = localTTS.supportLang.split(",")
        langList.addAll(langs)
        var languages:MutableList<String> = mutableListOf()
        for (lang in localTTS.supportLang.split(",")){
            langList.add(lang)
            val desc = when(lang){
                "zh"-> getString(R.string.lang_zh)
                "en"-> getString(R.string.lang_en)
                "jp"->getString(R.string.lang_jp)
                "yue"->getString(R.string.lang_yue)
                "kr"->getString(R.string.lang_kr)
                else->" "
            }
            languages.add(desc)
        }
        var speakerAdapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            languages
        )
        speakerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spLang.adapter = speakerAdapter
    }


    private fun initRefer(localTTS: LocalTTS) {
        refers.add(SpinnerOption(text=" "))
        if (localTTS.type == 2) {
            var data = appDb.localTTSDao.findReferModel();
            for (sp in data) {
                refers.add(SpinnerOption(id = sp.id, text = sp.name!!))
            }
        }
        var referAdapter = ArrayAdapter<SpinnerOption>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            refers
        )
        referAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spRefer.adapter = referAdapter

    }

    private fun initEvent() = binding.run {

        ivTtsSpeechReduce.setOnClickListener {
            seekTtsSpeechRate.progress -= 1
            upTtsSpeechRateText(seekTtsSpeechRate.progress)
        }
        ivTtsSpeechAdd.setOnClickListener {
            seekTtsSpeechRate.progress += 1
            upTtsSpeechRateText(seekTtsSpeechRate.progress)
        }
        seekTtsSpeechRate.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                super.onProgressChanged(seekBar, progress, fromUser)
                upTtsSpeechRateText(progress)
            }
        })

        ivTtsTemperatureReduce.setOnClickListener {
            seekTtsTemperature.progress -= 1
            upTtsTemperatureText(seekTtsTemperature.progress)
        }
        ivTtsTemperatureAdd.setOnClickListener {
            seekTtsTemperature.progress += 1
            upTtsTemperatureText(seekTtsTemperature.progress)
        }
        seekTtsTemperature.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                super.onProgressChanged(seekBar, progress, fromUser)
                upTtsTemperatureText(progress)
            }
        })

        ivTtsTopKReduce.setOnClickListener {
            seekTtsTopK.progress -= 1
            upTtsTopKText(seekTtsTopK.progress)
        }
        ivTtsTopKAdd.setOnClickListener {
            seekTtsTopK.progress += 1
            upTtsTopKText(seekTtsTopK.progress)
        }
        seekTtsTopK.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                super.onProgressChanged(seekBar, progress, fromUser)
                upTtsTopKText(progress)
            }
        })

        ivTtsTopPReduce.setOnClickListener {
            seekTtsTopP.progress -= 1
            upTtsTopPText(seekTtsTopP.progress)
        }
        ivTtsTopPAdd.setOnClickListener {
            seekTtsTopP.progress += 1
            upTtsTopPText(seekTtsTopP.progress)
        }
        seekTtsTopP.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                super.onProgressChanged(seekBar, progress, fromUser)
                upTtsTopPText(progress)
            }
        })
        ivTtsTest.setOnClickListener{
            generateJob?.cancel()
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            var fileName = appCtx.cacheDir.absolutePath + File.separator+"text.wav"
            var text = tvText.text.toString()
            if(text.isNotEmpty()){
                rotateLoading.visible()
                ivTtsTest.gone()
                generateJob = execute {
                    var model = dataFromView()
                    LocalTTSHelp.refreshModel(model)
                    LocalTTSHelp.loadModel(model).let { model->
                        model.init()
                        var audio = model.tts(text)
                        WavFileUtils.rawToWave(fileName, audio, model.sampleRate())
                    }
                }.onSuccess {
                    val mediaItem = MediaItem.fromUri(Uri.fromFile(File(fileName)))
                    exoPlayer.addMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }.onFinally {
                    rotateLoading.gone()
                    ivTtsTest.visible()
                }
            }else{
                toastOnUi(getString(R.string.error_req,getString(R.string.test_text)))
            }

        }
    }

    fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.speak_engine_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    fun initView(localTTS: LocalTTS) {
        binding.tvName.setText(localTTS.name)
        binding.tvTtsTopKValue.text = localTTS.topK.toString()
        binding.tvTtsTopPValue.text = localTTS.topP.toString()
        binding.tvTtsTemperatureValue.text = localTTS.temperature.toString()
        binding.tvTtsSpeedValue.text = localTTS.speed.toString()
        binding.seekTtsSpeechRate.progress = localTTS.speed?.let {
            it.times(10).toInt() - 5
        } ?: 5
        var selectSpeaker: Int = 0
        var selectRefer: Int = 0
        var selectLang:Int = 0
        localTTS.speakerId?.let {
            for (index in speakers.indices) {
                if (speakers[index].id == it) {
                    selectSpeaker = index
                    break
                }

            }
        }
        localTTS.refId?.let {
            for (index in refers.indices) {
                if (refers[index].id == it) {
                    selectRefer = index
                    break
                }

            }
        }
        localTTS.mainLang?.let {
            for (index in langList.indices) {
                if (langList[index] == it) {
                    selectLang = index
                    break
                }

            }
        }
        binding.swRefineText.isChecked = localTTS.refineText
        binding.spSpeaker.setSelection(selectSpeaker)
        binding.spRefer.setSelection(selectRefer)
        binding.spLang.setSelection(selectLang)
        binding.seekTtsTemperature.progress = (localTTS.temperature?.times(10))?.toInt() ?: 10
        binding.seekTtsTopK.progress = localTTS.topK?.let {
            it - 1
        } ?: 6
        binding.seekTtsTopP.progress = (localTTS.topP?.times(10))?.toInt() ?: 10
        if (localTTS.type == 0) {
            binding.llTtsRefer.gone()
            binding.swRefineText.gone()
        } else if (localTTS.type == 1) {
            binding.llTopK.gone()
            binding.llTopP.gone()
            binding.llTemperature.gone()
            binding.llTtsRefer.gone()
            binding.swRefineText.gone()
        } else if (localTTS.type == 2) {
            binding.llSpeech.gone()
            binding.llTopK.gone()
            binding.llTopP.gone()
            binding.llTtsLang.gone()
            binding.swRefineText.gone()
        }else if (localTTS.type == 3) {
            binding.llSpeech.gone()
            binding.llTtsRefer.gone()
        }else if (localTTS.type == 4)  {
            binding.llSpeech.gone()
            binding.llTtsRefer.gone()
            binding.llTopK.gone()
            binding.llTopP.gone()
            binding.llTemperature.gone()
            binding.swRefineText.gone()
        }else {
            binding.llSpeech.gone()
            binding.llTtsRefer.gone()
            binding.llTopK.gone()
            binding.swRefineText.gone()
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> viewModel.save(dataFromView()) {
                toastOnUi("保存成功")
                postEvent(EventBus.UP_STORE_MODEL, viewModel.id)
                exoPlayer.release()
                dismiss()
            }
        }
        return true
    }

    @SuppressLint("SetTextI18n")
    private fun upTtsSpeechRateText(value: Int) {
        binding.tvTtsSpeedValue.text = ((value + 5) / 10f).toString()
    }

    @SuppressLint("SetTextI18n")
    private fun upTtsTopKText(value: Int) {
        binding.tvTtsTopKValue.text = (value + 1).toString()
    }

    @SuppressLint("SetTextI18n")
    private fun upTtsTopPText(value: Int) {
        binding.tvTtsTopPValue.text = (value / 10f).toString()
    }

    @SuppressLint("SetTextI18n")
    private fun upTtsTemperatureText(value: Int) {
        binding.tvTtsTemperatureValue.text = (value / 10f).toString()
    }

    private fun dataFromView(): LocalTTS {
        var local = appDb.localTTSDao.get(viewModel.id!!)
        local?.let {
            it.name = binding.tvName.text.toString()
            it.topK = binding.tvTtsTopKValue.text.toString().toInt()
            it.topP = binding.tvTtsTopPValue.text.toString().toFloat()
            it.temperature = binding.tvTtsTemperatureValue.text.toString().toFloat()
            it.speed = binding.tvTtsSpeedValue.text.toString().toFloat()
            var selectSpeaker = speakers[binding.spSpeaker.selectedItemPosition]
            it.speakerId = selectSpeaker.id
            it.speakerName = selectSpeaker.text
            if (selectSpeaker.id > 0) {
                var speaker = appDb.ttsSpeakerDao.get(selectSpeaker.id)
                it.speakerName = speaker?.name
                it.speaker = speaker?.speakerFile()
            } else {
                it.speaker = ""
            }
            var selectRefer = refers[binding.spRefer.selectedItemPosition]
            if(selectRefer.id>0){
                it.refId = selectRefer.id
            }
            var selectLang = langList[binding.spLang.selectedItemPosition]
            it.mainLang = selectLang
            it.refineText= binding.swRefineText.isChecked
        }
        return local!!
    }


}