package ltd.finelink.read.ui.book.toc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseFragment
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Bookmark
import ltd.finelink.read.databinding.FragmentBookmarkBinding
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.ui.book.bookmark.BookmarkDialog
import ltd.finelink.read.ui.widget.recycler.UpLinearLayoutManager
import ltd.finelink.read.ui.widget.recycler.VerticalDivider
import ltd.finelink.read.utils.setEdgeEffectColor
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class BookmarkFragment : VMBaseFragment<TocViewModel>(R.layout.fragment_bookmark),
    BookmarkAdapter.Callback,
    TocViewModel.BookmarkCallBack {
    override val viewModel by activityViewModels<TocViewModel>()
    private val binding by viewBinding(FragmentBookmarkBinding::bind)
    private val mLayoutManager by lazy { UpLinearLayoutManager(requireContext()) }
    private val adapter by lazy { BookmarkAdapter(requireContext(), this) }
    private var durChapterIndex = 0

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.bookMarkCallBack = this
        initRecyclerView()
        viewModel.bookData.observe(this) {
            durChapterIndex = it.durChapterIndex
            upBookmark(null)
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.layoutManager = mLayoutManager
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = adapter
    }

    override fun upBookmark(searchKey: String?) {
        val book = viewModel.bookData.value ?: return
        lifecycleScope.launch {
            when {
                searchKey.isNullOrBlank() -> appDb.bookmarkDao.flowByBook(book.name, book.author)
                else -> appDb.bookmarkDao.flowSearch(book.name, book.author, searchKey)
            }.catch {
                AppLog.put("目录界面获取书签数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).collect {
                adapter.setItems(it)
                var scrollPos = 0
                withContext(Dispatchers.Default) {
                    adapter.getItems().forEachIndexed { index, bookmark ->
                        if (bookmark.chapterIndex >= durChapterIndex) {
                            return@withContext
                        }
                        scrollPos = index
                    }
                }
                mLayoutManager.scrollToPositionWithOffset(scrollPos, 0)
            }
        }
    }


    override fun onClick(bookmark: Bookmark) {
        activity?.run {
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("index", bookmark.chapterIndex)
                putExtra("chapterPos", bookmark.chapterPos)
            })
            finish()
        }
    }

    override fun onLongClick(bookmark: Bookmark, pos: Int) {
        showDialogFragment(BookmarkDialog(bookmark, pos))
    }

}