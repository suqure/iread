package ltd.finelink.read.ui.store.book

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseActivity
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.constant.IntentAction
import ltd.finelink.read.constant.Theme
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.BookChapter
import ltd.finelink.read.data.entities.BookSpeaker
import ltd.finelink.read.data.entities.LLMConfig
import ltd.finelink.read.data.entities.ReadAloudBook
import ltd.finelink.read.databinding.ActivityReadAloudBookInfoBinding
import ltd.finelink.read.help.book.ContentProcessor
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.lib.theme.backgroundColor
import ltd.finelink.read.lib.theme.bottomBackground
import ltd.finelink.read.lib.theme.getPrimaryTextColor
import ltd.finelink.read.model.BookCover
import ltd.finelink.read.model.ReadAloud
import ltd.finelink.read.service.ReadAloudBookService
import ltd.finelink.read.ui.book.read.ReadBookActivity
import ltd.finelink.read.ui.book.read.config.SpeakEngineDialog
import ltd.finelink.read.ui.store.book.speaker.SpeakerDetailActivity
import ltd.finelink.read.ui.widget.dialog.WaitDialog
import ltd.finelink.read.utils.ColorUtils
import ltd.finelink.read.utils.gone
import ltd.finelink.read.utils.observeEvent
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.startActivity
import ltd.finelink.read.utils.startService
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import ltd.finelink.read.utils.visible

class ReadAloudBookActivity :
    VMBaseActivity<ActivityReadAloudBookInfoBinding, ReadAloudBookViewModel>(toolBarTheme = Theme.Dark),
    BookSpeakerAdapter.CallBack {

    private val waitDialog by lazy { WaitDialog(this) }

    override val binding by viewBinding(ActivityReadAloudBookInfoBinding::inflate)
    override val viewModel by viewModels<ReadAloudBookViewModel>()
    private val layoutManager by lazy { LinearLayoutManager(this) }
    private val adapter by lazy { BookSpeakerAdapter(this, this) }


    @SuppressLint("PrivateResource")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.setBackgroundResource(R.color.transparent)
        binding.refreshLayout?.setColorSchemeColors(accentColor)
        binding.arcView.setBgColor(backgroundColor)
        binding.llInfo.setBackgroundColor(backgroundColor)
        binding.flAction.setBackgroundColor(bottomBackground)
        binding.tvShelf.setTextColor(getPrimaryTextColor(ColorUtils.isColorLight(bottomBackground)))
        initRecyclerView()
        viewModel.llm.observe(this) {
            showAnalyse(it)
        }
        viewModel.bookData.observe(this) { showBook(it) }
        viewModel.waitDialogData.observe(this) { upWaitDialogStatus(it) }
        viewModel.speakerListData.observe(this) {
            adapter.setItems(it)
            if(it.isNotEmpty()){
                binding.llAnalyseInfo.visible()
            }
        }
        viewModel.readAloudBookData.observe(this) {
            showBookAnalyse(it)
        }
        viewModel.initData(intent)
        initViewEvent()

    }

    private fun initRecyclerView() {
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.read_aloud_book_info, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    private fun showBookAnalyse(readAloudBook: ReadAloudBook) {
        binding.swAdvanceMode.isChecked = readAloudBook.advanceMode
        if (readAloudBook.advanceMode) {
            binding.llDialogue.gone()
            viewModel.speakerListData.value?.let {
                if(it.isNotEmpty()){
                    binding.llAnalyseInfo.visible()
                }
            }
            binding.llAnalyse.visible()
            binding.recyclerView.visible()
        } else {
            binding.llDialogue.visible()
            binding.llAnalyse.gone()
            binding.llAnalyseInfo.gone()
            binding.recyclerView.gone()
        }
        if (readAloudBook.modelId > 0) {
            appDb.localTTSDao.get(readAloudBook.modelId)?.let {
                binding.tvModel.text = getString(R.string.engine_s, it.name)
            }
        } else {
            binding.tvModel.text = getString(R.string.engine_s, getString(R.string.not_set))
        }

        if (readAloudBook.durChapterIndex > 0 ) {
            if(readAloudBook.durChapterIndex >= readAloudBook.totalChapterNum){
                binding.tvLlm.text = getString(R.string.analyse_success)
            }else {
                binding.tvLlm.text = getString(R.string.analyse_portion)
            }

        }

        if (readAloudBook.speakerId > 0) {
            appDb.ttsSpeakerDao.get(readAloudBook.speakerId)?.let {
                binding.tvSpeaker.text = getString(R.string.voice_over_s, it.name)
            }
        } else {
            binding.tvSpeaker.text =
                getString(R.string.voice_over_s, getString(R.string.text_default))
        }
        if (readAloudBook.dialogueId > 0) {
            appDb.ttsSpeakerDao.get(readAloudBook.dialogueId)?.let {
                binding.tvDialogue.text = getString(R.string.dialogue_s, it.name)
            }
        } else {
            binding.tvDialogue.text =
                getString(R.string.dialogue_s, getString(R.string.text_default))
        }
    }


    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> {
                refreshBook()
            }

            R.id.menu_clear_cache -> alert(
                titleResource = R.string.draw,
                messageResource = R.string.sure_del_book_cache
            ) {
                yesButton {
                    viewModel.clearCache()
                }
                noButton()
            }
            R.id.menu_clear_llm -> alert(
                titleResource = R.string.draw,
                messageResource = R.string.clear_llm_confirm
            ) {
                yesButton {
                    viewModel.clearLLm()
                }
                noButton()
            }
            R.id.menu_aloud_config -> {
                showDialogFragment(SpeakEngineDialog(object: SpeakEngineDialog.CallBack {
                    override fun upSpeakEngineSummary() {

                    }
                }))
            }

        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun showAnalyse(llmConfig: LLMConfig) = binding.run {
        if (llmConfig.progress == 100) {
            tvLlm.text = getString(R.string.llm_ready)
            tvLlmView.visible()
        } else {
            tvLlm.text = getString(R.string.download_llm, "${llmConfig.progress}%")
        }
    }

    override fun observeLiveBus() {
        observeEvent<LLMConfig>(EventBus.UP_LLM_DOWNLOAD) {
            if (it.id == -1L) {
                viewModel.llm.postValue(it)
            }
        }
        observeEvent<ReadAloudBook>(EventBus.UP_ALOUD_BOOK) {
            viewModel.readAloudBookData.value?.let { book ->
                it.bookUrl == book.bookUrl
                viewModel.readAloudBookData.postValue(it)
            }
        }
        observeEvent<BookSpeaker>(EventBus.UP_BOOK_SPEAKER) {
            adapter.changeItem(it)
            binding.llAnalyseInfo.visible()
        }

        observeEvent<String>(EventBus.UP_BOOK_SPEAKER_DETAIL) {
            viewModel.readAloudBookData.value?.let { book ->
                it == book.bookUrl
                var speakers = appDb.bookSpeakerDao.findByBook(it)
                viewModel.speakerListData.postValue(speakers)
            }
        }
        observeEvent<Book>(EventBus.FINISH_BOOK_ANALYSE) {
            viewModel.bookData.value?.let { book ->
                if (it.bookUrl == book.bookUrl) {
                    binding.llAnalyse.visible()
                    binding.tvLlmView.visible()
                    appDb.readAloudBookDao.get(it.bookUrl)?.let {
                        it.durChapterIndex += 1
                        appDb.readAloudBookDao.update(it)
                        viewModel.readAloudBookData.postValue(it)
                    }
                    binding.tvAnalyse.text =""
                }
            }
        }
        observeEvent<BookChapter>(EventBus.UP_BOOK_ANALYSE) {
            viewModel.bookData.value?.let { book ->
                if (it.bookUrl == book.bookUrl) {
                    val contentProcessor = ContentProcessor.get(book.name, book.origin)
                    val displayTitle = it.getDisplayTitle(
                        contentProcessor.getTitleReplaceRules(),
                        book.getUseReplaceRule()
                    )
                    binding.llAnalyse.gone()
                    binding.llAnalyseInfo.visible()
                    binding.tvAnalyse.text = getString(R.string.analyse_book_process, displayTitle)
                }
            }
        }
    }


    private fun refreshBook() {
        viewModel.readAloudBookData.value?.let { it ->
            appDb.bookSpeakerDao.findByBook(it.bookUrl)?.let {speakers->
                viewModel.speakerListData.postValue(speakers)
            }
            appDb.readAloudBookDao.get(it.bookUrl).let {data->
                viewModel.readAloudBookData.postValue(data)
            }
        }
    }

    private fun showBook(book: Book) = binding.run {
        showCover(book)
        tvName.text = book.name
    }

    private fun showCover(book: Book) {
        binding.ivCover.load(book.getDisplayCover(), book.name, book.author, false, book.origin)
        if (!AppConfig.isEInkMode) {
            BookCover.loadBlur(this, book.getDisplayCover())
                .into(binding.bgBook)
        }
    }


    private fun initViewEvent() = binding.run {
        tvSetting.setOnClickListener {
            viewModel.bookData.value?.let {
                showDialogFragment(ReadAloudBookEditDialog(it.bookUrl))
            }
        }
        tvShelf.setOnClickListener {
            viewModel.readAloudBookData.value?.let {
                if(it.modelId>0){
                    AppConfig.ttsEngine = it.modelId.toString()
                    ReadAloud.upReadAloudClass()
                }
                startActivity<ReadBookActivity> {
                    putExtra("bookUrl", it.bookUrl)
                    putExtra("readAloud", true)
                }
            }

        }
        swAdvanceMode.setOnClickListener {
            if (swAdvanceMode.isChecked) {
                binding.llDialogue.gone()
                binding.llAnalyse.visible()
                viewModel.speakerListData.value?.let {
                    if(it.isNotEmpty()){
                        binding.llAnalyseInfo.visible()
                        binding.recyclerView.visible()
                    }
                }
            } else {
                binding.llDialogue.visible()
                binding.llAnalyse.gone()
                binding.llAnalyseInfo.gone()
                binding.recyclerView.gone()
            }
            viewModel.readAloudBookData.value?.let {
                it.advanceMode = swAdvanceMode.isChecked
                appDb.readAloudBookDao.update(it)
            }
        }
        tvAnalyseView.setOnClickListener {
            viewModel.bookData.value?.let {
                startActivity<SpeakerDetailActivity>{
                    putExtra("bookUrl", it.bookUrl)
                }
            }
        }
        tvLlmView.setOnClickListener {
            viewModel.llm.value?.let { llm ->
                if (llm.download) {
                    viewModel.readAloudBookData.value?.let {data->
                        if(data.durChapterIndex>=data.totalChapterNum){
                            alert(
                                titleResource = R.string.draw,
                                messageResource = R.string.analyse_again
                            ) {
                                yesButton {
                                    appDb.bookSpeakerDao.deleteByBook(data.bookUrl)
                                    viewModel.speakerListData.postValue(arrayListOf())
                                    tvLlm.text = getString(R.string.analyse_wait)
                                    tvLlmView.gone()
                                    startService<ReadAloudBookService> {
                                        action = IntentAction.start
                                        putExtra("bookUrl", data.bookUrl)
                                        putExtra("start",0)
                                    }
                                }
                                noButton()
                            }
                        }else{
                            if(data.durChapterIndex>0){
                                alert(
                                    titleResource = R.string.draw,
                                    messageResource = R.string.analyse_continue
                                ) {
                                    yesButton {
                                        tvLlm.text = getString(R.string.analyse_wait)
                                        tvLlmView.gone()
                                        startService<ReadAloudBookService> {
                                            action = IntentAction.start
                                            putExtra("bookUrl", data.bookUrl)

                                        }
                                    }
                                    noButton {
                                        tvLlm.text = getString(R.string.analyse_wait)
                                        tvLlmView.gone()
                                        startService<ReadAloudBookService> {
                                            action = IntentAction.start
                                            putExtra("bookUrl", data.bookUrl)
                                            putExtra("start",0)
                                        }
                                    }
                                }
                            }else{
                                alert(
                                    titleResource = R.string.draw,
                                    messageResource = R.string.analyse_confirm
                                ) {
                                    yesButton {
                                        tvLlm.text = getString(R.string.analyse_wait)
                                        tvLlmView.gone()
                                        startService<ReadAloudBookService> {
                                            action = IntentAction.start
                                            putExtra("bookUrl", data.bookUrl)
                                        }
                                    }
                                    noButton()
                                }
                            }

                        }
                    }
                }
            }
        }

        refreshLayout?.setOnRefreshListener {
            refreshLayout.isRefreshing = false
            refreshBook()
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

    override fun edit(source: BookSpeaker) {
        showDialogFragment(BookSpeakerEditDialog(source.id))
    }


}