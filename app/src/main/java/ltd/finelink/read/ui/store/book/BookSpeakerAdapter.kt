package ltd.finelink.read.ui.store.book

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DiffUtil
import ltd.finelink.read.R
import ltd.finelink.read.base.adapter.ItemViewHolder
import ltd.finelink.read.base.adapter.RecyclerAdapter
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.BookSpeaker
import ltd.finelink.read.databinding.ItemBookSpeakerBinding
import ltd.finelink.read.lib.theme.backgroundColor
import ltd.finelink.read.utils.ColorUtils


class BookSpeakerAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<BookSpeaker, ItemBookSpeakerBinding>(context)  {

    val diffItemCallback = object : DiffUtil.ItemCallback<BookSpeaker>() {

        override fun areItemsTheSame(oldItem: BookSpeaker, newItem: BookSpeaker): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BookSpeaker, newItem: BookSpeaker): Boolean {
            return oldItem.speakerId == newItem.speakerId
                    && oldItem.bookUrl == newItem.bookUrl
                    && oldItem.spkName == newItem.spkName
        }

        override fun getChangePayload(oldItem: BookSpeaker, newItem: BookSpeaker): Any? {
            val payload = Bundle()
            if (oldItem.speakerId != newItem.speakerId
                || oldItem.spkName != newItem.spkName
            ) {
                payload.putBoolean("upName", true)
            }
            if (payload.isEmpty) {
                return null
            }
            return payload
        }
    }

    override fun getViewBinding(parent: ViewGroup): ItemBookSpeakerBinding {
        return ItemBookSpeakerBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBookSpeakerBinding,
        item: BookSpeaker,
        payloads: MutableList<Any>
    ) {
        binding.run {
            val bundle = payloads.getOrNull(0) as? Bundle
            if (bundle == null) {
                root.setBackgroundColor(ColorUtils.withAlpha(context.backgroundColor, 0.5f))
                cbName.text = "${item.spkName}  ${getSpeakerName(item.speakerId)}"
            } else {
                bundle.keySet().map {
                    when (it) {
                        "upName" -> cbName.text = "${item.spkName}  ${getSpeakerName(item.speakerId)}"
                    }
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBookSpeakerBinding) {
        binding.apply {

            ivEdit.setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.edit(it)
                }
            }
        }
    }

    private fun getSpeakerName(id:Long=0):String{
        var speaker = appDb.ttsSpeakerDao.get(id)
        return speaker?.name?:context.getString(R.string.text_default)
    }

    fun changeItem(bookSpeaker: BookSpeaker){
        var isNew = true
        getItems().forEachIndexed { i, it ->
            if (it.id == bookSpeaker.id) {
                it.speakerId = bookSpeaker.speakerId
                notifyItemChanged(i, bundleOf(Pair("upName", bookSpeaker)))
                isNew = false
                return
            }
        }
        if(isNew){
            addItem(bookSpeaker)
        }
    }

    interface CallBack {
        fun edit(source: BookSpeaker)
    }
}
