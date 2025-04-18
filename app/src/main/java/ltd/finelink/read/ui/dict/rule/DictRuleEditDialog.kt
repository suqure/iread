package ltd.finelink.read.ui.dict.rule

import android.app.Application
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar

import androidx.fragment.app.viewModels
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseDialogFragment
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.DictRule
import ltd.finelink.read.databinding.DialogDictRuleEditBinding
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.utils.*
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import ltd.finelink.read.utils.GSON
import ltd.finelink.read.utils.fromJsonObject
import ltd.finelink.read.utils.getClipText
import ltd.finelink.read.utils.sendToClip
import ltd.finelink.read.utils.setLayout
import ltd.finelink.read.utils.toastOnUi

class DictRuleEditDialog() : BaseDialogFragment(R.layout.dialog_dict_rule_edit, true),
    Toolbar.OnMenuItemClickListener {

    val viewModel by viewModels<DictRuleEditViewModel>()
    val binding by viewBinding(DialogDictRuleEditBinding::bind)

    constructor(name: String) : this() {
        arguments = Bundle().apply {
            putString("name", name)
        }
    }

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.inflateMenu(R.menu.dict_rule_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
        viewModel.initData(arguments?.getString("name")) {
            upRuleView(viewModel.dictRule)
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> viewModel.save(getDictRule()) {
                dismissAllowingStateLoss()
            }
            R.id.menu_copy_rule -> viewModel.copyRule(getDictRule())
            R.id.menu_paste_rule -> viewModel.pasteRule {
                upRuleView(it)
            }
        }
        return true
    }

    private fun upRuleView(dictRule: DictRule?) {
        binding.tvRuleName.setText(dictRule?.name)
        binding.tvUrlRule.setText(dictRule?.urlRule)
        binding.tvShowRule.setText(dictRule?.showRule)
    }

    private fun getDictRule(): DictRule {
        val dictRule = viewModel.dictRule?.copy() ?: DictRule()
        dictRule.name = binding.tvRuleName.text.toString()
        dictRule.urlRule = binding.tvUrlRule.text.toString()
        dictRule.showRule = binding.tvShowRule.text.toString()
        return dictRule
    }

    class DictRuleEditViewModel(application: Application) : BaseViewModel(application) {

        var dictRule: DictRule? = null

        fun initData(name: String?, onFinally: () -> Unit) {
            execute {
                if (dictRule == null && name != null) {
                    dictRule = appDb.dictRuleDao.getByName(name)
                }
            }.onFinally {
                onFinally.invoke()
            }
        }

        fun save(newDictRule: DictRule, onFinally: () -> Unit) {
            execute {
                dictRule?.let {
                    appDb.dictRuleDao.delete(it)
                }
                appDb.dictRuleDao.insert(newDictRule)
                dictRule = newDictRule
            }.onFinally {
                onFinally.invoke()
            }
        }

        fun copyRule(dictRule: DictRule) {
            context.sendToClip(GSON.toJson(dictRule))
        }

        fun pasteRule(success: (DictRule) -> Unit) {
            val text = context.getClipText()
            if (text.isNullOrBlank()) {
                context.toastOnUi("剪贴板没有内容")
                return
            }
            execute {
                GSON.fromJsonObject<DictRule>(text).getOrThrow()
            }.onSuccess {
                success.invoke(it)
            }.onError {
                context.toastOnUi("格式不对")
            }
        }

    }

}