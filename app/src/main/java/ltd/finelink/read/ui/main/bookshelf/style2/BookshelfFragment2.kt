package ltd.finelink.read.ui.main.bookshelf.style2

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ltd.finelink.read.R
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.BookGroup
import ltd.finelink.read.databinding.FragmentBookshelf2Binding
import ltd.finelink.read.help.book.isAudio
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.ui.book.audio.AudioPlayActivity
import ltd.finelink.read.ui.book.group.GroupEditDialog
import ltd.finelink.read.ui.book.import.local.ImportBookActivity
import ltd.finelink.read.ui.book.info.BookInfoActivity
import ltd.finelink.read.ui.book.read.ReadBookActivity
import ltd.finelink.read.ui.book.search.SearchActivity
import ltd.finelink.read.ui.main.bookshelf.BaseBookshelfFragment
import ltd.finelink.read.utils.cnCompare
import ltd.finelink.read.utils.observeEvent
import ltd.finelink.read.utils.setEdgeEffectColor
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.startActivity
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import kotlin.math.max

/**
 * 书架界面
 */
class BookshelfFragment2() : BaseBookshelfFragment(R.layout.fragment_bookshelf2),
    SearchView.OnQueryTextListener,
    BaseBooksAdapter.CallBack {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    private val binding by viewBinding(FragmentBookshelf2Binding::bind)
    private val bookshelfLayout by lazy { AppConfig.bookshelfLayout }
    private val booksAdapter: BaseBooksAdapter<*> by lazy {
        if (bookshelfLayout == 0) {
            BooksAdapterList(requireContext(), this)
        } else {
            BooksAdapterGrid(requireContext(), this)
        }
    }
    private var bookGroups: List<BookGroup> = emptyList()
    private var booksFlowJob: Job? = null
    override var groupId = BookGroup.IdRoot
    override var books: List<Book> = emptyList()

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        initRecyclerView()
        initBookGroupData()
        initBooksData()
        binding.ivMenuAdd.setOnClickListener{
            startActivity<ImportBookActivity>()
        }
    }

    private fun initRecyclerView() {
        binding.rvBookshelf.setEdgeEffectColor(primaryColor)
        binding.refreshLayout.setColorSchemeColors(accentColor)
        binding.refreshLayout.setOnRefreshListener {
            binding.refreshLayout.isRefreshing = false
            activityViewModel.upToc(books)
        }
        if (bookshelfLayout == 0) {
            binding.rvBookshelf.layoutManager = LinearLayoutManager(context)
        } else {
            binding.rvBookshelf.layoutManager = GridLayoutManager(context, bookshelfLayout + 2)
        }
        binding.rvBookshelf.adapter = booksAdapter
        booksAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                val layoutManager = binding.rvBookshelf.layoutManager
                if (positionStart == 0 && layoutManager is LinearLayoutManager) {
                    val scrollTo = layoutManager.findFirstVisibleItemPosition() - itemCount
                    binding.rvBookshelf.scrollToPosition(max(0, scrollTo))
                }
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                val layoutManager = binding.rvBookshelf.layoutManager
                if (toPosition == 0 && layoutManager is LinearLayoutManager) {
                    val scrollTo = layoutManager.findFirstVisibleItemPosition() - itemCount
                    binding.rvBookshelf.scrollToPosition(max(0, scrollTo))
                }
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun upGroup(data: List<BookGroup>) {
        if (data != bookGroups) {
            bookGroups = data
            booksAdapter.notifyDataSetChanged()
            binding.llEmpty.isGone = getItemCount() > 0
        }
    }

    override fun upSort() {
        initBooksData()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initBooksData() {
        if (groupId == -100L) {
            if (isAdded) {
                binding.titleBar.title = getString(R.string.bookshelf)
                binding.refreshLayout.isEnabled = true
            }
        } else {
            bookGroups.firstOrNull {
                groupId == it.groupId
            }?.let {
                binding.titleBar.title = "${getString(R.string.bookshelf)}(${it.groupName})"
                binding.refreshLayout.isEnabled = it.enableRefresh
            }
        }
        booksFlowJob?.cancel()
        booksFlowJob = lifecycleScope.launch {
            appDb.bookDao.flowByGroup(groupId).map { list ->
                //排序
                when (AppConfig.getBookSortByGroupId(groupId)) {
                    1 -> list.sortedByDescending {
                        it.latestChapterTime
                    }

                    2 -> list.sortedWith { o1, o2 ->
                        o1.name.cnCompare(o2.name)
                    }

                    3 -> list.sortedBy {
                        it.order
                    }
                    else -> list.sortedByDescending {
                        it.durChapterTime
                    }
                }
            }.flowOn(Dispatchers.Default).catch {
                AppLog.put("书架更新出错", it)
            }.conflate().collect { list ->
                if (isAdded) {
                    books = list
                    booksAdapter.notifyDataSetChanged()
                    binding.llEmpty.isGone = getItemCount() > 0
                    delay(100)
                }
            }
        }
    }

    fun back(): Boolean {
        if (groupId != -100L) {
            groupId = -100L
            initBooksData()
            return true
        }
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        SearchActivity.start(requireContext(), query)
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    override fun gotoTop() {
        if (AppConfig.isEInkMode) {
            binding.rvBookshelf.scrollToPosition(0)
        } else {
            binding.rvBookshelf.smoothScrollToPosition(0)
        }
    }

    override fun onItemClick(position: Int) {
        when (val item = getItem(position)) {
            is Book -> when {
                item.isAudio ->
                    startActivity<AudioPlayActivity> {
                        putExtra("bookUrl", item.bookUrl)
                    }
                else -> startActivity<ReadBookActivity> {
                    putExtra("bookUrl", item.bookUrl)
                }
            }
            is BookGroup -> {
                groupId = item.groupId
                initBooksData()
            }
        }
    }

    override fun onItemLongClick(position: Int) {
        when (val item = getItem(position)) {
            is Book -> startActivity<BookInfoActivity> {
                putExtra("name", item.name)
                putExtra("author", item.author)
            }
            is BookGroup -> showDialogFragment(GroupEditDialog(item))
        }
    }

    override fun isUpdate(bookUrl: String): Boolean {
        return activityViewModel.isUpdate(bookUrl)
    }

    override fun getItemCount(): Int {
        return if (groupId == BookGroup.IdRoot) {
            bookGroups.size + books.size
        } else {
            books.size
        }
    }

    override fun getItemType(position: Int): Int {
        if (groupId != BookGroup.IdRoot) {
            return 0
        }
        if (position < bookGroups.size) {
            return 1
        }
        return 0
    }

    override fun getItem(position: Int): Any? {
        if (groupId != BookGroup.IdRoot) {
            return books.getOrNull(position)
        }
        if (position < bookGroups.size) {
            return bookGroups[position]
        }
        return books.getOrNull(position - bookGroups.size)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<String>(EventBus.UP_BOOKSHELF) {
            booksAdapter.notification(it)
        }
        observeEvent<String>(EventBus.BOOKSHELF_REFRESH) {
            booksAdapter.notifyDataSetChanged()
        }
    }
}