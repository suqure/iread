package ltd.finelink.read.ui.book.explore

import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.RecyclerView
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseActivity
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.SearchBook
import ltd.finelink.read.databinding.ActivityExploreShowBinding
import ltd.finelink.read.databinding.ViewLoadMoreBinding
import ltd.finelink.read.ui.book.info.BookInfoActivity
import ltd.finelink.read.ui.widget.recycler.LoadMoreView
import ltd.finelink.read.ui.widget.recycler.VerticalDivider
import ltd.finelink.read.utils.startActivity
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding

/**
 * 发现列表
 */
class ExploreShowActivity : VMBaseActivity<ActivityExploreShowBinding, ExploreShowViewModel>(),
    ExploreShowAdapter.CallBack {
    override val binding by viewBinding(ActivityExploreShowBinding::inflate)
    override val viewModel by viewModels<ExploreShowViewModel>()

    private val adapter by lazy { ExploreShowAdapter(this, this) }
    private val loadMoreView by lazy { LoadMoreView(this) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.title = intent.getStringExtra("exploreName")
        initRecyclerView()
        viewModel.booksData.observe(this) { upData(it) }
        viewModel.initData(intent)
        viewModel.errorLiveData.observe(this) {
            loadMoreView.error(it)
        }
        viewModel.upAdapterLiveData.observe(this) {
            adapter.notifyItemRangeChanged(0, adapter.itemCount, it)
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        binding.recyclerView.adapter = adapter
        adapter.addFooterView {
            ViewLoadMoreBinding.bind(loadMoreView)
        }
        loadMoreView.startLoad()
        loadMoreView.setOnClickListener {
            if (!loadMoreView.isLoading) {
                loadMoreView.hasMore()
                scrollToBottom()
            }
        }
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    scrollToBottom()
                }
            }
        })
    }

    private fun scrollToBottom() {
        adapter.let {
            if (loadMoreView.hasMore && !loadMoreView.isLoading) {
                loadMoreView.startLoad()
                viewModel.explore()
            }
        }
    }

    private fun upData(books: List<SearchBook>) {
        loadMoreView.stopLoad()
        if (books.isEmpty() && adapter.isEmpty()) {
            loadMoreView.noMore(getString(R.string.empty))
        } else if (books.isEmpty()) {
            loadMoreView.noMore()
        } else if (adapter.getItems().contains(books.first()) && adapter.getItems()
                .contains(books.last())
        ) {
            loadMoreView.noMore()
        } else {
            adapter.addItems(books)
        }
    }

    override fun isInBookshelf(name: String, author: String): Boolean {
        return viewModel.isInBookShelf(name, author)
    }

    override fun showBookInfo(book: Book) {
        startActivity<BookInfoActivity> {
            putExtra("name", book.name)
            putExtra("author", book.author)
            putExtra("bookUrl", book.bookUrl)
        }
    }
}
