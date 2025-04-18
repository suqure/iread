package ltd.finelink.read.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.utils.applyTint

class ThemeRadioButton(context: Context, attrs: AttributeSet) :
    AppCompatRadioButton(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }
    }
}
