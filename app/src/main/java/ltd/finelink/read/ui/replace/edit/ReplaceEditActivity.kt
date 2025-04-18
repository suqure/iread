package ltd.finelink.read.ui.replace.edit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseActivity
import ltd.finelink.read.data.entities.ReplaceRule
import ltd.finelink.read.databinding.ActivityReplaceEditBinding
import ltd.finelink.read.lib.dialogs.SelectItem
import ltd.finelink.read.ui.widget.keyboard.KeyboardToolPop
import ltd.finelink.read.utils.GSON
import ltd.finelink.read.utils.sendToClip
import ltd.finelink.read.utils.showHelp
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding

/**
 * 编辑替换规则
 */
class ReplaceEditActivity :
    VMBaseActivity<ActivityReplaceEditBinding, ReplaceEditViewModel>(false),
    KeyboardToolPop.CallBack {

    companion object {

        fun startIntent(
            context: Context,
            id: Long = -1,
            pattern: String? = null,
            isRegex: Boolean = false,
            scope: String? = null
        ): Intent {
            val intent = Intent(context, ReplaceEditActivity::class.java)
            intent.putExtra("id", id)
            intent.putExtra("pattern", pattern)
            intent.putExtra("isRegex", isRegex)
            intent.putExtra("scope", scope)
            return intent
        }

    }

    override val binding by viewBinding(ActivityReplaceEditBinding::inflate)
    override val viewModel by viewModels<ReplaceEditViewModel>()

    private val softKeyboardTool by lazy {
        KeyboardToolPop(this, lifecycleScope, binding.root, this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        softKeyboardTool.attachToWindow(window)
        viewModel.initData(intent) {
            upReplaceView(it)
        }
        binding.ivHelp.setOnClickListener {
            showHelp("regexHelp")
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.replace_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> viewModel.save(getReplaceRule()) {
                setResult(RESULT_OK)
                finish()
            }
            R.id.menu_copy_rule -> sendToClip(GSON.toJson(getReplaceRule()))
            R.id.menu_paste_rule -> viewModel.pasteRule {
                upReplaceView(it)
            }
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        softKeyboardTool.dismiss()
    }

    private fun upReplaceView(replaceRule: ReplaceRule) = binding.run {
        etName.setText(replaceRule.name)
        etGroup.setText(replaceRule.group)
        etReplaceRule.setText(replaceRule.pattern)
        cbUseRegex.isChecked = replaceRule.isRegex
        etReplaceTo.setText(replaceRule.replacement)
        cbScopeTitle.isChecked = replaceRule.scopeTitle
        cbScopeContent.isChecked = replaceRule.scopeContent
        etScope.setText(replaceRule.scope)
        etExcludeScope.setText(replaceRule.excludeScope)
        etTimeout.setText(replaceRule.timeoutMillisecond.toString())
    }

    private fun getReplaceRule(): ReplaceRule = binding.run {
        val replaceRule: ReplaceRule = viewModel.replaceRule ?: ReplaceRule()
        replaceRule.name = etName.text.toString()
        replaceRule.group = etGroup.text.toString()
        replaceRule.pattern = etReplaceRule.text.toString()
        replaceRule.isRegex = cbUseRegex.isChecked
        replaceRule.replacement = etReplaceTo.text.toString()
        replaceRule.scopeTitle = cbScopeTitle.isChecked
        replaceRule.scopeContent = cbScopeContent.isChecked
        replaceRule.scope = etScope.text.toString()
        replaceRule.excludeScope = etExcludeScope.text.toString()
        replaceRule.timeoutMillisecond = etTimeout.text.toString().ifEmpty { "3000" }.toLong()
        return replaceRule
    }

    override fun helpActions(): List<SelectItem<String>> {
        return arrayListOf(
            SelectItem("正则教程", "regexHelp")
        )
    }

    override fun onHelpActionSelect(action: String) {
        when (action) {
            "regexHelp" -> showHelp("regexHelp")
        }
    }

    override fun sendText(text: String) {
        if (text.isBlank()) return
        val view = window?.decorView?.findFocus()
        if (view is EditText) {
            val start = view.selectionStart
            val end = view.selectionEnd
            //获取EditText的文字
            val edit = view.editableText
            if (start < 0 || start >= edit.length) {
                edit.append(text)
            } else {
                //光标所在位置插入文字
                edit.replace(start, end, text)
            }
        }
    }

}
