package ltd.finelink.read.ui.main.store.voice

import android.content.Context
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import ltd.finelink.read.base.adapter.DiffRecyclerAdapter
import ltd.finelink.read.data.entities.TTSSpeaker

abstract class BaseVoiceAdapter<VB : ViewBinding>(context: Context) :
    DiffRecyclerAdapter<TTSSpeaker, VB>(context) {

    override val diffItemCallback: DiffUtil.ItemCallback<TTSSpeaker> =
        object : DiffUtil.ItemCallback<TTSSpeaker>() {

            override fun areItemsTheSame(oldItem: TTSSpeaker, newItem: TTSSpeaker): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: TTSSpeaker, newItem: TTSSpeaker): Boolean {
                return when {
                    oldItem.lastUpdateTime != newItem.lastUpdateTime -> false
                    oldItem.name != newItem.name -> false
                    oldItem.id != newItem.id -> false
                    oldItem.path != newItem.path -> false
                    oldItem.progress != newItem.progress ->false
                    oldItem.description != newItem.description -> false
                    oldItem.type != newItem.type -> false
                    oldItem.download != newItem.download-> false
                    else -> true
                }
            }

            override fun getChangePayload(oldItem: TTSSpeaker, newItem: TTSSpeaker): Any? {
                val bundle = bundleOf()
                if (oldItem.name != newItem.name) {
                    bundle.putString("name", newItem.name)
                }
                if (oldItem.description != newItem.description) {
                    bundle.putString("description", newItem.description)
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
                    || oldItem.description!= newItem.description
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


    fun downloadNotify(speaker:TTSSpeaker) {
        getItems().forEachIndexed { i, it ->
            if (it.id == speaker.id) {
                it.progress = speaker.progress
                it.download = speaker.download
                it.status = speaker.status
                notifyItemChanged(i, bundleOf(Pair("status", speaker.status),Pair("progress", speaker.progress),Pair("download", speaker.download)))
                return
            }
        }
    }



    interface CallBack {
        fun openSetting(speaker: TTSSpeaker)
        fun remove(speaker: TTSSpeaker,ask:Boolean=true): Boolean

        fun openVoiceInfo(speaker: TTSSpeaker)
    }
}