package ltd.finelink.read.ui.main.bookshelf.style1.books

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseFragment
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.BookGroup
import ltd.finelink.read.databinding.FragmentBooksBinding
import ltd.finelink.read.help.book.isAudio
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.ui.book.audio.AudioPlayActivity
import ltd.finelink.read.ui.book.import.local.ImportBookActivity
import ltd.finelink.read.ui.book.info.BookInfoActivity
import ltd.finelink.read.ui.book.read.ReadBookActivity
import ltd.finelink.read.ui.main.MainViewModel
import ltd.finelink.read.ui.store.book.ReadAloudBookActivity
import ltd.finelink.read.utils.cnCompare
import ltd.finelink.read.utils.observeEvent
import ltd.finelink.read.utils.setEdgeEffectColor
import ltd.finelink.read.utils.startActivity
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import kotlin.math.max

/**
 * 书架界面
 */
class BooksFragment() : BaseFragment(R.layout.fragment_books),
    BaseBooksAdapter.CallBack {

    constructor(position: Int, group: BookGroup) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        bundle.putLong("groupId", group.groupId)
        bundle.putInt("bookSort", group.getRealBookSort())
        bundle.putBoolean("enableRefresh", group.enableRefresh)
        arguments = bundle
    }

    private val binding by viewBinding(FragmentBooksBinding::bind)
    private val activityViewModel by activityViewModels<MainViewModel>()
    private val bookshelfLayout by lazy { AppConfig.bookshelfLayout }
    private val booksAdapter: BaseBooksAdapter<*> by lazy {
        if (bookshelfLayout == 0) {
            BooksAdapterList(requireContext(), this, viewLifecycleOwner.lifecycle)
        } else {
            BooksAdapterGrid(requireContext(), this)
        }
    }
    private var booksFlowJob: Job? = null
    private var savedInstanceState: Bundle? = null
    var position = 0
        private set
    var groupId = -1L
        private set
    var bookSort = 0
        private set
    private var upLastUpdateTimeJob: Job? = null
    private var defaultScrollBarSize = 0
    private var enableRefresh = true

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        this.savedInstanceState = savedInstanceState
        arguments?.let {
            position = it.getInt("position", 0)
            groupId = it.getLong("groupId", -1)
            bookSort = it.getInt("bookSort", 0)
            enableRefresh = it.getBoolean("enableRefresh", true)
            binding.refreshLayout.isEnabled = enableRefresh
        }
        initRecyclerView()
        upRecyclerData()
        binding.ivMenuAdd.setOnClickListener{
            startActivity<ImportBookActivity>()
        }
    }

    private fun initRecyclerView() {
        binding.rvBookshelf.setEdgeEffectColor(primaryColor)
        defaultScrollBarSize = binding.rvBookshelf.scrollBarSize
        upFastScrollerBar()
        binding.refreshLayout.setColorSchemeColors(accentColor)
        binding.refreshLayout.setOnRefreshListener {
            binding.refreshLayout.isRefreshing = false
            activityViewModel.upToc(booksAdapter.getItems())
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
        startLastUpdateTimeJob()
    }

    private fun upFastScrollerBar() {
        val showBookshelfFastScroller = AppConfig.showBookshelfFastScroller
        binding.rvBookshelf.setFastScrollEnabled(showBookshelfFastScroller)
        if (showBookshelfFastScroller) {
            binding.rvBookshelf.scrollBarSize = 0
        } else {
            binding.rvBookshelf.scrollBarSize = defaultScrollBarSize
        }
    }

    fun upBookSort(sort: Int) {
        binding.root.post {
            arguments?.putInt("bookSort", sort)
            bookSort = sort
            upRecyclerData()
        }
    }

    fun setEnableRefresh(enable: Boolean) {
        enableRefresh = enable
        binding.refreshLayout.isEnabled = enable
    }

    /**
     * 更新书籍列表信息
     */
    private fun upRecyclerData() {
        booksFlowJob?.cancel()
        booksFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            appDb.bookDao.flowByGroup(groupId).map { list ->
                //排序
                when (bookSort) {
                    1 -> list.sortedByDescending { it.latestChapterTime }
                    2 -> list.sortedWith { o1, o2 ->
                        o1.name.cnCompare(o2.name)
                    }

                    3 -> list.sortedBy { it.order }

                    // 综合排序 issue #3192
                    4 -> list.sortedByDescending {
                        max(it.latestChapterTime, it.durChapterTime)
                    }

                    else -> list.sortedByDescending { it.durChapterTime }
                }
            }.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.RESUMED).catch {
                AppLog.put("书架更新出错", it)
            }.conflate().flowOn(Dispatchers.Default).collect { list ->
                binding.llEmpty.isGone = list.isNotEmpty()
                binding.refreshLayout.isEnabled = enableRefresh && list.isNotEmpty()
                booksAdapter.setItems(list)
                recoverPositionState()
                delay(100)
            }
        }
    }

    private fun recoverPositionState() {
        // 恢复书架位置状态
        if (savedInstanceState?.getBoolean("needRecoverState") == true) {
            val layoutManager = binding.rvBookshelf.layoutManager
            if (layoutManager is LinearLayoutManager) {
                val leavePosition = savedInstanceState!!.getInt("leavePosition")
                val leaveOffset = savedInstanceState!!.getInt("leaveOffset")
                layoutManager.scrollToPositionWithOffset(leavePosition, leaveOffset)
            }
            savedInstanceState!!.putBoolean("needRecoverState", false)
        }
    }

    private fun startLastUpdateTimeJob() {
        upLastUpdateTimeJob?.cancel()
        if (!AppConfig.showLastUpdateTime) {
            return
        }
        upLastUpdateTimeJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    booksAdapter.upLastUpdateTime()
                    delay(30 * 1000)
                }
            }
        }
    }

    fun getBooks(): List<Book> {
        return booksAdapter.getItems()
    }

    fun gotoTop() {
        if (AppConfig.isEInkMode) {
            binding.rvBookshelf.scrollToPosition(0)
        } else {
            binding.rvBookshelf.smoothScrollToPosition(0)
        }
    }

    fun getBooksCount(): Int {
        return booksAdapter.itemCount
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 保存书架位置状态
        val layoutManager = binding.rvBookshelf.layoutManager
        if (layoutManager is LinearLayoutManager) {
            val itemPosition = layoutManager.findFirstVisibleItemPosition()
            val currentView = layoutManager.findViewByPosition(itemPosition)
            val viewOffset = currentView?.top
            if (viewOffset != null) {
                outState.putInt("leavePosition", itemPosition)
                outState.putInt("leaveOffset", viewOffset)
                outState.putBoolean("needRecoverState", true)
            } else if (savedInstanceState != null) {
                val leavePosition = savedInstanceState!!.getInt("leavePosition")
                val leaveOffset = savedInstanceState!!.getInt("leaveOffset")
                outState.putInt("leavePosition", leavePosition)
                outState.putInt("leaveOffset", leaveOffset)
                outState.putBoolean("needRecoverState", true)
            }
        }
    }

    override fun open(book: Book) {
        when {
            book.isAudio ->
                startActivity<AudioPlayActivity> {
                    putExtra("bookUrl", book.bookUrl)
                }

            else -> startActivity<ReadBookActivity> {
                putExtra("bookUrl", book.bookUrl)
            }
        }
    }

    override fun openBookInfo(book: Book) {
        startActivity<BookInfoActivity> {
            putExtra("name", book.name)
            putExtra("author", book.author)
        }
    }

    override fun isUpdate(bookUrl: String): Boolean {
        return activityViewModel.isUpdate(bookUrl)
    }

    override fun readAloudBook(book: Book) {
        alert(
            titleResource = R.string.draw,
            messageResource = R.string.read_aloud_tip
        ) {
            yesButton {
                startActivity<ReadBookActivity> {
                    putExtra("bookUrl", book.bookUrl)
                    putExtra("readAloud", true)
                }
            }
            noButton()
        }

    }

    override fun readAloudAnalyse(book: Book) {
        startActivity<ReadAloudBookActivity> {
            putExtra("bookUrl", book.bookUrl)

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<String>(EventBus.UP_BOOKSHELF) {
            booksAdapter.notification(it)
        }
        observeEvent<String>(EventBus.BOOKSHELF_REFRESH) {
            booksAdapter.notifyDataSetChanged()
            startLastUpdateTimeJob()
            upFastScrollerBar()
        }
    }
}
