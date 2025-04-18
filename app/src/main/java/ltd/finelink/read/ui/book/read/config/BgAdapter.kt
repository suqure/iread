package ltd.finelink.read.ui.book.read.config

import android.content.Context
import android.view.ViewGroup
import ltd.finelink.read.base.adapter.ItemViewHolder
import ltd.finelink.read.base.adapter.RecyclerAdapter
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.databinding.ItemBgImageBinding
import ltd.finelink.read.help.config.ReadBookConfig
import ltd.finelink.read.help.glide.ImageLoader
import ltd.finelink.read.utils.postEvent
import java.io.File

class BgAdapter(context: Context, val textColor: Int) :
    RecyclerAdapter<String, ItemBgImageBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemBgImageBinding {
        return ItemBgImageBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBgImageBinding,
        item: String,
        payloads: MutableList<Any>
    ) {
        binding.run {
            ImageLoader.load(
                context,
                context.assets.open("bg${File.separator}$item").readBytes()
            )
                .centerCrop()
                .into(ivBg)
            tvName.setTextColor(textColor)
            tvName.text = item.substringBeforeLast(".")
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBgImageBinding) {
        holder.itemView.apply {
            this.setOnClickListener {
                getItemByLayoutPosition(holder.layoutPosition)?.let {
                    ReadBookConfig.durConfig.setCurBg(1, it)
                    postEvent(EventBus.UP_CONFIG, arrayListOf(1))
                }
            }
        }
    }
}