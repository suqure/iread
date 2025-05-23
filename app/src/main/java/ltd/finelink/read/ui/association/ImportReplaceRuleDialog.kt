package ltd.finelink.read.ui.association

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseDialogFragment
import ltd.finelink.read.base.adapter.ItemViewHolder
import ltd.finelink.read.base.adapter.RecyclerAdapter
import ltd.finelink.read.constant.PreferKey
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.ReplaceRule
import ltd.finelink.read.databinding.DialogCustomGroupBinding
import ltd.finelink.read.databinding.DialogRecyclerViewBinding
import ltd.finelink.read.databinding.ItemSourceImportBinding
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.ui.widget.dialog.CodeDialog
import ltd.finelink.read.ui.widget.dialog.WaitDialog
import ltd.finelink.read.utils.*
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import ltd.finelink.read.utils.GSON
import ltd.finelink.read.utils.dpToPx
import ltd.finelink.read.utils.fromJsonObject
import ltd.finelink.read.utils.putPrefBoolean
import ltd.finelink.read.utils.setLayout
import ltd.finelink.read.utils.showDialogFragment
import splitties.views.onClick

class ImportReplaceRuleDialog() : BaseDialogFragment(R.layout.dialog_recycler_view),
    Toolbar.OnMenuItemClickListener,
    CodeDialog.Callback {

    constructor(source: String, finishOnDismiss: Boolean = false) : this() {
        arguments = Bundle().apply {
            putString("source", source)
            putBoolean("finishOnDismiss", finishOnDismiss)
        }
    }

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val viewModel by viewModels<ImportReplaceRuleViewModel>()
    private val adapter by lazy { SourcesAdapter(requireContext()) }

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (arguments?.getBoolean("finishOnDismiss") == true) {
            activity?.finish()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.setTitle(R.string.import_replace_rule)
        binding.rotateLoading.visible()
        initMenu()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.tvCancel.visible()
        binding.tvCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.tvOk.visible()
        binding.tvOk.setOnClickListener {
            val waitDialog = WaitDialog(requireContext())
            waitDialog.show()
            viewModel.importSelect {
                waitDialog.dismiss()
                dismissAllowingStateLoss()
            }
        }
        binding.tvFooterLeft.visible()
        binding.tvFooterLeft.setOnClickListener {
            val selectAll = viewModel.isSelectAll
            viewModel.selectStatus.forEachIndexed { index, b ->
                if (b != !selectAll) {
                    viewModel.selectStatus[index] = !selectAll
                }
            }
            adapter.notifyDataSetChanged()
            upSelectText()
        }
        viewModel.errorLiveData.observe(this) {
            binding.rotateLoading.gone()
            binding.tvMsg.apply {
                text = it
                visible()
            }
        }
        viewModel.successLiveData.observe(this) {
            binding.rotateLoading.gone()
            if (it > 0) {
                adapter.setItems(viewModel.allRules)
                upSelectText()
            } else {
                binding.tvMsg.apply {
                    setText(R.string.wrong_format)
                    visible()
                }
            }
        }
        val source = arguments?.getString("source")
        if (source.isNullOrEmpty()) {
            dismiss()
            return
        }
        viewModel.import(source)
    }

    private fun initMenu() {
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.toolBar.inflateMenu(R.menu.import_replace)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_new_group -> alertCustomGroup(item)
            R.id.menu_keep_original_name -> {
                item.isChecked = !item.isChecked
                putPrefBoolean(PreferKey.importKeepName, item.isChecked)
            }
        }
        return true
    }

    private fun alertCustomGroup(item: MenuItem) {
        alert(R.string.diy_edit_source_group) {
            val alertBinding = DialogCustomGroupBinding.inflate(layoutInflater).apply {
                val groups = appDb.replaceRuleDao.allGroups()
                textInputLayout.setHint(R.string.group_name)
                editView.setFilterValues(groups.toList())
                editView.dropDownHeight = 180.dpToPx()
            }
            customView {
                alertBinding.root
            }
            okButton {
                viewModel.isAddGroup = alertBinding.swAddGroup.isChecked
                viewModel.groupName = alertBinding.editView.text?.toString()
                if (viewModel.groupName.isNullOrBlank()) {
                    item.title = getString(R.string.diy_source_group)
                } else {
                    val group = getString(R.string.diy_edit_source_group_title, viewModel.groupName)
                    if (viewModel.isAddGroup) {
                        item.title = "+$group"
                    } else {
                        item.title = group
                    }
                }
            }
            noButton()
        }
    }

    private fun upSelectText() {
        if (viewModel.isSelectAll) {
            binding.tvFooterLeft.text = getString(
                R.string.select_cancel_count,
                viewModel.selectCount,
                viewModel.allRules.size
            )
        } else {
            binding.tvFooterLeft.text = getString(
                R.string.select_all_count,
                viewModel.selectCount,
                viewModel.allRules.size
            )
        }
    }

    override fun onCodeSave(code: String, requestId: String?) {
        requestId?.toInt()?.let {
            GSON.fromJsonObject<ReplaceRule>(code).getOrNull()?.let { rule ->
                viewModel.allRules[it] = rule
                adapter.setItem(it, rule)
            }
        }
    }

    inner class SourcesAdapter(context: Context) :
        RecyclerAdapter<ReplaceRule, ItemSourceImportBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemSourceImportBinding {
            return ItemSourceImportBinding.inflate(inflater, parent, false)
        }

        @SuppressLint("SetTextI18n")
        override fun convert(
            holder: ItemViewHolder,
            binding: ItemSourceImportBinding,
            item: ReplaceRule,
            payloads: MutableList<Any>
        ) {
            binding.run {
                cbSourceName.isChecked = viewModel.selectStatus[holder.layoutPosition]
                cbSourceName.text = if (item.group.isNullOrBlank()) {
                    item.name
                } else {
                    "${item.name}(${item.group})"
                }
                val localRule = viewModel.checkRules[holder.layoutPosition]
                tvSourceState.text = when {
                    localRule == null -> "新增"
                    item.pattern != localRule.pattern
                            || item.replacement != localRule.replacement
                            || item.isRegex != localRule.isRegex
                            || item.scope != localRule.scope -> "更新"
                    else -> "已有"
                }
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemSourceImportBinding) {
            binding.run {
                cbSourceName.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) {
                        viewModel.selectStatus[holder.layoutPosition] = isChecked
                        upSelectText()
                    }
                }
                root.onClick {
                    cbSourceName.isChecked = !cbSourceName.isChecked
                    viewModel.selectStatus[holder.layoutPosition] = cbSourceName.isChecked
                    upSelectText()
                }
                tvOpen.setOnClickListener {
                    val source = viewModel.allRules[holder.layoutPosition]
                    showDialogFragment(
                        CodeDialog(
                            GSON.toJson(source),
                            disableEdit = false,
                            requestId = holder.layoutPosition.toString()
                        )
                    )
                }
            }
        }

    }

}