package ltd.finelink.read.ui.book.changesource

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseDialogFragment
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.BookChapter
import ltd.finelink.read.data.entities.BookSource
import ltd.finelink.read.data.entities.SearchBook
import ltd.finelink.read.databinding.DialogChapterChangeSourceBinding
import ltd.finelink.read.help.book.BookHelp
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.theme.elevation
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.ui.book.read.ReadBookActivity
import ltd.finelink.read.ui.book.source.edit.BookSourceEditActivity
import ltd.finelink.read.ui.book.source.manage.BookSourceActivity
import ltd.finelink.read.ui.widget.recycler.VerticalDivider
import ltd.finelink.read.utils.StartActivityContract
import ltd.finelink.read.utils.applyTint
import ltd.finelink.read.utils.cnCompare
import ltd.finelink.read.utils.dpToPx
import ltd.finelink.read.utils.gone
import ltd.finelink.read.utils.observeEvent
import ltd.finelink.read.utils.setLayout
import ltd.finelink.read.utils.startActivity
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import ltd.finelink.read.utils.visible
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch


class ChangeChapterSourceDialog() : BaseDialogFragment(R.layout.dialog_chapter_change_source),
    Toolbar.OnMenuItemClickListener,
    ChangeChapterSourceAdapter.CallBack,
    ChangeChapterTocAdapter.Callback {

    constructor(name: String, author: String, chapterIndex: Int, chapterTitle: String) : this() {
        arguments = Bundle().apply {
            putString("name", name)
            putString("author", author)
            putInt("chapterIndex", chapterIndex)
            putString("chapterTitle", chapterTitle)
        }
    }

    private val binding by viewBinding(DialogChapterChangeSourceBinding::bind)
    private val groups = linkedSetOf<String>()
    private val callBack: CallBack? get() = activity as? CallBack
    private val viewModel: ChangeChapterSourceViewModel by viewModels()
    private val editSourceResult =
        registerForActivityResult(StartActivityContract(BookSourceEditActivity::class.java)) {
            viewModel.startSearch()
        }
    private val searchBookAdapter by lazy {
        ChangeChapterSourceAdapter(requireContext(), viewModel, this)
    }
    private val tocAdapter by lazy {
        ChangeChapterTocAdapter(requireContext(), this)
    }
    private val contentSuccess: (content: String) -> Unit = {
        binding.loadingToc.gone()
        callBack?.replaceContent(it)
        dismissAllowingStateLoss()
    }
    private var searchBook: SearchBook? = null
    private val searchFinishCallback: (isEmpty: Boolean) -> Unit = {
        if (it) {
            val searchGroup = AppConfig.searchGroup
            if (searchGroup.isNotEmpty()) {
                lifecycleScope.launch {
                    context?.alert("搜索结果为空") {
                        setMessage("${searchGroup}分组搜索结果为空,是否切换到全部分组")
                        noButton()
                        yesButton {
                            AppConfig.searchGroup = ""
                            upGroupMenu()
                            viewModel.startSearch()
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        viewModel.initData(arguments, callBack?.oldBook, activity is ReadBookActivity)
        showTitle()
        initMenu()
        initView()
        initRecyclerView()
        initSearchView()
        initBottomBar()
        initLiveData()
        viewModel.searchFinishCallback = searchFinishCallback
        activity?.onBackPressedDispatcher?.addCallback(this) {
            if (binding.clToc.isVisible) {
                binding.clToc.gone()
                return@addCallback
            }
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.searchFinishCallback = null
    }

    private fun showTitle() {
        binding.toolBar.title = viewModel.chapterTitle
    }

    private fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.change_source)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.toolBar.menu.findItem(R.id.menu_check_author)
            ?.isChecked = AppConfig.changeSourceCheckAuthor
        binding.toolBar.menu.findItem(R.id.menu_load_info)
            ?.isChecked = AppConfig.changeSourceLoadInfo
        binding.toolBar.menu.findItem(R.id.menu_load_toc)
            ?.isChecked = AppConfig.changeSourceLoadToc
        binding.toolBar.menu.findItem(R.id.menu_load_word_count)
            ?.isChecked = AppConfig.changeSourceLoadWordCount
    }

    private fun initView() {
        binding.ivHideToc.setOnClickListener {
            binding.clToc.gone()
        }
        binding.flHideToc.elevation = requireContext().elevation
    }

    private fun initRecyclerView() {
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = searchBookAdapter
        searchBookAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                if (toPosition == 0) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }
        })
        binding.recyclerViewToc.adapter = tocAdapter
    }

    private fun initSearchView() {
        val searchView = binding.toolBar.menu.findItem(R.id.menu_screen).actionView as SearchView
        searchView.setOnCloseListener {
            showTitle()
            false
        }
        searchView.setOnSearchClickListener {
            binding.toolBar.title = ""
            binding.toolBar.subtitle = ""
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.screen(newText)
                return false
            }

        })
    }

    private fun initBottomBar() {
        binding.tvDur.text = callBack?.oldBook?.originName
        binding.tvDur.setOnClickListener {
            scrollToDurSource()
        }
        binding.ivTop.setOnClickListener {
            binding.recyclerView.scrollToPosition(0)
        }
        binding.ivBottom.setOnClickListener {
            binding.recyclerView.scrollToPosition(searchBookAdapter.itemCount - 1)
        }
    }

    private fun initLiveData() {
        viewModel.searchStateData.observe(viewLifecycleOwner) {
            binding.refreshProgressBar.isAutoLoading = it
            if (it) {
                startStopMenuItem?.let { item ->
                    item.setIcon(R.drawable.ic_stop_black_24dp)
                    item.setTitle(R.string.stop)
                }
            } else {
                startStopMenuItem?.let { item ->
                    item.setIcon(R.drawable.ic_refresh_black_24dp)
                    item.setTitle(R.string.refresh)
                }
            }
            binding.toolBar.menu.applyTint(requireContext())
        }
        lifecycleScope.launch {
            repeatOnLifecycle(STARTED) {
                viewModel.searchDataFlow.conflate().collect {
                    searchBookAdapter.setItems(it)
                    delay(1000)
                }
            }
        }
        lifecycleScope.launch {
            appDb.bookSourceDao.flowEnabledGroups().conflate().collect {
                groups.clear()
                groups.addAll(it)
                upGroupMenu()
            }
        }
    }

    private val startStopMenuItem: MenuItem?
        get() = binding.toolBar.menu.findItem(R.id.menu_start_stop)

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_check_author -> {
                AppConfig.changeSourceCheckAuthor = !item.isChecked
                item.isChecked = !item.isChecked
                viewModel.refresh()
            }

            R.id.menu_load_info -> {
                AppConfig.changeSourceLoadInfo = !item.isChecked
                item.isChecked = !item.isChecked
            }

            R.id.menu_load_toc -> {
                AppConfig.changeSourceLoadToc = !item.isChecked
                item.isChecked = !item.isChecked
            }

            R.id.menu_load_word_count -> {
                AppConfig.changeSourceLoadWordCount = !item.isChecked
                item.isChecked = !item.isChecked
                viewModel.onLoadWordCountChecked(item.isChecked)
            }

            R.id.menu_start_stop -> viewModel.startOrStopSearch()
            R.id.menu_source_manage -> startActivity<BookSourceActivity>()
            else -> if (item?.groupId == R.id.source_group && !item.isChecked) {
                item.isChecked = true
                if (item.title.toString() == getString(R.string.all_source)) {
                    AppConfig.searchGroup = ""
                } else {
                    AppConfig.searchGroup = item.title.toString()
                }
                lifecycleScope.launch(IO) {
                    viewModel.stopSearch()
                    if (viewModel.refresh()) {
                        viewModel.startSearch()
                    }
                }
            }
        }
        return false
    }

    private fun scrollToDurSource() {
        searchBookAdapter.getItems().forEachIndexed { index, searchBook ->
            if (searchBook.bookUrl == oldBookUrl) {
                (binding.recyclerView.layoutManager as LinearLayoutManager)
                    .scrollToPositionWithOffset(index, 60.dpToPx())
                return
            }
        }
    }

    override fun openToc(searchBook: SearchBook) {
        this.searchBook = searchBook
        tocAdapter.setItems(null)
        binding.clToc.visible()
        binding.loadingToc.visible()
        val book = searchBook.toBook()
        viewModel.getToc(book, {
            binding.clToc.gone()
            toastOnUi(it)
        }) { toc: List<BookChapter>, _: BookSource ->
            tocAdapter.durChapterIndex =
                BookHelp.getDurChapter(viewModel.chapterIndex, viewModel.chapterTitle, toc)
            binding.loadingToc.gone()
            tocAdapter.setItems(toc)
            binding.recyclerViewToc.scrollToPosition(tocAdapter.durChapterIndex - 5)
        }
    }

    override val oldBookUrl: String?
        get() = callBack?.oldBook?.bookUrl

    override fun topSource(searchBook: SearchBook) {
        viewModel.topSource(searchBook)
    }

    override fun bottomSource(searchBook: SearchBook) {
        viewModel.bottomSource(searchBook)
    }

    override fun editSource(searchBook: SearchBook) {
        editSourceResult.launch {
            putExtra("sourceUrl", searchBook.origin)
        }
    }

    override fun disableSource(searchBook: SearchBook) {
        viewModel.disableSource(searchBook)
    }

    override fun deleteSource(searchBook: SearchBook) {
        viewModel.del(searchBook)
        if (oldBookUrl == searchBook.bookUrl) {
            viewModel.autoChangeSource(callBack?.oldBook?.type) { book, toc, source ->
                callBack?.changeTo(source, book, toc)
            }
        }
    }

    override fun setBookScore(searchBook: SearchBook, score: Int) {
        viewModel.setBookScore(searchBook, score)
    }

    override fun getBookScore(searchBook: SearchBook): Int {
        return viewModel.getBookScore(searchBook)
    }

    override fun clickChapter(bookChapter: BookChapter, nextChapterUrl: String?) {
        searchBook?.let {
            binding.loadingToc.visible()
            viewModel.getContent(it.toBook(), bookChapter, nextChapterUrl, contentSuccess) { msg ->
                binding.loadingToc.gone()
                binding.clToc.gone()
                toastOnUi(msg)
            }
        }
    }

    /**
     * 更新分组菜单
     */
    private fun upGroupMenu() {
        binding.toolBar.menu.findItem(R.id.menu_group)?.subMenu?.let { menu ->
            val selectedGroup = AppConfig.searchGroup
            menu.removeGroup(R.id.source_group)
            val allItem = menu.add(R.id.source_group, Menu.NONE, Menu.NONE, R.string.all_source)
            var hasSelectedGroup = false
            groups.sortedWith { o1, o2 ->
                o1.cnCompare(o2)
            }.forEach { group ->
                menu.add(R.id.source_group, Menu.NONE, Menu.NONE, group)?.let {
                    if (group == selectedGroup) {
                        it.isChecked = true
                        hasSelectedGroup = true
                    }
                }
            }
            menu.setGroupCheckable(R.id.source_group, true, true)
            if (!hasSelectedGroup) {
                allItem.isChecked = true
            }
        }
    }

    override fun observeLiveBus() {
        observeEvent<String>(EventBus.SOURCE_CHANGED) {
            searchBookAdapter.notifyItemRangeChanged(
                0,
                searchBookAdapter.itemCount,
                bundleOf(Pair("upCurSource", oldBookUrl))
            )
        }
    }

    interface CallBack {
        val oldBook: Book?
        fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>)
        fun replaceContent(content: String)
    }

}