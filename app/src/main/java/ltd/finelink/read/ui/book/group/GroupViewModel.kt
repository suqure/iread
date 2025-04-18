package ltd.finelink.read.ui.book.group

import android.app.Application
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.BookGroup

class GroupViewModel(application: Application) : BaseViewModel(application) {

    fun upGroup(vararg bookGroup: BookGroup, finally: (() -> Unit)? = null) {
        execute {
            appDb.bookGroupDao.update(*bookGroup)
        }.onFinally {
            finally?.invoke()
        }
    }

    fun addGroup(
        groupName: String,
        bookSort: Int,
        enableRefresh: Boolean,
        cover: String?,
        finally: () -> Unit
    ) {
        execute {
            val groupId = appDb.bookGroupDao.getUnusedId()
            val bookGroup = BookGroup(
                groupId = groupId,
                groupName = groupName,
                cover = cover,
                bookSort = bookSort,
                enableRefresh = enableRefresh,
                order = appDb.bookGroupDao.maxOrder.plus(1)
            )
            appDb.bookGroupDao.getByID(groupId) ?: appDb.bookDao.removeGroup(groupId)
            appDb.bookGroupDao.insert(bookGroup)
        }.onFinally {
            finally()
        }
    }

    fun delGroup(bookGroup: BookGroup, finally: () -> Unit) {
        execute {
            appDb.bookGroupDao.delete(bookGroup)
            appDb.bookDao.removeGroup(bookGroup.groupId)
        }.onFinally {
            finally()
        }
    }


}