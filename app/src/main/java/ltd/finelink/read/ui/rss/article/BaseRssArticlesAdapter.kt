package ltd.finelink.read.ui.rss.article

import android.content.Context
import androidx.viewbinding.ViewBinding
import ltd.finelink.read.base.adapter.RecyclerAdapter
import ltd.finelink.read.data.entities.RssArticle


abstract class BaseRssArticlesAdapter<VB : ViewBinding>(context: Context, val callBack: CallBack) :
    RecyclerAdapter<RssArticle, VB>(context) {

    interface CallBack {
        val isGridLayout: Boolean
        fun readRss(rssArticle: RssArticle)
    }
}