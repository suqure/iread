package ltd.finelink.read.ui.main.explore

import android.app.Application
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.BookSourcePart
import ltd.finelink.read.help.config.SourceConfig

class ExploreViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(bookSource: BookSourcePart) {
        execute {
            val minXh = appDb.bookSourceDao.minOrder
            bookSource.customOrder = minXh - 1
            appDb.bookSourceDao.upOrder(bookSource)
        }
    }

    fun deleteSource(source: BookSourcePart) {
        execute {
            appDb.bookSourceDao.delete(source.bookSourceUrl)
            SourceConfig.removeSource(source.bookSourceUrl)
        }
    }

}