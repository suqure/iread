package ltd.finelink.read.ui.book.read.config

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseDialogFragment
import ltd.finelink.read.base.SpinnerOption
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.data.entities.TTSSpeaker
import ltd.finelink.read.databinding.DialogSpeakerEditBinding
import ltd.finelink.read.help.tts.ConvertFeature
import ltd.finelink.read.help.tts.LocalTTSHelp
import ltd.finelink.read.help.tts.WavResampleRate
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.ui.file.HandleFileContract
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.MD5Utils
import ltd.finelink.read.utils.SelectImageContract
import ltd.finelink.read.utils.applyTint
import ltd.finelink.read.utils.externalFiles
import ltd.finelink.read.utils.gone
import ltd.finelink.read.utils.inputStream
import ltd.finelink.read.utils.postEvent
import ltd.finelink.read.utils.readUri
import ltd.finelink.read.utils.setLayout
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import ltd.finelink.read.utils.visible
import splitties.init.appCtx
import java.io.File
import java.io.FileOutputStream

class VoiceEditDialog() : BaseDialogFragment(R.layout.dialog_speaker_edit, true),
    Toolbar.OnMenuItemClickListener, AdapterView.OnItemSelectedListener {

    constructor(id: Long) : this() {
        arguments = Bundle().apply {
            putLong("id", id)
        }
    }

    private val categories: MutableList<SpinnerOption> = mutableListOf()
    private val binding by viewBinding(DialogSpeakerEditBinding::bind)
    private val viewModel by viewModels<VoiceEditViewModel>()
    private val resample: WavResampleRate = WavResampleRate()
    private var audio: FloatArray? = null


    private val selectWavFile = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            handleWav(uri)
            binding.tvWavPath.text = uri.path
        }
    }

    private val selectImage = registerForActivityResult(SelectImageContract()) {
        it.uri?.let { uri ->
            setCoverUri(uri)
        }
    }
    private val speakerDir: File by lazy {
        FileUtils.createFolderIfNotExist(appCtx.externalFiles.absolutePath + File.separator + "speaker" + File.separator)
    }

    private fun handleWav(uri: Uri) {
        readUri(uri) { fileDoc, inputStream ->
            kotlin.runCatching {
                audio = resample.loadAudio(inputStream, LocalTTSHelp.extsr, 10, 0)
            }.onFailure {
                appCtx.toastOnUi(it.localizedMessage)
                Log.e("voiceEdit", "error", it)
            }
        }
    }

    private fun setCoverUri(uri: Uri) {
        readUri(uri) { fileDoc, inputStream ->
            kotlin.runCatching {
                var file = requireContext().externalFiles
                val suffix = fileDoc.name.substringAfterLast(".")
                val fileName = uri.inputStream(requireContext()).getOrThrow().use {
                    MD5Utils.md5Encode(it) + ".$suffix"
                }
                file = FileUtils.createFileIfNotExist(file, "cover", fileName)
                FileOutputStream(file).use {
                    inputStream.copyTo(it)
                }
                binding.ivCover.load(file.absolutePath)
                binding.ivMenuDelete.visible(true)
            }.onFailure {
                appCtx.toastOnUi(it.localizedMessage)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        LocalTTSHelp.loadExtModel()
        LocalTTSHelp.loadSpkModel()
        viewModel.initData(arguments) {
            initCategory()
            initView(speaker = it)
        }
        initMenu()
        initEvent()
    }

    private fun initCategory() {
        categories.add(SpinnerOption(id = 2, text = "Clone"))
        categories.add(SpinnerOption(id = 3, text = "CHAT"))
        var speakerAdapter = ArrayAdapter<SpinnerOption>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )
        speakerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spCategory.adapter = speakerAdapter
        binding.spCategory.onItemSelectedListener = this


    }


    private fun initEvent() = binding.run {
        llTtsWav.setOnClickListener {
            selectWavFile.launch {
                mode = HandleFileContract.FILE
                allowExtensions = arrayOf("wav")
            }
        }
        ivCover.setOnClickListener {
            selectImage.launch(0)
        }
        ivMenuDelete.setOnClickListener {
            ivCover.load(null, tvName.text.toString())
            binding.ivMenuDelete.visible(false)
        }


    }

    fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.speak_engine_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    fun initView(speaker: TTSSpeaker) {
        binding.tvName.setText(speaker.name)
        binding.spCategory.setSelection(0)
        binding.ivCover.load(null, speaker.name)
        binding.ivMenuDelete.visible(false)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> {
                if (audio == null&&binding.llTtsWav.isVisible) {
                    toastOnUi(getString(R.string.wav_select))
                    return false
                }
                viewModel.save(dataFromView()) {
                    toastOnUi("保存成功")
                    postEvent(EventBus.UP_STORE_MODEL, viewModel.id)
                    dismiss()
                }

            }

        }
        return true
    }

    private fun writeSpeaker(fileName: String, data: ConvertFeature) {
        var json = Gson().toJson(data)
        var file = FileUtils.createFileIfNotExist("${speakerDir}/${fileName}")
        file.printWriter().use {
            it.println(json)
        }
    }

    private fun dataFromView(): TTSSpeaker {
        var category = categories[binding.spCategory.selectedItemPosition]
        var speaker = TTSSpeaker(
            id = System.currentTimeMillis(),
            name = binding.tvName.text.toString(),
            type = category.id.toInt(),
            description = binding.tvDesc.text.toString(),
            cover = binding.ivCover.bitmapPath,
            progress = 100,
            download = true
        )
        if(speaker.type==3){
            var seed = System.currentTimeMillis()
            if(binding.tvSeed.text!=null){
                seed = binding.tvSeed.text.toString().toLong()
            }
            writeSpeaker(speaker.speakerFile(), LocalTTSHelp.spkAudioFeature(seed))
        }else{
            writeSpeaker(speaker.speakerFile(), LocalTTSHelp.extAudioFeature(audio!!))
        }

        return speaker

    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        when (p2) {
            1 -> {
                binding.llTtsWav.gone()
                binding.llTtsSeed.gone(false)
            }
            else -> {
                binding.llTtsWav.gone(false)
                binding.llTtsSeed.gone()
            }
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {

    }

}

