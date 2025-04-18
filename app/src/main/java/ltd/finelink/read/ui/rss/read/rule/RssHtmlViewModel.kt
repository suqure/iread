package ltd.finelink.read.ui.rss.read.rule

import android.app.Application
import androidx.lifecycle.MutableLiveData
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.RssReadAloudRule
import ltd.finelink.read.utils.isAbsUrl
import ltd.finelink.read.utils.normalText
import org.jsoup.Jsoup
import org.jsoup.nodes.Element


class RssHtmlViewModel(app: Application) : BaseViewModel(app) {

    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<Element>()
    val checkSources = arrayListOf<Element?>()
    val selectStatus = arrayListOf<Boolean>()
    val ignoreTags:MutableSet<String> = mutableSetOf()
    val acceptTags:MutableSet<String> = mutableSetOf()
    val ignoreIds:MutableSet<String> = mutableSetOf()
    val acceptIds:MutableSet<String> = mutableSetOf()
    val ignoreClass:MutableSet<String> = mutableSetOf()
    val acceptClass:MutableSet<String> = mutableSetOf()
    private var rssRule:RssReadAloudRule?=null


    val isSelectAll: Boolean
        get() {
            selectStatus.forEach {
                if (!it) {
                    return false
                }
            }
            return true
        }

    val selectCount: Int
        get() {
            var count = 0
            selectStatus.forEach {
                if (it) {
                    count++
                }
            }
            return count
        }

    fun saveRule(finally: () -> Unit) {
        execute {
            rssRule?.let {
                if(ignoreIds.isNotEmpty()){
                    it.ignoreIds = ignoreIds.joinToString(",")
                }else{
                    it.ignoreIds = ""
                }
                if(ignoreTags.isNotEmpty()){
                    it.ignoreTags= ignoreTags.joinToString(",")
                }else{
                    it.ignoreTags = ""
                }
                if(ignoreClass.isNotEmpty()){
                    it.ignoreClass=ignoreClass.joinToString(",")
                }else{
                    it.ignoreClass = ""
                }
                if(acceptIds.isNotEmpty()){
                    it.acceptIds = acceptIds.joinToString(",")
                }else{
                    it.acceptIds = ""
                }
                if(acceptTags.isNotEmpty()){
                    it.acceptTags = acceptTags.joinToString(",")
                }else{
                    it.acceptTags = ""
                }
                if(acceptClass.isNotEmpty()){
                    it.acceptClass = acceptClass.joinToString(",")
                }else{
                    it.acceptClass = ""
                }

                appDb.rssReadAloudRuleDao.update(it)
            }

        }.onFinally {
            finally.invoke()
        }
    }

    fun initSource(rss:String,text: String) {
        execute {
            importSourceAwait(text.trim())
            appDb.rssReadAloudRuleDao.get(rss)?.let {
                rssRule = it
                for(t in it.acceptTags.split(",")){
                    if(t.trim().isNotEmpty()){
                        acceptTags.add(t.trim())
                    }
                }
                for(t in it.ignoreTags.split(",")){
                    if(t.trim().isNotEmpty()){
                        ignoreTags.add(t.trim())
                    }
                }
                for(t in it.acceptIds.split(",")){
                    if(t.trim().isNotEmpty()){
                        acceptIds.add(t.trim())
                    }
                }
                for(t in it.ignoreIds.split(",")){
                    if(t.trim().isNotEmpty()){
                        ignoreIds.add(t.trim())
                    }
                }
                for(t in it.acceptClass.split(",")){
                    if(t.trim().isNotEmpty()){
                        acceptClass.add(t.trim())
                    }
                }
                for(t in it.ignoreClass.split(",")){
                    if(t.trim().isNotEmpty()){
                        ignoreClass.add(t.trim())
                    }
                }
            }
            if(rssRule==null){
                rssRule = RssReadAloudRule(rss)
                appDb.rssReadAloudRuleDao.insert(rssRule!!)
            }
        }.onError {
            errorLiveData.postValue("ImportError:${it.localizedMessage}")
            AppLog.put("ImportError:${it.localizedMessage}", it)
        }.onSuccess {
            comparisonSource()

        }
    }

    private fun importSourceAwait(text: String) {
        when {
            text.isAbsUrl() -> {
                val document = Jsoup.connect(text).get()
                for(e in document.body().allElements){
                    if(e.normalText().isNotEmpty()){
                        allSources.add(e)
                    }
                }
            }
            else ->  {
                var document = Jsoup.parse(text)
                for(e in document.body().allElements){
                    if(e.normalText().isNotEmpty()){
                        allSources.add(e)
                    }
                }
            }
        }
    }



    private fun comparisonSource() {
        execute {
            allSources.forEach {
                checkSources.add(it)
                selectStatus.add(false)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}