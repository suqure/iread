package ltd.finelink.read.ui.about

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseDialogFragment
import ltd.finelink.read.databinding.DialogUpdateBinding
import ltd.finelink.read.help.AppUpdate
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.model.Download
import ltd.finelink.read.utils.setLayout
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin

class UpdateDialog() : BaseDialogFragment(R.layout.dialog_update) {

    constructor(updateInfo: AppUpdate.UpdateInfo) : this() {
        arguments = Bundle().apply {
            putString("newVersion", updateInfo.tagName)
            putString("updateBody", updateInfo.updateLog)
            putString("url", updateInfo.downloadUrl)
            putString("name", updateInfo.fileName)
        }
    }

    val binding by viewBinding(DialogUpdateBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.title = getString(R.string.update_version,arguments?.getString("newVersion"))
        val updateBody = arguments?.getString("updateBody")
        if (updateBody == null) {
            toastOnUi("没有数据")
            dismiss()
            return
        }
        binding.textView.post {
            Markwon.builder(requireContext())
                .usePlugin(GlideImagesPlugin.create(requireContext()))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(TablePlugin.create(requireContext()))
                .build()
                .setMarkdown(binding.textView, updateBody)
        }
        binding.toolBar.inflateMenu(R.menu.app_update)
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_download -> {
                    val url = arguments?.getString("url")
                    val name = arguments?.getString("name")
                    if (url != null && name != null) {
                        Download.start(requireContext(), url, name)
                        toastOnUi(R.string.download_start)
                    }
                }
            }
            return@setOnMenuItemClickListener true
        }
    }

}