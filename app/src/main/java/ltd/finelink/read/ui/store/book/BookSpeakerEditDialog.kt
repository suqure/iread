package ltd.finelink.read.ui.store.book

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseDialogFragment
import ltd.finelink.read.base.SpinnerOption
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.BookSpeaker
import ltd.finelink.read.databinding.DialogBookSpeakerBinding
import ltd.finelink.read.lib.theme.bottomBackground
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.utils.applyTint
import ltd.finelink.read.utils.setLayout
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding

class BookSpeakerEditDialog() : BaseDialogFragment(R.layout.dialog_book_speaker, true),
    Toolbar.OnMenuItemClickListener {
    constructor(id: Long) : this() {
        arguments = Bundle().apply {
            putLong("id", id)
        }
    }
    private val speakers: MutableList<SpinnerOption> = mutableListOf()
    private val binding by viewBinding(DialogBookSpeakerBinding::bind)
    private val viewModel by viewModels<BookSpeakerEditViewModel>()
    private var modeType:Int?=0


    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        val bg = requireContext().bottomBackground
        viewModel.initData(arguments) {
            var data = viewModel.bookData.value
            data?.let {
                binding.tvTtsSpeaker.text = it.spkName
                appDb.localTTSDao.get(it.modelId)?.let {tts->
                    modeType = tts.type
                    initSpeaker(tts.type,it.speakerId)
                }
            } 
            
        }
        initMenu()
    }

    private fun initSpeaker(type: Int,select: Long?=0) {
        var selectIndex:Int = 0
        speakers.add(SpinnerOption())
        var index = 1
        var data = appDb.ttsSpeakerDao.findByTypeAndDownload(type, true)
        for (sp in data) {
            speakers.add(SpinnerOption(id = sp.id, text = sp.name!!))
            if(select==sp.id){
                selectIndex = index 
            }
            index++
        } 
        initSpeakerSelect(speakers,selectIndex)
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





    fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.speak_engine_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> viewModel.save(dataFromView()) {
                toastOnUi("保存成功")
                dismiss()
            }
        }
        return true
    }


    private fun dataFromView(): BookSpeaker {
        viewModel.bookData.value?.let {
            var selectSpeaker = speakers[binding.spSpeaker.selectedItemPosition]
            it.speakerId = selectSpeaker.id
        }
        return viewModel.bookData.value!!
    }



}