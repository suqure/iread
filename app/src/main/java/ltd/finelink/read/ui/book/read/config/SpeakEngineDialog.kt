package ltd.finelink.read.ui.book.read.config

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseDialogFragment
import ltd.finelink.read.base.adapter.ItemViewHolder
import ltd.finelink.read.base.adapter.RecyclerAdapter
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.LocalTTS
import ltd.finelink.read.databinding.DialogRecyclerViewBinding
import ltd.finelink.read.databinding.ItemLocalTtsBinding
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.lib.dialogs.SelectItem
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.model.ReadAloud
import ltd.finelink.read.model.ReadBook
import ltd.finelink.read.utils.GSON
import ltd.finelink.read.utils.applyTint
import ltd.finelink.read.utils.fromJsonObject
import ltd.finelink.read.utils.gone
import ltd.finelink.read.utils.isJsonObject
import ltd.finelink.read.utils.setEdgeEffectColor
import ltd.finelink.read.utils.setLayout
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import ltd.finelink.read.utils.visible

/**
 * tts引擎管理
 */
class SpeakEngineDialog(val callBack: CallBack) : BaseDialogFragment(R.layout.dialog_recycler_view),
    Toolbar.OnMenuItemClickListener {

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val viewModel: SpeakEngineViewModel by viewModels()
    private val adapter by lazy { Adapter(requireContext()) }
    private var ttsEngine: String? = ReadAloud.ttsEngine
    private val sysTtsViews = arrayListOf<RadioButton>()


    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 0.9f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initMenu()
        initData()
    }

    private fun initView() = binding.run {
        toolBar.setBackgroundColor(primaryColor)
        toolBar.setTitle(R.string.speak_engine)
        recyclerView.setEdgeEffectColor(primaryColor)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        adapter.addHeaderView {
            ItemLocalTtsBinding.inflate(layoutInflater, recyclerView, false).apply {
                sysTtsViews.add(cbName)
                ivEdit.gone()
                labelSys.visible()
                cbName.text = getString(R.string.system_tts)
                cbName.tag = ""
                cbName.isChecked = ttsEngine == null || ttsEngine!!.isJsonObject()
                        && GSON.fromJsonObject<SelectItem<String>>(ttsEngine)
                    .getOrNull()?.value.isNullOrEmpty()
                cbName.setOnClickListener {
                    upTts(GSON.toJson(SelectItem("系统默认", "")))
                }
            }
        }
        viewModel.sysEngines.forEach { engine ->
            adapter.addHeaderView {
                ItemLocalTtsBinding.inflate(layoutInflater, recyclerView, false).apply {
                    sysTtsViews.add(cbName)
                    ivEdit.gone()
                    labelSys.visible()
                    cbName.text = engine.label
                    cbName.tag = engine.name
                    cbName.isChecked = GSON.fromJsonObject<SelectItem<String>>(ttsEngine)
                        .getOrNull()?.value == cbName.tag
                    cbName.setOnClickListener {
                        upTts(GSON.toJson(SelectItem(engine.label, engine.name)))
                    }
                }
            }
        }
        tvFooterLeft.setText(R.string.book)
        tvFooterLeft.visible()
        tvFooterLeft.setOnClickListener {
            ReadBook.book?.setTtsEngine(ttsEngine)
            callBack.upSpeakEngineSummary()
            ReadAloud.upReadAloudClass()
            dismissAllowingStateLoss()
        }
        tvOk.setText(R.string.general)
        tvOk.visible()
        tvOk.setOnClickListener {
            ReadBook.book?.setTtsEngine(null)
            AppConfig.ttsEngine = ttsEngine
            callBack.upSpeakEngineSummary()
            ReadAloud.upReadAloudClass()
            dismissAllowingStateLoss()
        }
        tvCancel.visible()
        tvCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun initMenu() = binding.run {
        toolBar.inflateMenu(R.menu.speak_engine)
        toolBar.menu.applyTint(requireContext())
        toolBar.setOnMenuItemClickListener(this@SpeakEngineDialog)
    }

    private fun initData() {
        lifecycleScope.launch {
            appDb.localTTSDao.flowByDownload(true).catch {
                AppLog.put("朗读引擎界面获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).conflate().collect {
                adapter.setItems(it)
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {

        return true
    }



    private fun upTts(tts: String) {
        ttsEngine = tts
        sysTtsViews.forEach {
            it.isChecked = GSON.fromJsonObject<SelectItem<String>>(ttsEngine)
                .getOrNull()?.value == it.tag
        }
        adapter.notifyItemRangeChanged(adapter.getHeaderCount(), adapter.itemCount)
    }

    inner class Adapter(context: Context) :
        RecyclerAdapter<LocalTTS, ItemLocalTtsBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemLocalTtsBinding {
            return ItemLocalTtsBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemLocalTtsBinding,
            item: LocalTTS,
            payloads: MutableList<Any>
        ) {
            binding.apply {
                cbName.text = item.name
                cbName.isChecked = item.id.toString() == ttsEngine
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemLocalTtsBinding) {
            binding.run {
                cbName.setOnClickListener {
                    getItemByLayoutPosition(holder.layoutPosition)?.let { localTTS ->
                        val id = localTTS.id.toString()
                        upTts(id)
                    }
                }
                ivEdit.setOnClickListener {
                    val id = getItemByLayoutPosition(holder.layoutPosition)!!.id
                    showDialogFragment(LocalTtsEditDialog(id))
                }
            }
        }

    }

    interface CallBack {
        fun upSpeakEngineSummary()
    }

}