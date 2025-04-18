package ltd.finelink.read.ui.book.read.config

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import ltd.finelink.read.R
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.ui.widget.text.StrokeTextView


class ChineseConverter(context: Context, attrs: AttributeSet?) : StrokeTextView(context, attrs) {

    private val spannableString = SpannableString("简/繁")
    private var enabledSpan: ForegroundColorSpan = ForegroundColorSpan(context.accentColor)
    private var onChanged: (() -> Unit)? = null

    init {
        text = spannableString
        if (!isInEditMode) {
            upUi(AppConfig.chineseConverterType)
        }
        setOnClickListener {
            selectType()
        }
    }

    private fun upUi(type: Int) {
        spannableString.removeSpan(enabledSpan)
        when (type) {
            1 -> spannableString.setSpan(enabledSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            2 -> spannableString.setSpan(enabledSpan, 2, 3, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
        text = spannableString
    }

    private fun selectType() {
        context.alert(titleResource = R.string.chinese_converter) {
            items(context.resources.getStringArray(R.array.chinese_mode).toList()) { _, i ->
                AppConfig.chineseConverterType = i
                upUi(i)
                onChanged?.invoke()
            }
        }
    }

    fun onChanged(unit: () -> Unit) {
        onChanged = unit
    }
}