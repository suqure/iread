package ltd.finelink.read.ui.main.store.model

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseFragment
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.constant.IntentAction
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.LocalTTS
import ltd.finelink.read.databinding.FragmentModelsBinding
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.model.ReadAloud
import ltd.finelink.read.model.ReadBook
import ltd.finelink.read.ui.book.read.config.LocalTtsEditDialog
import ltd.finelink.read.ui.store.model.ModelInfoActivity
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.externalFiles
import ltd.finelink.read.utils.observeEvent
import ltd.finelink.read.utils.setEdgeEffectColor
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.startActivity
import ltd.finelink.read.utils.startService
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import splitties.init.appCtx
import java.io.File
import kotlin.math.max


/**
 *  模型界面
 */
class ModelsFragment() : BaseFragment(R.layout.fragment_models),
    BaseModelAdapter.CallBack {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    private val binding by viewBinding(FragmentModelsBinding::bind)
    private val modelsAdapter: BaseModelAdapter<*> by lazy {
        ModelAdapterList(requireContext(), this, viewLifecycleOwner.lifecycle)

    }

    private val modelsDir: File by lazy {
        FileUtils.createFolderIfNotExist(appCtx.externalFiles.absolutePath + File.separator + "model" + File.separator)
    }
    private var modelFlowJob: Job? = null
    private var savedInstanceState: Bundle? = null
    var position = 0
        private set

    private var upLastUpdateTimeJob: Job? = null
    private var defaultScrollBarSize = 0
    private var enableRefresh = true

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        this.savedInstanceState = savedInstanceState
        arguments?.let {
            position = it.getInt("position", 0)
            enableRefresh = it.getBoolean("enableRefresh", true)
            binding.refreshLayout.isEnabled = enableRefresh
        }
        initRecyclerView()
        upRecyclerData()
    }

    private fun initRecyclerView() {
        binding.rvBookshelf.setEdgeEffectColor(primaryColor)
        defaultScrollBarSize = binding.rvBookshelf.scrollBarSize
        upFastScrollerBar()
        binding.refreshLayout.setColorSchemeColors(accentColor)
        binding.refreshLayout.setOnRefreshListener {
            binding.refreshLayout.isRefreshing = false
        }
        binding.rvBookshelf.layoutManager = LinearLayoutManager(context)
        binding.rvBookshelf.adapter = modelsAdapter
        modelsAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
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


    /**
     * 更新书籍列表信息
     */
    private fun upRecyclerData() {
        modelFlowJob?.cancel()
        modelFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            appDb.localTTSDao.flowAll()
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.RESUMED).catch {
                    AppLog.put("书架更新出错", it)
                }.conflate().flowOn(Dispatchers.Default).collect { list ->
                    binding.tvEmptyMsg.isGone = list.isNotEmpty()
                    binding.refreshLayout.isEnabled = enableRefresh && list.isNotEmpty()
                    modelsAdapter.setItems(list)
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
                    modelsAdapter.upLastUpdateTime()
                    delay(30 * 1000)
                }
            }
        }
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



    override fun openSetting(model: LocalTTS) {
        showDialogFragment(LocalTtsEditDialog(model.id))
    }

    override fun remove(model: LocalTTS,ask:Boolean): Boolean {
        if(ask){
            alert(
                titleResource = R.string.draw,
                messageResource = R.string.sure_del
            ) {
                yesButton{
                    delete(model)
                }
                noButton()
            }
        }else{
            delete(model)
        }
        return true
    }

    private fun delete(model: LocalTTS){
        var dir = File("${modelsDir}/${model.id}")
        if (dir.exists()) {
            for (file in dir.listFiles()) {
                file.delete()
            }
            dir.delete()
        }
        model.download = false
        model.progress = 0
        model.status = 0
        appDb.localTTSDao.update(model)
        modelsAdapter.notifyDataSetChanged()
    }

    override fun openModelInfo(model: LocalTTS) {
        startActivity<ModelInfoActivity> {
            putExtra("id", model.id)
        }
    }




    override fun setEngine(model: LocalTTS) {
        alert(
            titleResource = R.string.draw,
            messageResource = R.string.set_engine_confirm
        ) {
            yesButton {
                ReadBook.book?.setTtsEngine(model.id.toString())
                AppConfig.ttsEngine = model.id.toString()
                ReadAloud.upReadAloudClass()
                toastOnUi(R.string.success)
            }
            noButton()
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<String>(EventBus.UP_STORE) {
            modelsAdapter.notifyDataSetChanged()
        }
        observeEvent<Long>(EventBus.UP_STORE_MODEL) {
            appDb.localTTSDao.get(it)?.let {model->
                modelsAdapter.notifyChange(model)
            }

        }
        observeEvent<LocalTTS>(EventBus.UP_MODEL_DOWNLOAD) {
            modelsAdapter.downloadNotify(it)
        }
    }
}
