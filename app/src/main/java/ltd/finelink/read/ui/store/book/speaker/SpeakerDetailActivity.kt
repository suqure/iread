package ltd.finelink.read.ui.store.book.speaker

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseActivity
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.BookSpeakerDetail
import ltd.finelink.read.databinding.ActivitySpeakerDetailBinding
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.lib.theme.primaryTextColor
import ltd.finelink.read.ui.widget.SelectActionBar
import ltd.finelink.read.ui.widget.recycler.DragSelectTouchHelper
import ltd.finelink.read.ui.widget.recycler.ItemTouchCallback
import ltd.finelink.read.ui.widget.recycler.VerticalDivider
import ltd.finelink.read.utils.applyTint
import ltd.finelink.read.utils.hideSoftInput
import ltd.finelink.read.utils.setEdgeEffectColor
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding

/**
 * 朗读对话管理
 */
class SpeakerDetailActivity :
    VMBaseActivity<ActivitySpeakerDetailBinding, SpeakerDetailViewModel>(),
    PopupMenu.OnMenuItemClickListener,
    SelectActionBar.CallBack,
    SpeakerDetailAdapter.CallBack {

    override val binding by viewBinding(ActivitySpeakerDetailBinding::inflate)
    override val viewModel by viewModels<SpeakerDetailViewModel>()
    private val adapter by lazy { SpeakerDetailAdapter(this, this) }
    private var sourceFlowJob: Job? = null


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initSearchView()
        viewModel.bookData.observe(this) {
            upSourceFlow()
        }
        viewModel.initData(intent)
        initSelectActionBar()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            currentFocus?.let {
                if (it is EditText) {
                    it.clearFocus()
                    it.hideSoftInput()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_speaker, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_clear_cache -> delAllDialog()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return true
    }

    private fun initRecyclerView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        binding.recyclerView.adapter = adapter
        // When this page is opened, it is in selection mode
        val dragSelectTouchHelper: DragSelectTouchHelper =
            DragSelectTouchHelper(adapter.dragSelectCallback).setSlideArea(16, 50)
        dragSelectTouchHelper.attachToRecyclerView(binding.recyclerView)
        dragSelectTouchHelper.activeSlideSelect()
        // Note: need judge selection first, so add ItemTouchHelper after it.
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun initSearchView() {
        binding.titleBar.findViewById<SearchView>(R.id.search_view).let {
            it.applyTint(primaryTextColor)
            it.onActionViewExpanded()
            it.queryHint = getString(R.string.search_speaker_key)
            it.clearFocus()
            it.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    upSourceFlow(newText)
                    return false
                }
            })
        }
    }

    private fun initSelectActionBar() {
        binding.selectActionBar.setMainActionText(R.string.delete)
        binding.selectActionBar.setOnMenuItemClickListener(this)
        binding.selectActionBar.setCallBack(this)
    }


    override fun selectAll(selectAll: Boolean) {
        if (selectAll) {
            adapter.selectAll()
        } else {
            adapter.revertSelection()
        }
    }

    override fun revertSelection() {
        adapter.revertSelection()
    }

    override fun onClickSelectBarMainAction() {
        delSourceDialog()
    }

    private fun delSourceDialog() {
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            yesButton { viewModel.del(*adapter.selection.toTypedArray()) }
            noButton()
        }
    }

    private fun delAllDialog() {
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del_all) {
            yesButton { viewModel.clearAll() }
            noButton()
        }
    }


    private fun upSourceFlow(searchKey: String? = null) {
        sourceFlowJob?.cancel()
        sourceFlowJob = lifecycleScope.launch {
            viewModel.bookData.value?.let { book ->
                when {
                    searchKey.isNullOrBlank() -> {
                        appDb.bookSpeakerDetailDao.flowByBook(book.bookUrl)
                    }
                    else -> {
                        appDb.bookSpeakerDetailDao.flowSearch(book.bookUrl, searchKey)
                    }
                }.catch {
                    AppLog.put("朗读对话管理界面更新数据出错", it)
                }.flowOn(IO).conflate().collect {
                    adapter.setItems(it, adapter.diffItemCallback)
                    delay(100)
                }
            }
        }
    }


    override fun upCountView() {
        binding.selectActionBar.upCountView(
            adapter.selection.size,
            adapter.itemCount
        )
    }


    override fun del(source: BookSpeakerDetail) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n" + source.spkName + "\n" + source.text)
            noButton()
            yesButton {
                viewModel.del(source)
            }
        }
    }

    override fun edit(source: BookSpeakerDetail) {
        showDialogFragment(SpeakerDetailEditDialog(source.id))
    }

}