package ltd.finelink.read.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ProgressBar
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.utils.applyTint

class ThemeProgressBar(context: Context, attrs: AttributeSet) : ProgressBar(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }
    }
}