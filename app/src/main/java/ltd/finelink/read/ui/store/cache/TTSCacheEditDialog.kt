package ltd.finelink.read.ui.store.cache

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseDialogFragment
import ltd.finelink.read.base.SpinnerOption
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.TTSCache
import ltd.finelink.read.databinding.DialogTtsCacheEditBinding
import ltd.finelink.read.help.coroutine.Coroutine
import ltd.finelink.read.help.tts.LocalTTSHelp
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.ui.book.read.config.LocalTtsEditDialog
import ltd.finelink.read.utils.WavFileUtils
import ltd.finelink.read.utils.applyTint
import ltd.finelink.read.utils.gone
import ltd.finelink.read.utils.setLayout
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding

class TTSCacheEditDialog() : BaseDialogFragment(R.layout.dialog_tts_cache_edit, true),
    Toolbar.OnMenuItemClickListener, AdapterView.OnItemSelectedListener {

    constructor(id: Long) : this() {
        arguments = Bundle().apply {
            putLong("id", id)
        }
    }

    private val categories: MutableList<SpinnerOption> = mutableListOf()
    private val binding by viewBinding(DialogTtsCacheEditBinding::bind)
    private val viewModel by viewModels<TTSCacheEditViewModel>()
    private var generateJob: Coroutine<*>? = null


    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        viewModel.initData(arguments) {
            initCategory()
            initView(it)
        }
        initMenu()
        initEvent()
    }

    private fun initCategory() {
        var data = appDb.localTTSDao.findByDownload(true);
        for (sp in data) {
            categories.add(SpinnerOption(id = sp.id, text = sp.name!!))
        }
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
        ivEdit.setOnClickListener {
            if(categories.isNotEmpty()){
                val position =  spCategory.selectedItemPosition
                showDialogFragment(LocalTtsEditDialog(categories[position].id))
            }else{
                toastOnUi(R.string.engine_select)

            }

        }
        tvCancel.setOnClickListener{
            dismiss()
        }
        tvOk.setOnClickListener{
            if(categories.isNotEmpty()){
                generateJob?.cancel()
                val position =  spCategory.selectedItemPosition
                appDb.localTTSDao.get(categories[position].id)?.let {
                    LocalTTSHelp.loadModel(it).let { model->
                        tvCancel.gone()
                        tvOk.gone()
                        rotateLoading.visible()
                        generateJob = execute {
                            model.init()
                            viewModel.record!!.modelId = model.modelId()
                            viewModel.record!!.speakerId = model.speakerId()
                            var audio = model.tts(viewModel.record!!.text)
                            WavFileUtils.rawToWave(viewModel.record!!.file, audio, model.sampleRate())
                            viewModel.save(viewModel.record!!)
                            toastOnUi(R.string.success)
                            dismiss()
                        }
                    }
                }
            }else{
                toastOnUi(R.string.engine_select)
            }
        }
    }

    fun initMenu() {
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    fun initView(cache: TTSCache) {
        binding.tvBookName.text = cache.bookName
        binding.tvBookChapter.text = cache.chapterTitle
        binding.tvWavText.text = cache.text
        var selected = 0
        for(i in categories.indices){
            if(categories[i].id==cache.modelId){
                selected = i
                break
            }
        }
        binding.spCategory.setSelection(selected)

    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {

        return true
    }



    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        var id =  categories[p2].id
        appDb.localTTSDao.get(id)?.let {
             LocalTTSHelp.loadModel(it)
        }

    }

    override fun onNothingSelected(p0: AdapterView<*>?) {

    }

}

