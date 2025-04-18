package ltd.finelink.read.ui.rss.subscription

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseActivity
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.RuleSub
import ltd.finelink.read.databinding.ActivityRuleSubBinding
import ltd.finelink.read.databinding.DialogRuleSubEditBinding
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.ui.association.ImportBookSourceDialog
import ltd.finelink.read.ui.association.ImportReplaceRuleDialog
import ltd.finelink.read.ui.association.ImportRssSourceDialog
import ltd.finelink.read.ui.widget.recycler.ItemTouchCallback
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 规则订阅界面
 */
class RuleSubActivity : BaseActivity<ActivityRuleSubBinding>(),
    RuleSubAdapter.Callback {

    override val binding by viewBinding(ActivityRuleSubBinding::inflate)
    private val adapter by lazy { RuleSubAdapter(this, this) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.source_subscription, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> {
                val order = appDb.ruleSubDao.maxOrder + 1
                editSubscription(RuleSub(customOrder = order))
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        binding.recyclerView.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun initData() {
        lifecycleScope.launch {
            appDb.ruleSubDao.flowAll().catch {
                AppLog.put("规则订阅界面获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).conflate().collect {
                binding.tvEmptyMsg.isGone = it.isNotEmpty()
                adapter.setItems(it)
            }
        }
    }

    override fun openSubscription(ruleSub: RuleSub) {
        when (ruleSub.type) {
            0 -> showDialogFragment(
                ImportBookSourceDialog(ruleSub.url)
            )
            1 -> showDialogFragment(
                ImportRssSourceDialog(ruleSub.url)
            )
            2 -> showDialogFragment(
                ImportReplaceRuleDialog(ruleSub.url)
            )
        }
    }

    override fun editSubscription(ruleSub: RuleSub) {
        alert(R.string.rule_subscription) {
            val alertBinding = DialogRuleSubEditBinding.inflate(layoutInflater).apply {
                spType.setSelection(ruleSub.type)
                etName.setText(ruleSub.name)
                etUrl.setText(ruleSub.url)
            }
            customView { alertBinding.root }
            okButton {
                lifecycleScope.launch {
                    ruleSub.type = alertBinding.spType.selectedItemPosition
                    ruleSub.name = alertBinding.etName.text?.toString() ?: ""
                    ruleSub.url = alertBinding.etUrl.text?.toString() ?: ""
                    val rs = withContext(IO) {
                        appDb.ruleSubDao.findByUrl(ruleSub.url)
                    }
                    if (rs != null && rs.id != ruleSub.id) {
                        toastOnUi("${getString(R.string.url_already)}(${rs.name})")
                        return@launch
                    }
                    withContext(IO) {
                        appDb.ruleSubDao.insert(ruleSub)
                    }
                }
            }
            cancelButton()
        }
    }

    override fun delSubscription(ruleSub: RuleSub) {
        lifecycleScope.launch(IO) {
            appDb.ruleSubDao.delete(ruleSub)
        }
    }

    override fun updateSourceSub(vararg ruleSub: RuleSub) {
        lifecycleScope.launch(IO) {
            appDb.ruleSubDao.update(*ruleSub)
        }
    }

    override fun upOrder() {
        lifecycleScope.launch(IO) {
            val sourceSubs = appDb.ruleSubDao.all
            for ((index: Int, ruleSub: RuleSub) in sourceSubs.withIndex()) {
                ruleSub.customOrder = index + 1
            }
            appDb.ruleSubDao.update(*sourceSubs.toTypedArray())
        }
    }

}