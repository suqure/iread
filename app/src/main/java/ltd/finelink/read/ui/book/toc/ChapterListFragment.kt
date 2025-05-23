package ltd.finelink.read.ui.book.toc

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseFragment
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.BookChapter
import ltd.finelink.read.databinding.FragmentChapterListBinding
import ltd.finelink.read.help.book.BookHelp
import ltd.finelink.read.help.book.isLocal
import ltd.finelink.read.lib.theme.bottomBackground
import ltd.finelink.read.lib.theme.getPrimaryTextColor
import ltd.finelink.read.ui.widget.recycler.UpLinearLayoutManager
import ltd.finelink.read.ui.widget.recycler.VerticalDivider
import ltd.finelink.read.utils.ColorUtils
import ltd.finelink.read.utils.observeEvent
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChapterListFragment : VMBaseFragment<TocViewModel>(R.layout.fragment_chapter_list),
    ChapterListAdapter.Callback,
    TocViewModel.ChapterListCallBack {
    override val viewModel by activityViewModels<TocViewModel>()
    private val binding by viewBinding(FragmentChapterListBinding::bind)
    private val mLayoutManager by lazy { UpLinearLayoutManager(requireContext()) }
    private val adapter by lazy { ChapterListAdapter(requireContext(), this) }
    private var durChapterIndex = 0

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) = binding.run {
        viewModel.chapterListCallBack = this@ChapterListFragment
        val bbg = bottomBackground
        val btc = requireContext().getPrimaryTextColor(ColorUtils.isColorLight(bbg))
        llChapterBaseInfo.setBackgroundColor(bbg)
        tvCurrentChapterInfo.setTextColor(btc)
        ivChapterTop.setColorFilter(btc, PorterDuff.Mode.SRC_IN)
        ivChapterBottom.setColorFilter(btc, PorterDuff.Mode.SRC_IN)
        initRecyclerView()
        initView()
        viewModel.bookData.observe(this@ChapterListFragment) {
            initBook(it)
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.layoutManager = mLayoutManager
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = adapter
    }

    private fun initView() = binding.run {
        ivChapterTop.setOnClickListener {
            mLayoutManager.scrollToPositionWithOffset(0, 0)
        }
        ivChapterBottom.setOnClickListener {
            if (adapter.itemCount > 0) {
                mLayoutManager.scrollToPositionWithOffset(adapter.itemCount - 1, 0)
            }
        }
        tvCurrentChapterInfo.setOnClickListener {
            mLayoutManager.scrollToPositionWithOffset(durChapterIndex, 0)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initBook(book: Book) {
        lifecycleScope.launch {
            upChapterList(null)
            durChapterIndex = book.durChapterIndex
            binding.tvCurrentChapterInfo.text =
                "${book.durChapterTitle}(${book.durChapterIndex + 1}/${book.totalChapterNum})"
            initCacheFileNames(book)
        }
    }

    private fun initCacheFileNames(book: Book) {
        lifecycleScope.launch(IO) {
            adapter.cacheFileNames.addAll(BookHelp.getChapterFiles(book))
            withContext(Main) {
                adapter.notifyItemRangeChanged(0, adapter.itemCount, true)
            }
        }
    }

    override fun observeLiveBus() {
        observeEvent<Pair<Book, BookChapter>>(EventBus.SAVE_CONTENT) { (book, chapter) ->
            viewModel.bookData.value?.bookUrl?.let { bookUrl ->
                if (book.bookUrl == bookUrl) {
                    adapter.cacheFileNames.add(chapter.getFileName())
                    if (viewModel.searchKey.isNullOrEmpty()) {
                        adapter.notifyItemChanged(chapter.index, true)
                    } else {
                        adapter.getItems().forEachIndexed { index, bookChapter ->
                            if (bookChapter.index == chapter.index) {
                                adapter.notifyItemChanged(index, true)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun upChapterList(searchKey: String?) {
        lifecycleScope.launch {
            withContext(IO) {
                when {
                    searchKey.isNullOrBlank() -> appDb.bookChapterDao.getChapterList(viewModel.bookUrl)
                    else -> appDb.bookChapterDao.search(viewModel.bookUrl, searchKey)
                }
            }.let {
                adapter.setItems(it)
            }
        }
    }

    override fun onListChanged() {
        lifecycleScope.launch {
            var scrollPos = 0
            withContext(Default) {
                adapter.getItems().forEachIndexed { index, bookChapter ->
                    if (bookChapter.index >= durChapterIndex) {
                        return@withContext
                    }
                    scrollPos = index
                }
            }
            mLayoutManager.scrollToPositionWithOffset(scrollPos, 0)
            adapter.upDisplayTitles(scrollPos)
        }
    }

    override fun clearDisplayTitle() {
        adapter.clearDisplayTitle()
        adapter.upDisplayTitles(mLayoutManager.findFirstVisibleItemPosition())
    }

    override val scope: CoroutineScope
        get() = lifecycleScope

    override val book: Book?
        get() = viewModel.bookData.value

    override val isLocalBook: Boolean
        get() = viewModel.bookData.value?.isLocal == true

    override fun durChapterIndex(): Int {
        return durChapterIndex
    }

    override fun openChapter(bookChapter: BookChapter) {
        activity?.run {
            setResult(RESULT_OK, Intent()
                .putExtra("index", bookChapter.index)
                .putExtra("chapterChanged", bookChapter.index != durChapterIndex)
            )
            finish()
        }
    }

}