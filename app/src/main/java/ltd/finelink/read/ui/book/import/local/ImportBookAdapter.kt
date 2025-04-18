package ltd.finelink.read.ui.book.import.local

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import ltd.finelink.read.R
import ltd.finelink.read.base.adapter.ItemViewHolder
import ltd.finelink.read.base.adapter.RecyclerAdapter
import ltd.finelink.read.constant.AppConst
import ltd.finelink.read.databinding.ItemImportBookBinding
import ltd.finelink.read.model.localBook.LocalBook
import ltd.finelink.read.utils.*
import ltd.finelink.read.utils.ConvertUtils
import ltd.finelink.read.utils.FileDoc


class ImportBookAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<FileDoc, ItemImportBookBinding>(context) {
    val selectedUris = hashSetOf<String>()
    var checkableCount = 0

    override fun getViewBinding(parent: ViewGroup): ItemImportBookBinding {
        return ItemImportBookBinding.inflate(inflater, parent, false)
    }

    override fun onCurrentListChanged() {
        upCheckableCount()
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemImportBookBinding,
        item: FileDoc,
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
                    if (isOnBookShelf(item)) {
                        ivIcon.setImageResource(R.drawable.ic_book_has)
                        ivIcon.visible()
                        cbSelect.invisible()
                    } else {
                        ivIcon.invisible()
                        cbSelect.visible()
                    }
                    llBrief.visible()
                    tvTag.text = item.name.substringAfterLast(".")
                    tvSize.text = ConvertUtils.formatFileSize(item.size)
                    tvDate.text = AppConst.dateFormat.format(item.lastModified)
                    cbSelect.isChecked = selectedUris.contains(item.toString())
                }
                tvName.text = item.name
            } else {
                cbSelect.isChecked = selectedUris.contains(item.toString())
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemImportBookBinding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                if (it.isDir) {
                    callBack.nextDoc(it)
                } else if (!isOnBookShelf(it)) {
                    if (!selectedUris.contains(it.toString())) {
                        selectedUris.add(it.toString())
                    } else {
                        selectedUris.remove(it.toString())
                    }
                    notifyItemChanged(holder.layoutPosition, true)
                    callBack.upCountView()
                } else {
                    /* 点击开始阅读 */
                    callBack.startRead(it)
                }
            }
        }
    }

    private fun isOnBookShelf(fileDoc: FileDoc): Boolean {
        return LocalBook.isOnBookShelf(fileDoc.name)
    }

    private fun upCheckableCount() {
        checkableCount = 0
        getItems().forEach {
            if (!it.isDir && !isOnBookShelf(it)) {
                checkableCount++
            }
        }
        callBack.upCountView()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectAll(selectAll: Boolean) {
        if (selectAll) {
            getItems().forEach {
                if (!it.isDir && !isOnBookShelf(it)) {
                    selectedUris.add(it.toString())
                }
            }
        } else {
            selectedUris.clear()
        }
        notifyDataSetChanged()
        callBack.upCountView()
    }

    fun revertSelection() {
        getItems().forEach {
            if (!it.isDir && !isOnBookShelf(it)) {
                if (selectedUris.contains(it.toString())) {
                    selectedUris.remove(it.toString())
                } else {
                    selectedUris.add(it.toString())
                }
            }
        }
        notifyItemRangeChanged(0, itemCount, true)
        callBack.upCountView()
    }

    fun removeSelection() {
        for (i in getItems().lastIndex downTo 0) {
            if (getItem(i)?.toString() in selectedUris) {
                removeItem(i)
            }
        }
    }

    interface CallBack {
        fun nextDoc(fileDoc: FileDoc)
        fun upCountView()
        fun startRead(fileDoc: FileDoc)
    }

}