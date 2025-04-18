package ltd.finelink.read.ui.book.manage

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseDialogFragment
import ltd.finelink.read.base.adapter.ItemViewHolder
import ltd.finelink.read.base.adapter.RecyclerAdapter
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.BookSource
import ltd.finelink.read.data.entities.BookSourcePart
import ltd.finelink.read.databinding.DialogSourcePickerBinding
import ltd.finelink.read.databinding.Item1lineTextBinding
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.lib.theme.primaryTextColor
import ltd.finelink.read.ui.widget.number.NumberPickerDialog
import ltd.finelink.read.utils.applyTint
import ltd.finelink.read.utils.dpToPx
import ltd.finelink.read.utils.setLayout
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import splitties.views.onClick

/**
 * 书源选择
 */
class SourcePickerDialog : BaseDialogFragment(R.layout.dialog_source_picker),
    Toolbar.OnMenuItemClickListener {

    private val binding by viewBinding(DialogSourcePickerBinding::bind)
    private val searchView: SearchView by lazy {
        binding.toolBar.findViewById(R.id.search_view)
    }
    private val toolBar: Toolbar by lazy {
        binding.toolBar.toolbar
    }
    private val adapter by lazy {
        SourceAdapter(requireContext())
    }
    private var sourceFlowJob: Job? = null

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initData()
        initMenu()
    }

    private fun initView() {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.title = "选择书源"
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        searchView.applyTint(primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.search_book_source)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                initData(newText)
                return false
            }
        })
    }

    private fun initData(searchKey: String? = null) {
        sourceFlowJob?.cancel()
        sourceFlowJob = lifecycleScope.launch {
            when {
                searchKey.isNullOrEmpty() -> appDb.bookSourceDao.flowEnabled()
                else -> appDb.bookSourceDao.flowSearchEnabled(searchKey)
            }.catch {
                AppLog.put("书源选择界面获取书源数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).collect {
                adapter.setItems(it)
            }
        }
    }

    private fun initMenu() {
        toolBar.setOnMenuItemClickListener(this)
        toolBar.inflateMenu(R.menu.source_picker)
        toolBar.menu.applyTint(requireContext())
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_change_source_delay -> NumberPickerDialog(requireContext())
                .setTitle(getString(R.string.change_source_delay))
                .setMaxValue(9999)
                .setMinValue(0)
                .setValue(AppConfig.batchChangeSourceDelay)
                .show {
                    AppConfig.batchChangeSourceDelay = it
                }
        }
        return true
    }

    inner class SourceAdapter(context: Context) :
        RecyclerAdapter<BookSourcePart, Item1lineTextBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): Item1lineTextBinding {
            return Item1lineTextBinding.inflate(inflater, parent, false).apply {
                root.setPadding(16.dpToPx())
            }
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: Item1lineTextBinding,
            item: BookSourcePart,
            payloads: MutableList<Any>
        ) {
            binding.textView.text = item.getDisPlayNameGroup()
        }

        override fun registerListener(holder: ItemViewHolder, binding: Item1lineTextBinding) {
            binding.root.onClick {
                getItemByLayoutPosition(holder.layoutPosition)?.let {
                    it.getBookSource()?.let { source ->
                        callback?.sourceOnClick(source)
                    }
                    dismissAllowingStateLoss()
                }
            }
        }

    }

    private val callback: Callback?
        get() {
            return (parentFragment as? Callback) ?: activity as? Callback
        }

    interface Callback {
        fun sourceOnClick(source: BookSource)
    }

}