package ltd.finelink.read.ui.store.cache

import android.app.Application
import android.os.Bundle
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.TTSCache

class TTSCacheEditViewModel(app: Application) : BaseViewModel(app) {

    var id: Long? = null

    var record: TTSCache?=null

    fun initData(arguments: Bundle?, success: (cache: TTSCache) -> Unit) {
        execute {
            if (id == null) {
                val argumentId = arguments?.getLong("id")
                if (argumentId != null && argumentId != 0L) {
                    id = argumentId
                    record = appDb.ttsCacheDao.get(argumentId)
                    return@execute record
                }
            }
            return@execute null
        }.onSuccess {
            it?.let {
                success.invoke(it)
            }
        }
    }

    fun save(cache: TTSCache, success: (() -> Unit)? = null) {
        id = cache.id
        execute {
            appDb.ttsCacheDao.update(cache)
        }.onSuccess {
            success?.invoke()
        }
    }

}