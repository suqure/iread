package ltd.finelink.read.ui.book.toc.rule

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
import ltd.finelink.read.data.entities.TxtTocRule
import ltd.finelink.read.databinding.DialogTocRegexEditBinding
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.utils.*
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import ltd.finelink.read.utils.GSON
import ltd.finelink.read.utils.fromJsonObject
import ltd.finelink.read.utils.getClipText
import ltd.finelink.read.utils.printOnDebug
import ltd.finelink.read.utils.sendToClip
import ltd.finelink.read.utils.setLayout
import ltd.finelink.read.utils.toastOnUi

class TxtTocRuleEditDialog() : BaseDialogFragment(R.layout.dialog_toc_regex_edit, true),
    Toolbar.OnMenuItemClickListener {

    constructor(id: Long?) : this() {
        id ?: return
        arguments = Bundle().apply {
            putLong("id", id)
        }
    }

    private val binding by viewBinding(DialogTocRegexEditBinding::bind)
    private val viewModel by viewModels<ViewModel>()
    private val callback get() = (parentFragment as? Callback) ?: activity as? Callback

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        initMenu()
        viewModel.initData(arguments?.getLong("id")) {
            upRuleView(it)
        }
    }

    private fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.txt_toc_rule_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> {
                callback?.saveTxtTocRule(getRuleFromView())
                dismissAllowingStateLoss()
            }
            R.id.menu_copy_rule -> context?.sendToClip(GSON.toJson(getRuleFromView()))
            R.id.menu_paste_rule -> viewModel.pasteRule {
                upRuleView(it)
            }
        }
        return true
    }

    private fun upRuleView(tocRule: TxtTocRule?) {
        binding.tvRuleName.setText(tocRule?.name)
        binding.tvRuleRegex.setText(tocRule?.rule)
        binding.tvRuleExample.setText(tocRule?.example)
    }

    private fun getRuleFromView(): TxtTocRule {
        val tocRule = viewModel.tocRule ?: TxtTocRule().apply {
            viewModel.tocRule = this
        }
        binding.run {
            tocRule.name = tvRuleName.text.toString()
            tocRule.rule = tvRuleRegex.text.toString()
            tocRule.example = tvRuleExample.text.toString()
        }
        return tocRule
    }

    class ViewModel(application: Application) : BaseViewModel(application) {

        var tocRule: TxtTocRule? = null

        fun initData(id: Long?, finally: (tocRule: TxtTocRule?) -> Unit) {
            if (tocRule != null) return
            execute {
                if (id == null) return@execute
                tocRule = appDb.txtTocRuleDao.get(id)
            }.onFinally {
                finally.invoke(tocRule)
            }
        }

        fun pasteRule(success: (TxtTocRule) -> Unit) {
            execute(context = Dispatchers.Main) {
                val text = context.getClipText()
                if (text.isNullOrBlank()) {
                    throw NoStackTraceException("剪贴板为空")
                }
                GSON.fromJsonObject<TxtTocRule>(text).getOrNull()
                    ?: throw NoStackTraceException("格式不对")
            }.onSuccess {
                success.invoke(it)
            }.onError {
                context.toastOnUi(it.localizedMessage ?: "Error")
                it.printOnDebug()
            }
        }

    }

    interface Callback {

        fun saveTxtTocRule(txtTocRule: TxtTocRule)

    }

}