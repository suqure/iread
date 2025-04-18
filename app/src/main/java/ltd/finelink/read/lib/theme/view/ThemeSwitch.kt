package ltd.finelink.read.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.utils.applyTint

/**
 * @author Aidan Follestad (afollestad)
 */
class ThemeSwitch(context: Context, attrs: AttributeSet) : SwitchCompat(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }

    }

}
