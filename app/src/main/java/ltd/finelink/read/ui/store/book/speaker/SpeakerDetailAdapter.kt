package ltd.finelink.read.ui.store.book.speaker

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ltd.finelink.read.base.adapter.ItemViewHolder
import ltd.finelink.read.base.adapter.RecyclerAdapter
import ltd.finelink.read.data.entities.BookSpeakerDetail
import ltd.finelink.read.databinding.ItemBookSpeakerDetailBinding
import ltd.finelink.read.lib.theme.backgroundColor
import ltd.finelink.read.ui.widget.recycler.DragSelectTouchHelper
import ltd.finelink.read.ui.widget.recycler.ItemTouchCallback
import ltd.finelink.read.utils.ColorUtils


class SpeakerDetailAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<BookSpeakerDetail, ItemBookSpeakerDetailBinding>(context),
    ItemTouchCallback.Callback {

    private val selected = linkedSetOf<BookSpeakerDetail>()

    val selection: List<BookSpeakerDetail>
        get() {
            return getItems().filter {
                selected.contains(it)
            }
        }

    val diffItemCallback = object : DiffUtil.ItemCallback<BookSpeakerDetail>() {

        override fun areItemsTheSame(oldItem: BookSpeakerDetail, newItem: BookSpeakerDetail): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BookSpeakerDetail, newItem: BookSpeakerDetail): Boolean {
            return oldItem.spkName == newItem.spkName
                    && oldItem.chapter == newItem.chapter
                    && oldItem.bookUrl == newItem.bookUrl
                    && oldItem.pos == newItem.pos
                    && oldItem.text == newItem.text
        }

        override fun getChangePayload(oldItem: BookSpeakerDetail, newItem: BookSpeakerDetail): Any? {
            val payload = Bundle()
            if (oldItem.spkName != newItem.spkName
            ) {
                payload.putBoolean("upName", true)
            }
            if (payload.isEmpty) {
                return null
            }
            return payload
        }
    }

    override fun getViewBinding(parent: ViewGroup): ItemBookSpeakerDetailBinding {
        return ItemBookSpeakerDetailBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBookSpeakerDetailBinding,
        item: BookSpeakerDetail,
        payloads: MutableList<Any>
    ) {
        binding.run {
            val bundle = payloads.getOrNull(0) as? Bundle
            if (bundle == null) {
                root.setBackgroundColor(ColorUtils.withAlpha(context.backgroundColor, 0.5f))
                cbSource.text = item.spkName
                titleExample.text = item.text
                cbSource.isChecked = selected.contains(item)
            } else {
                bundle.keySet().map {
                    when (it) {
                        "upName" -> cbSource.text = item.spkName
                        "upText" -> titleExample.text = item.text
                        "selected" -> cbSource.isChecked = selected.contains(item)
                    }
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBookSpeakerDetailBinding) {
        binding.apply {
            cbSource.setOnCheckedChangeListener { view, checked ->
                if (view.isPressed) {
                    getItem(holder.layoutPosition)?.let {
                        if (view.isPressed) {
                            if (checked) {
                                selected.add(it)
                            } else {
                                selected.remove(it)
                            }
                            callBack.upCountView()
                        }
                    }
                }
            }
            ivEdit.setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.edit(it)
                }
            }
            ivMenuDelete.setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.del(it)
                }
            }
        }
    }

    override fun onCurrentListChanged() {
        callBack.upCountView()
    }

    fun selectAll() {
        getItems().forEach {
            selected.add(it)
        }
        notifyItemRangeChanged(0, itemCount, bundleOf(Pair("selected", null)))
        callBack.upCountView()
    }

    fun revertSelection() {
        getItems().forEach {
            if (selected.contains(it)) {
                selected.remove(it)
            } else {
                selected.add(it)
            }
        }
        notifyItemRangeChanged(0, itemCount, bundleOf(Pair("selected", null)))
        callBack.upCountView()
    }

    private val movedItems = hashSetOf<BookSpeakerDetail>()

    override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (movedItems.isNotEmpty()) {
            movedItems.clear()
        }
    }

    val dragSelectCallback: DragSelectTouchHelper.Callback =
        object : DragSelectTouchHelper.AdvanceCallback<BookSpeakerDetail>(Mode.ToggleAndReverse) {
            override fun currentSelectedId(): MutableSet<BookSpeakerDetail> {
                return selected
            }

            override fun getItemId(position: Int): BookSpeakerDetail {
                return getItem(position)!!
            }

            override fun updateSelectState(position: Int, isSelected: Boolean): Boolean {
                getItem(position)?.let {
                    if (isSelected) {
                        selected.add(it)
                    } else {
                        selected.remove(it)
                    }
                    notifyItemChanged(position, bundleOf(Pair("selected", null)))
                    callBack.upCountView()
                    return true
                }
                return false
            }
        }

    interface CallBack {
        fun del(source: BookSpeakerDetail)
        fun edit(source: BookSpeakerDetail)
        fun upCountView()
    }
}
