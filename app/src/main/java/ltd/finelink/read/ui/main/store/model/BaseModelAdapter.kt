package ltd.finelink.read.ui.main.store.model

import android.content.Context
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import ltd.finelink.read.base.adapter.DiffRecyclerAdapter
import ltd.finelink.read.data.entities.LocalTTS

abstract class BaseModelAdapter<VB : ViewBinding>(context: Context) :
    DiffRecyclerAdapter<LocalTTS, VB>(context) {

    override val diffItemCallback: DiffUtil.ItemCallback<LocalTTS> =
        object : DiffUtil.ItemCallback<LocalTTS>() {

            override fun areItemsTheSame(oldItem: LocalTTS, newItem: LocalTTS): Boolean {
                return oldItem.name == newItem.name
                        && oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: LocalTTS, newItem: LocalTTS): Boolean {
                return when {
                    oldItem.lastUpdateTime != newItem.lastUpdateTime -> false
                    oldItem.name != newItem.name -> false
                    oldItem.id != newItem.id -> false
                    oldItem.path != newItem.path -> false
                    oldItem.speaker != newItem.speaker -> false
                    oldItem.speakerName != newItem.speakerName -> false
                    oldItem.speakerId != newItem.speakerId -> false
                    oldItem.type != newItem.type -> false
                    oldItem.progress != newItem.progress ->false
                    oldItem.download != newItem.download-> false
                    else -> true
                }
            }

            override fun getChangePayload(oldItem: LocalTTS, newItem: LocalTTS): Any? {
                val bundle = bundleOf()
                if (oldItem.name != newItem.name) {
                    bundle.putString("name", newItem.name)
                }
                if (oldItem.speaker != newItem.speaker) {
                    bundle.putString("speaker", newItem.speaker)
                }
                if (oldItem.speakerName != newItem.speakerName) {
                    bundle.putString("speakerName", newItem.speakerName)
                }
                if (oldItem.download != newItem.download) {
                    bundle.putBoolean("download", newItem.download)
                }
                if (oldItem.progress != newItem.progress) {
                    bundle.putInt("progress", newItem.progress)
                }
                if (oldItem.download != newItem.download
                    || oldItem.progress != newItem.progress
                    || oldItem.name != newItem.name
                    || oldItem.speaker != newItem.speaker
                    || oldItem.speakerName != newItem.speakerName
                ) {
                    bundle.putBoolean("refresh", true)
                }
                if (oldItem.lastUpdateTime != newItem.lastUpdateTime) {
                    bundle.putBoolean("lastUpdateTime", true)
                }
                if (bundle.isEmpty) return null
                return bundle
            }

        }

    fun downloadNotify(model: LocalTTS) {
        getItems().forEachIndexed { i, it ->
            if (it.id == model.id) {
                it.progress = model.progress
                it.download = model.download
                it.status = model.status
                notifyItemChanged(i, bundleOf(Pair("status", model.status),Pair("progress", model.progress),Pair("download", model.download)))
                return
            }
        }
    }
    fun notifyChange(model: LocalTTS) {
        getItems().forEachIndexed { i, it ->
            if (it.id == model.id) {
                notifyItemChanged(i, bundleOf(Pair("refresh", model)))
                return
            }
        }
    }

    fun upLastUpdateTime() {
        notifyItemRangeChanged(0, itemCount, bundleOf(Pair("lastUpdateTime", null)))
    }

    interface CallBack {
        fun openSetting(book: LocalTTS)
        fun remove(model: LocalTTS,ask:Boolean=true): Boolean
        fun openModelInfo(model:LocalTTS)

        fun setEngine(model:LocalTTS)
    }
}