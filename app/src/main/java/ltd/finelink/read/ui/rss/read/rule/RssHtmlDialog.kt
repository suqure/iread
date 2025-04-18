package ltd.finelink.read.ui.rss.read.rule

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseDialogFragment
import ltd.finelink.read.base.adapter.ItemViewHolder
import ltd.finelink.read.base.adapter.RecyclerAdapter
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.databinding.DialogRecyclerViewBinding
import ltd.finelink.read.databinding.ItemRssHtmlBinding
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.ui.widget.dialog.CodeDialog
import ltd.finelink.read.ui.widget.dialog.WaitDialog
import ltd.finelink.read.utils.gone
import ltd.finelink.read.utils.normalText
import ltd.finelink.read.utils.postEvent
import ltd.finelink.read.utils.setLayout
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import ltd.finelink.read.utils.visible
import org.jsoup.nodes.Element

class RssHtmlDialog() : BaseDialogFragment(R.layout.dialog_recycler_view) {

    constructor(rss: String, source: String, finishOnDismiss: Boolean = false) : this() {
        arguments = Bundle().apply {
            putString("source", source)
            putString("rss", rss)
            putBoolean("finishOnDismiss", finishOnDismiss)
        }
    }

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val viewModel by viewModels<RssHtmlViewModel>()
    private val adapter by lazy { SourcesAdapter(requireContext()) }


    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (arguments?.getBoolean("finishOnDismiss") == true) {
            activity?.finish()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.setTitle(R.string.read_aloud_rule)
        binding.rotateLoading.visible()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.tvCancel.visible()
        binding.tvCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.tvOk.visible()
        binding.tvOk.setOnClickListener {
            val waitDialog = WaitDialog(requireContext())
            waitDialog.show()
            viewModel.saveRule {
                postEvent(EventBus.UP_RSS_READ_ALOUD_RULE, "")
                waitDialog.dismiss()
                dismissAllowingStateLoss()
            }
        }
        binding.tvFooterLeft.gone()
        binding.tvFooterLeft.setOnClickListener {
            val selectAll = viewModel.isSelectAll
            viewModel.selectStatus.forEachIndexed { index, b ->
                if (b != !selectAll) {
                    viewModel.selectStatus[index] = !selectAll
                }
            }
            adapter.notifyDataSetChanged()
            upSelectText()
        }
        viewModel.errorLiveData.observe(this) {
            binding.rotateLoading.gone()
            binding.tvMsg.apply {
                text = it
                visible()
            }
        }
        viewModel.successLiveData.observe(this) {
            binding.rotateLoading.gone()
            if (it > 0) {
                adapter.setItems(viewModel.allSources)
                upSelectText()
            } else {
                binding.tvMsg.apply {
                    setText(R.string.wrong_format)
                    visible()
                }
            }
        }
        val source = arguments?.getString("source")
        val rss = arguments?.getString("rss")
        if (source.isNullOrEmpty() || rss.isNullOrEmpty()) {
            dismiss()
            return
        }
        viewModel.initSource(rss, source)
    }

    private fun upSelectText() {
        if (viewModel.isSelectAll) {
            binding.tvFooterLeft.text = getString(
                R.string.select_cancel_count,
                viewModel.selectCount,
                viewModel.allSources.size
            )
        } else {
            binding.tvFooterLeft.text = getString(
                R.string.select_all_count,
                viewModel.selectCount,
                viewModel.allSources.size
            )
        }
    }

    inner class SourcesAdapter(context: Context) :
        RecyclerAdapter<Element, ItemRssHtmlBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemRssHtmlBinding {
            return ItemRssHtmlBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemRssHtmlBinding,
            item: Element,
            payloads: MutableList<Any>
        ) {
            binding.apply {
                if (viewModel.acceptTags.contains(item.tagName()) || viewModel.ignoreTags.contains(
                        item.tagName()
                    )
                ) {
                    cbSourceName.isChecked = true
                }
                if (item.id()
                        .isNotEmpty() && (viewModel.acceptIds.contains(item.id()) || viewModel.ignoreIds.contains(
                        item.id()
                    ))
                ) {
                    cbSourceId.isChecked = true
                }
                if (item.className()
                        .isNotEmpty() && (viewModel.acceptClass.contains(item.className()) || viewModel.ignoreClass.contains(
                        item.className()
                    ))
                ) {
                    cbSourceClass.isChecked = true
                }
                if (viewModel.ignoreTags.contains(item.tagName())
                    || (viewModel.ignoreIds.contains(item.id()) && item.id().isNotEmpty())
                    || (viewModel.ignoreClass.contains(item.className()) && item.className()
                        .isNotEmpty())
                ) {
                    swIgnoreMode.isChecked = true
                }

                cbSourceName.text = "tag:${item.nodeName()}"
                cbSourceId.text = "id:${item.id()}"
                cbSourceClass.text = "class:${item.className()}"
                tvSourceText.text = item.normalText()

            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemRssHtmlBinding) {
            binding.apply {

                cbSourceName.setOnCheckedChangeListener { buttonView, isChecked ->
                    val source = viewModel.allSources[holder.layoutPosition]
                    if (buttonView.isPressed) {
                        viewModel.selectStatus[holder.layoutPosition] = isChecked
                        upSelectText()
                    }
                    if (isChecked) {
                        if (swIgnoreMode.isChecked) {
                            viewModel.ignoreTags.add(source.tagName())
                        } else {
                            viewModel.acceptTags.add(source.tagName())
                        }
                    } else {
                        if (swIgnoreMode.isChecked) {
                            viewModel.ignoreTags.remove(source.tagName())
                        } else {
                            viewModel.acceptTags.remove(source.tagName())

                        }
                    }

                }
                cbSourceId.setOnCheckedChangeListener { buttonView, isChecked ->
                    val source = viewModel.allSources[holder.layoutPosition]
                    if (buttonView.isPressed) {
                        viewModel.selectStatus[holder.layoutPosition] = isChecked
                        upSelectText()
                    }
                    if (source.id().isNotEmpty()) {


                        if (isChecked) {
                            if (swIgnoreMode.isChecked) {
                                viewModel.ignoreIds.add(source.id())
                            } else {
                                viewModel.acceptIds.add(source.id())

                            }
                        } else {
                            if (swIgnoreMode.isChecked) {
                                viewModel.ignoreIds.remove(source.id())
                            } else {
                                viewModel.acceptIds.remove(source.id())

                            }
                        }
                    }

                }
                cbSourceClass.setOnCheckedChangeListener { buttonView, isChecked ->
                    val source = viewModel.allSources[holder.layoutPosition]
                    if (buttonView.isPressed) {
                        viewModel.selectStatus[holder.layoutPosition] = isChecked
                        upSelectText()
                    }
                    if (source.className().isNotEmpty()) {


                        if (isChecked) {
                            if (swIgnoreMode.isChecked) {
                                viewModel.ignoreClass.add(source.className())
                            } else {
                                viewModel.acceptClass.add(source.className())
                            }
                        } else {
                            if (swIgnoreMode.isChecked) {
                                viewModel.ignoreClass.remove(source.className())
                            } else {
                                viewModel.acceptClass.remove(source.className())

                            }
                        }
                    }
                }
                swIgnoreMode.setOnClickListener {
                    val source = viewModel.allSources[holder.layoutPosition]
                    if (swIgnoreMode.isChecked) {
                        if (cbSourceName.isChecked) {
                            viewModel.acceptTags.remove(source.tagName())
                            viewModel.ignoreTags.add(source.tagName())
                        }
                        if (source.id().isNotEmpty()) {
                            if (cbSourceId.isChecked) {
                                viewModel.acceptIds.remove(source.id())
                                viewModel.ignoreIds.add(source.id())
                            }
                        }
                        if (source.className().isNotEmpty()) {
                            if (cbSourceClass.isChecked) {
                                viewModel.acceptClass.remove(source.className())
                                viewModel.ignoreClass.add(source.className())
                            }
                        }

                    } else {
                        if (cbSourceName.isChecked) {
                            viewModel.acceptTags.add(source.tagName())
                            viewModel.ignoreTags.remove(source.tagName())
                        }
                        if (source.id().isNotEmpty()) {
                            if (cbSourceId.isChecked) {
                                viewModel.acceptIds.add(source.id())
                                viewModel.ignoreIds.remove(source.id())
                            }
                        }
                        if (source.className().isNotEmpty()) {
                            if (cbSourceClass.isChecked) {
                                viewModel.acceptClass.add(source.className())
                                viewModel.ignoreClass.remove(source.className())
                            }
                        }
                    }
                }
                tvOpen.setOnClickListener {
                    val source = viewModel.allSources[holder.layoutPosition]
                    showDialogFragment(
                        CodeDialog(
                            source.normalText(),
                            disableEdit = false,
                            requestId = holder.layoutPosition.toString()
                        )
                    )
                }
            }
        }

    }


}