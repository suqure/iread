package ltd.finelink.read.ui.book.import.remote

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import ltd.finelink.read.R
import ltd.finelink.read.base.adapter.ItemViewHolder
import ltd.finelink.read.base.adapter.RecyclerAdapter
import ltd.finelink.read.constant.AppConst
import ltd.finelink.read.databinding.ItemImportBookBinding
import ltd.finelink.read.model.remote.RemoteBook
import ltd.finelink.read.utils.ConvertUtils
import ltd.finelink.read.utils.gone
import ltd.finelink.read.utils.invisible
import ltd.finelink.read.utils.visible


/**
 * 适配器
 * @author qianfanguojin
 */
class RemoteBookAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<RemoteBook, ItemImportBookBinding>(context) {
    var selected = hashSetOf<RemoteBook>()
    var checkableCount = 0

    override fun getViewBinding(parent: ViewGroup): ItemImportBookBinding {
        return ItemImportBookBinding.inflate(inflater, parent, false)
    }

    override fun onCurrentListChanged() {
        upCheckableCount()
    }

    /**
     * 绑定RecycleView 中每一个项的视图和数据
     */
    override fun convert(
        holder: ItemViewHolder,
        binding: ItemImportBookBinding,
        item: RemoteBook,
        payloads: MutableList<Any>
    ) {
        binding.run {
            if (payloads.isEmpty()) {
                if (item.isDir) {
                    ivIcon.setImageResource(R.drawable.ic_folder)
                    ivIcon.visible()
                    cbSelect.invisible()
                    llBrief.gone()
                    cbSelect.isChecked = false
                } else {
                    if (item.isOnBookShelf) {
                        ivIcon.setImageResource(R.drawable.ic_book_has)
                        ivIcon.visible()
                        cbSelect.invisible()
                    } else {
                        ivIcon.invisible()
                        cbSelect.visible()
                    }
                    llBrief.visible()
                    tvTag.text = item.contentType
                    tvSize.text = ConvertUtils.formatFileSize(item.size)
                    tvDate.text = AppConst.dateFormat.format(item.lastModify)
                    cbSelect.isChecked = selected.contains(item)
                }
                tvName.text = item.filename
            } else {
                cbSelect.isChecked = selected.contains(item)
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemImportBookBinding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                if (it.isDir) {
                    callBack.openDir(it)
                } else if (!it.isOnBookShelf) {
                    if (!selected.contains(it)) {
                        selected.add(it)
                    } else {
                        selected.remove(it)
                    }
                    notifyItemChanged(holder.layoutPosition, true)
                    callBack.upCountView()
                } else {
                    /* 点击开始阅读 */
                    callBack.startRead(it)
                }
            }
        }
        holder.itemView.setOnLongClickListener {
            getItem(holder.layoutPosition)?.let { remoteBook ->
                if (remoteBook.isOnBookShelf) {
                    callBack.addToBookShelfAgain(remoteBook)
                }
            }
            true
        }
    }

    private fun upCheckableCount() {
        checkableCount = 0
        getItems().forEach {
            if (!it.isDir && !it.isOnBookShelf) {
                checkableCount++
            }
        }
        callBack.upCountView()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectAll(selectAll: Boolean) {
        if (selectAll) {
            getItems().forEach {
                if (!it.isDir && !it.isOnBookShelf) {
                    selected.add(it)
                }
            }
        } else {
            selected.clear()
        }
        notifyDataSetChanged()
        callBack.upCountView()
    }

    fun revertSelection() {
        getItems().forEach {
            if (!it.isDir && !it.isOnBookShelf) {
                if (selected.contains(it)) {
                    selected.remove(it)
                } else {
                    selected.add(it)
                }
            }
        }
        notifyItemRangeChanged(0, itemCount, true)
        callBack.upCountView()
    }

    fun removeSelection() {
        for (i in getItems().lastIndex downTo 0) {
            if (getItem(i) in selected) {
                removeItem(i)
            }
        }
    }


    interface CallBack {
        fun openDir(remoteBook: RemoteBook)
        fun upCountView()
        fun startRead(remoteBook: RemoteBook)
        fun addToBookShelfAgain(remoteBook: RemoteBook)
    }
}
