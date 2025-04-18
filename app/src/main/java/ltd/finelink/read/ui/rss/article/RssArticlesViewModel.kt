package ltd.finelink.read.ui.rss.article

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.RssArticle
import ltd.finelink.read.data.entities.RssSource
import ltd.finelink.read.model.rss.Rss
import ltd.finelink.read.utils.stackTraceStr
import kotlinx.coroutines.Dispatchers.IO


class RssArticlesViewModel(application: Application) : BaseViewModel(application) {
    val loadFinallyLiveData = MutableLiveData<Boolean>()
    val loadErrorLiveData = MutableLiveData<String>()
    var isLoading = true
    var order = System.currentTimeMillis()
    private var nextPageUrl: String? = null
    var sortName: String = ""
    var sortUrl: String = ""
    var page = 1

    fun init(bundle: Bundle?) {
        bundle?.let {
            sortName = it.getString("sortName") ?: ""
            sortUrl = it.getString("sortUrl") ?: ""
        }
    }

    fun loadArticles(rssSource: RssSource) {
        isLoading = true
        page = 1
        order = System.currentTimeMillis()
        Rss.getArticles(viewModelScope, sortName, sortUrl, rssSource, page).onSuccess(IO) {
            nextPageUrl = it.second
            val articles = it.first
            articles.forEach { rssArticle ->
                rssArticle.order = order--
            }
            appDb.rssArticleDao.insert(*articles.toTypedArray())
            if (!rssSource.ruleNextPage.isNullOrEmpty()) {
                appDb.rssArticleDao.clearOld(rssSource.sourceUrl, sortName, order)
            }
            val hasMore = articles.isNotEmpty() && !rssSource.ruleNextPage.isNullOrEmpty()
            loadFinallyLiveData.postValue(hasMore)
            isLoading = false
        }.onError {
            loadFinallyLiveData.postValue(false)
            AppLog.put("rss获取内容失败", it)
            loadErrorLiveData.postValue(it.stackTraceStr)
        }
    }

    fun loadMore(rssSource: RssSource) {
        isLoading = true
        page++
        val pageUrl = nextPageUrl
        if (pageUrl.isNullOrEmpty()) {
            loadFinallyLiveData.postValue(false)
            return
        }
        Rss.getArticles(viewModelScope, sortName, pageUrl, rssSource, page).onSuccess(IO) {
            nextPageUrl = it.second
            loadMoreSuccess(it.first)
            isLoading = false
        }.onError {
            loadFinallyLiveData.postValue(false)
            AppLog.put("rss获取内容失败", it)
            loadErrorLiveData.postValue(it.stackTraceStr)
        }
    }

    private fun loadMoreSuccess(articles: MutableList<RssArticle>) {
        if (articles.isEmpty()) {
            loadFinallyLiveData.postValue(false)
            return
        }
        val firstArticle = articles.first()
        val dbArticle = appDb.rssArticleDao.get(firstArticle.origin, firstArticle.link)
        if (dbArticle != null) {
            loadFinallyLiveData.postValue(false)
        } else {
            articles.forEach {
                it.order = order--
            }
            appDb.rssArticleDao.insert(*articles.toTypedArray())
        }
    }

}