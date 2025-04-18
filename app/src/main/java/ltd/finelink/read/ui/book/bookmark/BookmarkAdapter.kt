package ltd.finelink.read.ui.book.bookmark

import android.content.Context
import android.view.ViewGroup
import ltd.finelink.read.base.adapter.ItemViewHolder
import ltd.finelink.read.base.adapter.RecyclerAdapter
import ltd.finelink.read.data.entities.Bookmark
import ltd.finelink.read.databinding.ItemBookmarkBinding
import ltd.finelink.read.utils.gone
import splitties.views.onClick

class BookmarkAdapter(context: Context, val callback: Callback) :
    RecyclerAdapter<Bookmark, ItemBookmarkBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemBookmarkBinding {
        return ItemBookmarkBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBookmarkBinding,
        item: Bookmark,
        payloads: MutableList<Any>
    ) {
        binding.tvChapterName.text = item.chapterName
        binding.tvBookText.gone(item.bookText.isEmpty())
        binding.tvBookText.text = item.bookText
        binding.tvContent.gone(item.content.isEmpty())
        binding.tvContent.text = item.content
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBookmarkBinding) {
        binding.root.onClick {
            getItemByLayoutPosition(holder.layoutPosition)?.let {
                callback.onItemClick(it, holder.layoutPosition)
            }
        }
    }

    fun getHeaderText(position: Int): String {
        return with(getItem(position)) {
            "${this?.bookName ?: ""}(${this?.bookAuthor ?: ""})"
        }
    }

    fun isItemHeader(position: Int): Boolean {
        if (position == 0) return true
        val lastItem = getItem(position - 1)
        val curItem = getItem(position)
        return !(lastItem?.bookName == curItem?.bookName
                && lastItem?.bookAuthor == curItem?.bookAuthor)
    }

    interface Callback {

        fun onItemClick(bookmark: Bookmark, position: Int)

    }

}