package ltd.finelink.read.ui.book.search

import android.view.ViewGroup
import ltd.finelink.read.base.adapter.ItemViewHolder
import ltd.finelink.read.base.adapter.RecyclerAdapter
import ltd.finelink.read.data.entities.SearchKeyword
import ltd.finelink.read.databinding.ItemFilletTextBinding
import ltd.finelink.read.ui.widget.anima.explosion_field.ExplosionField
import splitties.views.onLongClick

class HistoryKeyAdapter(activity: SearchActivity, val callBack: CallBack) :
    RecyclerAdapter<SearchKeyword, ItemFilletTextBinding>(activity) {

    private val explosionField = ExplosionField.attach2Window(activity)

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getViewBinding(parent: ViewGroup): ItemFilletTextBinding {
        return ItemFilletTextBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemFilletTextBinding,
        item: SearchKeyword,
        payloads: MutableList<Any>
    ) {
        binding.run {
            textView.text = item.word
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemFilletTextBinding) {
        holder.itemView.apply {
            setOnClickListener {
                getItemByLayoutPosition(holder.layoutPosition)?.let {
                    callBack.searchHistory(it.word)
                }
            }
            onLongClick {
                explosionField.explode(this, true)
                getItemByLayoutPosition(holder.layoutPosition)?.let {
                    callBack.deleteHistory(it)
                }
            }
        }
    }

    interface CallBack {
        fun searchHistory(key: String)
        fun deleteHistory(searchKeyword: SearchKeyword)
    }
}