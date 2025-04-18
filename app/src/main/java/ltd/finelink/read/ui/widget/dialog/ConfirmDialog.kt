package ltd.finelink.read.ui.widget.dialog

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseDialogFragment
import ltd.finelink.read.databinding.DialogConfirmViewBinding
import ltd.finelink.read.help.IntentData
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.utils.applyTint
import ltd.finelink.read.utils.setHtml
import ltd.finelink.read.utils.setLayout
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding


class ConfirmDialog() : BaseDialogFragment(R.layout.dialog_confirm_view) {

    enum class Mode {
        MD, HTML, TEXT
    }
    constructor(
        title: String,
        content: String?,
        mode: Mode = Mode.TEXT,
        okText: String? = null,
        cancelText: String? = null
    ) : this() {
        arguments = Bundle().apply {
            putString("title", title)
            putString("content", IntentData.put(content))
            putString("mode", mode.name)
            putString("ok", okText)
            putString("cancel", cancelText)
        }
        isCancelable = false
    }

    private var confirmListener:OnConfirmListener?=null
    private val binding by viewBinding(DialogConfirmViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 0.9f)
    }

    fun setOnConfirmListener(confirmListener: OnConfirmListener?) {
        this.confirmListener = confirmListener
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_close -> dismissAllowingStateLoss()
            }
            true
        }
        arguments?.let {
            it.getString("ok")?.let { text->
                binding.tvOk.text = text
            }
            it.getString("cancel")?.let { text->
                binding.tvCancel.text = text
            }
            binding.toolBar.title = it.getString("title")
            val content = IntentData.get(it.getString("content")) ?: ""
            when (it.getString("mode")) {
                Mode.MD.name -> binding.textView.post {
                    Markwon.builder(requireContext())
                        .usePlugin(GlideImagesPlugin.create(requireContext()))
                        .usePlugin(HtmlPlugin.create())
                        .usePlugin(TablePlugin.create(requireContext()))
                        .build()
                        .setMarkdown(binding.textView, content)
                }

                Mode.HTML.name -> binding.textView.setHtml(content)
                else -> binding.textView.text = content
            }
        }

        binding.tvOk.setOnClickListener {
            confirmListener?.let {
                it.ok()
                dismiss()
            }
        }
        binding.tvCancel.setOnClickListener{
            confirmListener?.let {
                it.cancel()
                dismiss()
            }
        }

    }

    interface OnConfirmListener {
        fun ok()
        fun cancel()
    }



}
