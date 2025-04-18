package ltd.finelink.read.ui.widget.text

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import ltd.finelink.read.R
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.utils.getCompatColor

class AccentTextView(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {

    init {
        if (!isInEditMode) {
            setTextColor(context.accentColor)
        } else {
            setTextColor(context.getCompatColor(R.color.accent))
        }
    }

}
