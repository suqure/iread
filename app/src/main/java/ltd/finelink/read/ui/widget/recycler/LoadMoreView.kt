package ltd.finelink.read.ui.widget.recycler

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import ltd.finelink.read.R
import ltd.finelink.read.databinding.ViewLoadMoreBinding
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.utils.invisible
import ltd.finelink.read.utils.visible

@Suppress("unused")
class LoadMoreView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val binding = ViewLoadMoreBinding.inflate(LayoutInflater.from(context), this)
    private var errorMsg = ""

    private var onClickListener: OnClickListener? = null

    var isLoading = false
        private set

    var hasMore = true
        private set

    init {
        super.setOnClickListener {
            if (!showErrorDialog()) {
                onClickListener?.onClick(it)
            }
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        this.onClickListener = l
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
    }

    fun startLoad() {
        isLoading = true
        binding.tvText.invisible()
        binding.rotateLoading.visible()
    }

    fun stopLoad() {
        isLoading = false
        binding.rotateLoading.inVisible()
    }

    fun hasMore() {
        errorMsg = ""
        hasMore = true
        startLoad()
    }

    fun noMore(msg: String? = null) {
        stopLoad()
        errorMsg = ""
        hasMore = false
        if (msg != null) {
            binding.tvText.text = msg
        } else {
            binding.tvText.setText(R.string.bottom_line)
        }
        binding.tvText.visible()
    }

    fun error(msg: String) {
        stopLoad()
        hasMore = false
        errorMsg = msg
        binding.tvText.text = context.getString(R.string.error_load_msg, "点击查看详情")
        binding.tvText.visible()
    }

    private fun showErrorDialog(): Boolean {
        if (errorMsg.isBlank()) {
            return false
        }
        context.alert(R.string.error) {
            setMessage(errorMsg)
        }
        return true
    }

}
