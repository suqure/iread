package ltd.finelink.read.ui.book.bookmark

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseActivity
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Bookmark
import ltd.finelink.read.databinding.ActivityAllBookmarkBinding
import ltd.finelink.read.ui.file.HandleFileContract
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * 所有书签
 */
class AllBookmarkActivity : VMBaseActivity<ActivityAllBookmarkBinding, AllBookmarkViewModel>(),
    BookmarkAdapter.Callback {

    override val viewModel by viewModels<AllBookmarkViewModel>()
    override val binding by viewBinding(ActivityAllBookmarkBinding::inflate)
    private val adapter by lazy {
        BookmarkAdapter(this, this)
    }
    private val exportDir = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            when (it.requestCode) {
                1 -> viewModel.exportBookmark(uri)
                2 -> viewModel.exportBookmarkMd(uri)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        lifecycleScope.launch {
            appDb.bookmarkDao.flowAll().catch {
                AppLog.put("所有书签界面获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).collect {
                adapter.setItems(it)
            }
        }
    }

    private fun initView() {
        binding.recyclerView.addItemDecoration(BookmarkDecoration(adapter))
        binding.recyclerView.adapter = adapter
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.bookmark, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_export -> exportDir.launch {
                requestCode = 1
            }

            R.id.menu_export_md -> exportDir.launch {
                requestCode = 2
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onItemClick(bookmark: Bookmark, position: Int) {
        showDialogFragment(BookmarkDialog(bookmark, position))
    }

}