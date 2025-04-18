package ltd.finelink.read.ui.rss.favorites

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ltd.finelink.read.base.BaseActivity
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.RssStar
import ltd.finelink.read.databinding.ActivityRssFavoritesBinding
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.ui.rss.read.ReadRssActivity
import ltd.finelink.read.ui.widget.recycler.VerticalDivider
import ltd.finelink.read.utils.startActivity
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * 收藏夹
 */
class RssFavoritesActivity : BaseActivity<ActivityRssFavoritesBinding>(),
    RssFavoritesAdapter.CallBack {

    override val binding by viewBinding(ActivityRssFavoritesBinding::inflate)
    private val adapter by lazy { RssFavoritesAdapter(this, this) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initView() {
        binding.refreshLayout.setColorSchemeColors(accentColor)
        binding.recyclerView.let {
            it.layoutManager = LinearLayoutManager(this)
            it.addItemDecoration(VerticalDivider(this))
            it.adapter = adapter
        }
    }

    private fun initData() {
        lifecycleScope.launch {
            appDb.rssStarDao.liveAll().catch {
                AppLog.put("订阅收藏夹界面获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).conflate().collect {
                adapter.setItems(it)
            }
        }
    }

    override fun readRss(rssStar: RssStar) {
        startActivity<ReadRssActivity> {
            putExtra("title", rssStar.title)
            putExtra("origin", rssStar.origin)
            putExtra("link", rssStar.link)
        }
    }
}