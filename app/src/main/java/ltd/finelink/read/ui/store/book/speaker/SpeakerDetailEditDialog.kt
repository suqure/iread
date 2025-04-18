package ltd.finelink.read.ui.store.book.speaker

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseDialogFragment
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.BookSpeakerDetail
import ltd.finelink.read.databinding.DialogSpeakerDetailEditBinding
import ltd.finelink.read.help.book.ContentProcessor
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.utils.applyTint
import ltd.finelink.read.utils.setLayout
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding

class SpeakerDetailEditDialog() : BaseDialogFragment(R.layout.dialog_speaker_detail_edit, true),
    Toolbar.OnMenuItemClickListener{

    constructor(id: Long) : this() {
        arguments = Bundle().apply {
            putLong("id", id)
        }
    }

    private val binding by viewBinding(DialogSpeakerDetailEditBinding::bind)
    private val viewModel by viewModels<SpeakerDetailEditViewModel>()


    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        viewModel.initData(arguments) {
            initView(it)
        }
        initMenu()
        initEvent()
    }




    private fun initEvent() = binding.run {

        tvCancel.setOnClickListener{
            dismiss()
        }
        tvOk.setOnClickListener{
            var spkName = binding.tvSpeakerName.text.toString()
            if(spkName.isNotEmpty()){
                viewModel.record?.let {
                    it.spkName = "[$spkName]"
                    viewModel.save(it)
                }
                dismiss()
            }else{
                toastOnUi(getString(R.string.error_req,getString(R.string.speaker)))
            }
        }
    }

    fun initMenu() {
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    fun initView(speaker: BookSpeakerDetail) {
        appDb.bookDao.getBook(speaker.bookUrl)?.let {
            binding.tvBookName.text = it.name
            val contentProcessor = ContentProcessor.get(it.name, it.origin)
            appDb.bookChapterDao.getChapter(speaker.bookUrl,speaker.chapter)?.let {chapter->
                val displayTitle = chapter.getDisplayTitle(
                    contentProcessor.getTitleReplaceRules(),
                    it.getUseReplaceRule()
                )
                binding.tvBookChapter.text = displayTitle
            }
        }
        binding.tvWavText.text = speaker.text
        binding.tvSpeakerName.setText(speaker.spkName.substring(1,speaker.spkName.length-1))
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {

        return true
    }





}

