package ltd.finelink.read.ui.store.cache

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
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
import ltd.finelink.read.data.entities.TTSCache
import ltd.finelink.read.databinding.ActivityTtsCacheBinding
import ltd.finelink.read.lib.dialogs.SelectItem
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.lib.theme.primaryTextColor
import ltd.finelink.read.ui.file.HandleFileContract
import ltd.finelink.read.ui.widget.SelectActionBar
import ltd.finelink.read.ui.widget.dialog.WaitDialog
import ltd.finelink.read.ui.widget.recycler.DragSelectTouchHelper
import ltd.finelink.read.ui.widget.recycler.ItemTouchCallback
import ltd.finelink.read.ui.widget.recycler.VerticalDivider
import ltd.finelink.read.utils.ACache
import ltd.finelink.read.utils.applyTint
import ltd.finelink.read.utils.hideSoftInput
import ltd.finelink.read.utils.setEdgeEffectColor
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import java.io.File

/**
 * 朗读缓存管理
 */
class TTSCacheActivity : VMBaseActivity<ActivityTtsCacheBinding, TTSCacheViewModel>(),
    PopupMenu.OnMenuItemClickListener,
    SelectActionBar.CallBack,
    TTSCacheAdapter.CallBack {

    override val binding by viewBinding(ActivityTtsCacheBinding::inflate)
    override val viewModel by viewModels<TTSCacheViewModel>()
    private val adapter by lazy { TTSCacheAdapter(this, this) }
    private var sourceFlowJob: Job? = null
    private val exportPath = "exportPath"
    private var merge:Boolean=false

    private val waitDialog by lazy { WaitDialog(this) }

    private val saveWav = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            ACache.get().put(exportPath, uri.toString())
            waitDialog.show()
            if(merge){
                viewModel.export(uri.toString(), adapter.selection,
                    {fileName->
                        toastOnUi(getString(R.string.export_success_file, fileName))
                    },{ waitDialog.dismiss()})
            }else{
                viewModel.exportPer(uri.toString(), adapter.selection,{ toastOnUi(getString(R.string.export_success))},{waitDialog.dismiss()})
            }
        }
    }

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initSearchView()
        upSourceFlow()
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
        menuInflater.inflate(R.menu.tts_cache, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_clear_cache -> delAllDialog()
            R.id.menu_export_cache -> exportCache()
            R.id.menu_export_cache_merge -> exportCache(true)
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
            it.queryHint = getString(R.string.search_cache_key)
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

    private fun exportCache(merge:Boolean = false) {

        if (adapter.selection.isNotEmpty()) {
            val path = ACache.get().getAsString(exportPath)
            if (path.isNullOrEmpty()) {
                exportSelect(merge)
            } else {
                waitDialog.show()
                if(merge){
                    viewModel.export(path, adapter.selection,{
                        toastOnUi(getString(R.string.export_success_file, it))
                    },{waitDialog.dismiss()})
                }else{
                    viewModel.exportPer(path, adapter.selection,{toastOnUi(getString(R.string.export_success))},{waitDialog.dismiss()})
                }

            }
        } else {
            toastOnUi(R.string.record_select)
        }
    }

    private fun exportSelect(merge: Boolean) {
        this.merge = merge
        val default = arrayListOf<SelectItem<Int>>()
        val path = ACache.get().getAsString(exportPath)
        if (!path.isNullOrEmpty()) {
            default.add(SelectItem(path, -1))
        }
        saveWav.launch {
            otherActions = default
        }

    }


    private fun upSourceFlow(searchKey: String? = null) {
        sourceFlowJob?.cancel()
        sourceFlowJob = lifecycleScope.launch {
            when {
                searchKey.isNullOrBlank() -> {
                    appDb.ttsCacheDao.flowAll()
                }

                else -> {
                    appDb.ttsCacheDao.flowSearch(searchKey)
                }
            }.catch {
                AppLog.put("朗读缓存源管理界面更新数据出错", it)
            }.flowOn(IO).conflate().collect {
                adapter.setItems(it, adapter.diffItemCallback)
                delay(100)
            }
        }
    }


    override fun upCountView() {
        binding.selectActionBar.upCountView(
            adapter.selection.size,
            adapter.itemCount
        )
    }


    override fun del(source: TTSCache) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n" + source.bookName + "\n" + source.chapterTitle + "\n" + source.text)
            noButton()
            yesButton {
                viewModel.del(source)
            }
        }
    }

    override fun edit(source: TTSCache) {
        showDialogFragment(TTSCacheEditDialog(source.id))
    }

    override fun update(vararg source: TTSCache) {
        viewModel.update(*source)
    }

    override fun play(source: TTSCache) {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        var file = File(source.file)
        if (file.exists()) {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
            exoPlayer.addMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else {
            toastOnUi(R.string.error_read_file)
        }

    }


}