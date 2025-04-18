package ltd.finelink.read.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.utils.applyTint

class ThemeCheckBox(context: Context, attrs: AttributeSet) : AppCompatCheckBox(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }
    }
}
