package ltd.finelink.read.ui.store.book

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
import ltd.finelink.read.data.entities.ReadAloudBook
import ltd.finelink.read.databinding.DialogReadAloudBookBinding
import ltd.finelink.read.lib.theme.bottomBackground
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.utils.applyTint
import ltd.finelink.read.utils.setLayout
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding

class ReadAloudBookEditDialog() : BaseDialogFragment(R.layout.dialog_read_aloud_book, true),
    Toolbar.OnMenuItemClickListener , AdapterView.OnItemSelectedListener{
    constructor(bookUrl: String) : this() {
        arguments = Bundle().apply {
            putString("id", bookUrl)
        }
    }

    private val speakers: MutableList<SpinnerOption> = mutableListOf()
    private val refers: MutableList<SpinnerOption> = mutableListOf()
    private val binding by viewBinding(DialogReadAloudBookBinding::bind)
    private val viewModel by viewModels<ReadAloudBookEditViewModel>()
    private var modeType:Int?=-1
    private var beforeType:Int?=-1
    private var clearSpeaker:Boolean = false


    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        val bg = requireContext().bottomBackground
        binding.spRefer.onItemSelectedListener = this
        viewModel.initData(arguments) {
            var data = viewModel.bookData.value
            data?.let {
                initRefer(it.modelId)
                appDb.localTTSDao.get(it.modelId)?.let {tts->
                    modeType = tts.type
                    beforeType = tts.type
                    initSpeaker(tts.type,it.speakerId,it.dialogueId)
                }
            } 
            
        }

        initMenu()
    }

    private fun initSpeaker(type: Int,select: Long?=0,dialog:Long?=0) {
        var selectIndex:Int = 0
        var selectDialog:Int = 0
        speakers.add(SpinnerOption())
        var index = 1
        var data = appDb.ttsSpeakerDao.findByTypeAndDownload(type, true)
        for (sp in data) {
            speakers.add(SpinnerOption(id = sp.id, text = sp.name!!))
            if(select==sp.id){
                selectIndex = index 
            }
            if(dialog==sp.id){
                selectDialog = index
            }
            index++
        } 
        initSpeakerSelect(speakers,selectIndex)
        initDialogSelect(speakers,selectDialog)
    }
    
    private fun initSpeakerSelect(speaker:List<SpinnerOption>,index:Int){
        var speakerAdapter = ArrayAdapter<SpinnerOption>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            speaker
        )
        speakerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spSpeaker.adapter = speakerAdapter
        binding.spSpeaker.setSelection(index)
    }

    private fun initDialogSelect(speaker:List<SpinnerOption>,index:Int){
        var speakerAdapter = ArrayAdapter<SpinnerOption>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            speaker
        )
        speakerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spDialogue.adapter = speakerAdapter
        binding.spDialogue.setSelection(index)
    }

     
    
    
    


    private fun initRefer(select:Long?=0) {
        var selectIndex:Int = 0
        refers.add(SpinnerOption(text = " "))
        var index = 1
        var data = appDb.localTTSDao.findMultiSpeakerModel();
        for (sp in data) {
            refers.add(SpinnerOption(id = sp.id, text = sp.name!!))
            if(select==sp.id){
                selectIndex = index
            }
            index++
        }
        initModelSelect(refers,selectIndex)
    }

    private fun initModelSelect(model:List<SpinnerOption>,index:Int){
        var referAdapter = ArrayAdapter<SpinnerOption>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            model
        )
        referAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spRefer.adapter = referAdapter
        binding.spRefer.setSelection(index)
    }



    fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.speak_engine_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> viewModel.save(dataFromView()) {
                toastOnUi("保存成功")
                    viewModel.bookData.value?.let {
                        appDb.bookSpeakerDao.findByBook(it.bookUrl)?.let {speakers->
                            for(speaker in speakers) {
                                speaker.modelId = it.modelId
                                if(clearSpeaker){
                                    speaker.speakerId = 0L
                                }
                                appDb.bookSpeakerDao.update(speaker)
                            }
                        }
                    }
                dismiss()
            }
        }
        return true
    }


    private fun dataFromView(): ReadAloudBook {
        viewModel.bookData.value?.let {
            var selectSpeaker = speakers[binding.spSpeaker.selectedItemPosition]
            it.speakerId = selectSpeaker.id
            var selectDialog = speakers[binding.spDialogue.selectedItemPosition]
            it.dialogueId = selectDialog.id
        }
        return viewModel.bookData.value!!
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        if(refers.isNotEmpty()){
            var id =  refers[p2].id
            appDb.localTTSDao.get(id)?.let {
                if(beforeType!=it.type){
                    clearSpeaker = true
                }else{
                    clearSpeaker = false
                }
                viewModel.bookData.value?.modelId = id
                if(modeType!=it.type){
                    modeType = it.type
                    speakers.clear()
                    initSpeaker(it.type)
                }

            }
        }

    }

    override fun onNothingSelected(p0: AdapterView<*>?) {

    }


}