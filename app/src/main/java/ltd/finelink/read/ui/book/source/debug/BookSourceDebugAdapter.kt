package ltd.finelink.read.ui.book.source.debug

import android.content.Context
import android.view.View
import android.view.ViewGroup
import ltd.finelink.read.R
import ltd.finelink.read.base.adapter.ItemViewHolder
import ltd.finelink.read.base.adapter.RecyclerAdapter
import ltd.finelink.read.databinding.ItemLogBinding

class BookSourceDebugAdapter(context: Context) :
    RecyclerAdapter<String, ItemLogBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemLogBinding {
        return ItemLogBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemLogBinding,
        item: String,
        payloads: MutableList<Any>
    ) {
        binding.apply {
            if (textView.getTag(R.id.tag1) == null) {
                val listener = object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        textView.isCursorVisible = false
                        textView.isCursorVisible = true
                    }

                    override fun onViewDetachedFromWindow(v: View) {}
                }
                textView.addOnAttachStateChangeListener(listener)
                textView.setTag(R.id.tag1, listener)
            }
            textView.text = item
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemLogBinding) {
        //nothing
    }
}