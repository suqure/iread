package ltd.finelink.read.ui.store.cache

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ltd.finelink.read.R
import ltd.finelink.read.base.adapter.ItemViewHolder
import ltd.finelink.read.base.adapter.RecyclerAdapter
import ltd.finelink.read.data.entities.TTSCache
import ltd.finelink.read.databinding.ItemTtsCacheBinding
import ltd.finelink.read.lib.theme.backgroundColor
import ltd.finelink.read.ui.widget.recycler.DragSelectTouchHelper
import ltd.finelink.read.ui.widget.recycler.ItemTouchCallback
import ltd.finelink.read.utils.ColorUtils


class TTSCacheAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<TTSCache, ItemTtsCacheBinding>(context),
    ItemTouchCallback.Callback {

    private val selected = linkedSetOf<TTSCache>()

    val selection: List<TTSCache>
        get() {
            return getItems().filter {
                selected.contains(it)
            }
        }

    val diffItemCallback = object : DiffUtil.ItemCallback<TTSCache>() {

        override fun areItemsTheSame(oldItem: TTSCache, newItem: TTSCache): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TTSCache, newItem: TTSCache): Boolean {
            return oldItem.modelId == newItem.modelId
                    && oldItem.speakerId == newItem.speakerId
                    && oldItem.bookUrl == newItem.bookUrl
                    && oldItem.chapterIndex == newItem.chapterIndex
                    && oldItem.pageIndex == newItem.pageIndex
                    && oldItem.position == newItem.position
                    && oldItem.file == newItem.file
                    && oldItem.text == newItem.text
        }

        override fun getChangePayload(oldItem: TTSCache, newItem: TTSCache): Any? {
            val payload = Bundle()
            if (oldItem.modelId != newItem.modelId
                || oldItem.file != newItem.file
            ) {
                payload.putBoolean("upName", true)
            }
            if (payload.isEmpty) {
                return null
            }
            return payload
        }
    }

    override fun getViewBinding(parent: ViewGroup): ItemTtsCacheBinding {
        return ItemTtsCacheBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemTtsCacheBinding,
        item: TTSCache,
        payloads: MutableList<Any>
    ) {
        binding.run {
            val bundle = payloads.getOrNull(0) as? Bundle
            if (bundle == null) {
                root.setBackgroundColor(ColorUtils.withAlpha(context.backgroundColor, 0.5f))
                cbSource.text = item.bookName
                titleExample.text = item.text
                cbSource.isChecked = selected.contains(item)
            } else {
                bundle.keySet().map {
                    when (it) {
                        "upName" -> cbSource.text = item.text
                        "upText" -> titleExample.text = item.text
                        "selected" -> cbSource.isChecked = selected.contains(item)
                    }
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemTtsCacheBinding) {
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
            ivMenuMore.setOnClickListener {
                showMenu(ivMenuMore, holder.layoutPosition)
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

    private fun showMenu(view: View, position: Int) {
        val source = getItem(position) ?: return
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.tts_cache_item)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_play -> callBack.play(source)
                R.id.menu_del -> {
                    callBack.del(source)
                    selected.remove(source)
                }
            }
            true
        }
        popupMenu.show()
    }



    private val movedItems = hashSetOf<TTSCache>()

    override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (movedItems.isNotEmpty()) {
            callBack.update(*movedItems.toTypedArray())
            movedItems.clear()
        }
    }

    val dragSelectCallback: DragSelectTouchHelper.Callback =
        object : DragSelectTouchHelper.AdvanceCallback<TTSCache>(Mode.ToggleAndReverse) {
            override fun currentSelectedId(): MutableSet<TTSCache> {
                return selected
            }

            override fun getItemId(position: Int): TTSCache {
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
        fun del(source: TTSCache)
        fun edit(source: TTSCache)
        fun update(vararg source: TTSCache)
        fun play(source: TTSCache)
        fun upCountView()
    }
}
