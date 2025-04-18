package ltd.finelink.read.ui.store.book.speaker

import android.app.Application
import android.os.Bundle
import android.util.Log
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.BookSpeaker
import ltd.finelink.read.data.entities.BookSpeakerDetail
import ltd.finelink.read.utils.postEvent

class SpeakerDetailEditViewModel(app: Application) : BaseViewModel(app) {

    var id: Long? = null

    var record: BookSpeakerDetail?=null

    fun initData(arguments: Bundle?, success: (cache: BookSpeakerDetail) -> Unit) {
        execute {
            if (id == null) {
                val argumentId = arguments?.getLong("id")
                if (argumentId != null && argumentId != 0L) {
                    id = argumentId
                    record = appDb.bookSpeakerDetailDao.get(argumentId)
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

    fun save(speaker: BookSpeakerDetail, success: (() -> Unit)? = null) {
        id = speaker.id
        execute {
            appDb.bookSpeakerDetailDao.update(speaker)
            var bookSpeaker = appDb.bookSpeakerDao.get(speaker.bookUrl, speaker.spkName)
            if(bookSpeaker==null){
                appDb.readAloudBookDao.get(speaker.bookUrl)?.let {
                    var spk = BookSpeaker(System.currentTimeMillis(),speaker.bookUrl,it.modelId,0,speaker.spkName)
                    appDb.bookSpeakerDao.insert(spk)
                    postEvent(EventBus.UP_BOOK_SPEAKER,spk)
                }
            }
            var speakers = appDb.bookSpeakerDao.findByBook(speaker.bookUrl)
            for (spk in speakers){
                var count = appDb.bookSpeakerDetailDao.countSpeaker(spk.bookUrl,spk.spkName)
                Log.d("bookSpeaker","speaker ${spk.spkName},count:$count")
                if(count==0){
                    appDb.bookSpeakerDao.delete(spk)
                }
            }
            postEvent(EventBus.UP_BOOK_SPEAKER_DETAIL,speaker.bookUrl)

        }.onSuccess {
            success?.invoke()
        }
    }

}