package ltd.finelink.read.ui.main.store.voice

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseFragment
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.constant.IntentAction
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.TTSSpeaker
import ltd.finelink.read.databinding.FragmentVoicesBinding
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.ui.store.voice.VoiceInfoActivity
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.externalFiles
import ltd.finelink.read.utils.observeEvent
import ltd.finelink.read.utils.postEvent
import ltd.finelink.read.utils.setEdgeEffectColor
import ltd.finelink.read.utils.startActivity
import ltd.finelink.read.utils.startService
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import splitties.init.appCtx
import java.io.File
import kotlin.math.max

/**
 *  声音界面
 */
class VoicesFragment() : BaseFragment(R.layout.fragment_voices),
    BaseVoiceAdapter.CallBack {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    private val speakerFolderPath: String by lazy {

        appCtx.externalFiles.absolutePath + File.separator + "speaker" + File.separator
    }

    private val binding by viewBinding(FragmentVoicesBinding::bind)
    private val voicesAdapter: BaseVoiceAdapter<*> by lazy {
        VoiceAdapterList(requireContext(), this, viewLifecycleOwner.lifecycle)

    }
    private var voiceFlowJob: Job? = null
    private var savedInstanceState: Bundle? = null
    var position = 0
        private set

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
            voicesAdapter.notifyDataSetChanged()
        }
        binding.rvBookshelf.layoutManager = LinearLayoutManager(context)
        binding.rvBookshelf.adapter = voicesAdapter
        voicesAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
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
        voiceFlowJob?.cancel()
        voiceFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            appDb.ttsSpeakerDao.flowAll()
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.RESUMED).catch {
                    AppLog.put("书架更新出错", it)
                }.conflate().flowOn(Dispatchers.Default).collect { list ->
                    binding.tvEmptyMsg.isGone = list.isNotEmpty()
                    binding.refreshLayout.isEnabled = enableRefresh && list.isNotEmpty()
                    voicesAdapter.setItems(list)
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


    override fun openSetting(speaker: TTSSpeaker) {
        alert(
            titleResource = R.string.draw,
            messageResource = R.string.set_voice_confirm
        ) {
            yesButton {
                appDb.localTTSDao.fineByType(speaker.type).forEach { localTTS ->
                    localTTS.speakerId = speaker.id
                    localTTS.speakerName = speaker.name
                    localTTS.speaker = speaker.speakerFile()
                    appDb.localTTSDao.update(localTTS)
                    postEvent(EventBus.UP_STORE_MODEL, localTTS.id)
                }
                toastOnUi(R.string.success)
            }
            noButton()
        }
    }

    override fun remove(speaker: TTSSpeaker,ask:Boolean): Boolean {
        if(ask){
            alert(
                titleResource = R.string.draw,
                messageResource = R.string.sure_del
            ) {
                yesButton {
                    delete(speaker)
                }
                noButton()
            }
        }else {
            delete(speaker)
        }

        return true
    }

    private fun delete(speaker: TTSSpeaker){
        var speakerDir = FileUtils.createFolderIfNotExist(speakerFolderPath);
        var name = speaker.speakerFile()
        var file = File("${speakerDir}/${name}")
        if (file.exists()) {
            file.delete()
        }
        speaker.download = false
        speaker.progress = 0
        speaker.status = 0
        appDb.ttsSpeakerDao.update(speaker)
        voicesAdapter.notifyDataSetChanged()
    }
    override fun openVoiceInfo(speaker: TTSSpeaker) {
        startActivity<VoiceInfoActivity> {
            putExtra("id", speaker.id)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<String>(EventBus.UP_STORE) {
            voicesAdapter.notifyDataSetChanged()
        }
        observeEvent<String>(EventBus.UP_STORE_VOICE) {
            voicesAdapter.notifyDataSetChanged()
        }
        observeEvent<TTSSpeaker>(EventBus.UP_VOICE_DOWNLOAD) {
            voicesAdapter.downloadNotify(it)
        }
    }
}
