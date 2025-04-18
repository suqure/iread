package ltd.finelink.read.ui.main.bookshelf.style1.books

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import ltd.finelink.read.base.adapter.ItemViewHolder
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.databinding.ItemBookshelfListBinding
import ltd.finelink.read.help.book.isLocal
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.utils.invisible
import ltd.finelink.read.utils.toTimeAgo
import splitties.views.onLongClick

class BooksAdapterList(
    context: Context,
    private val callBack: CallBack,
    private val lifecycle: Lifecycle
) :
    BaseBooksAdapter<ItemBookshelfListBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemBookshelfListBinding {
        return ItemBookshelfListBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBookshelfListBinding,
        item: Book,
        payloads: MutableList<Any>
    ) = binding.run {
        val bundle = payloads.getOrNull(0) as? Bundle
        if (bundle == null) {
            tvName.text = item.name
            tvAuthor.text = item.author
            tvRead.text = item.durChapterTitle
            tvLast.text = item.latestChapterTitle
            ivCover.load(item.getDisplayCover(), item.name, item.author, false, item.origin)
            upRefresh(binding, item)
            upLastUpdateTime(binding, item)
            ivMenuReadAloud.setOnClickListener{
                callBack.readAloudBook(item)
            }
             
        } else {
            bundle.keySet().forEach {
                when (it) {
                    "name" -> tvName.text = item.name
                    "author" -> tvAuthor.text = item.author
                    "dur" -> tvRead.text = item.durChapterTitle
                    "last" -> tvLast.text = item.latestChapterTitle
                    "cover" -> ivCover.load(
                        item.getDisplayCover(),
                        item.name,
                        item.author,
                        false,
                        item.origin,
                        lifecycle
                    )

                    "refresh" -> upRefresh(binding, item)
                    "lastUpdateTime" -> upLastUpdateTime(binding, item)
                }
            }
        }
    }

    private fun upRefresh(binding: ItemBookshelfListBinding, item: Book) {
        if (!item.isLocal && callBack.isUpdate(item.bookUrl)) {
            binding.bvUnread.invisible()
            binding.rlLoading.visible()
        } else {
            binding.rlLoading.gone()
            if (AppConfig.showUnread) {
                binding.bvUnread.setHighlight(item.lastCheckCount > 0)
                binding.bvUnread.setBadgeCount(item.getUnreadChapterNum())
            } else {
                binding.bvUnread.invisible()
            }
        }
    }

    private fun upLastUpdateTime(binding: ItemBookshelfListBinding, item: Book) {
        if (AppConfig.showLastUpdateTime && !item.isLocal) {
            val time = item.latestChapterTime.toTimeAgo()
            if (binding.tvLastUpdateTime.text != time) {
                binding.tvLastUpdateTime.text = time
            }
        } else {
            binding.tvLastUpdateTime.text = ""
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBookshelfListBinding) {
        holder.itemView.apply {
            setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.open(it)
                }
            }

            onLongClick {
                getItem(holder.layoutPosition)?.let {
                    callBack.openBookInfo(it)
                }
            }
        }
    }
}