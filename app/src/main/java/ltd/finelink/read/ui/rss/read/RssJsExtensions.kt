package ltd.finelink.read.ui.rss.read

import ltd.finelink.read.data.entities.BaseSource
import ltd.finelink.read.help.JsExtensions
import ltd.finelink.read.ui.association.AddToBookshelfDialog
import ltd.finelink.read.ui.book.search.SearchActivity
import ltd.finelink.read.utils.showDialogFragment

@Suppress("unused")
class RssJsExtensions(private val activity: ReadRssActivity) : JsExtensions {

    override fun getSource(): BaseSource? {
        return activity.getSource()
    }

    fun searchBook(key: String) {
        SearchActivity.start(activity, key)
    }

    fun addBook(bookUrl: String) {
        activity.showDialogFragment(AddToBookshelfDialog(bookUrl))
    }

}
