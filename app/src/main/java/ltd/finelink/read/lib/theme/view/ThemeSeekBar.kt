package ltd.finelink.read.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.utils.applyTint

/**
 * @author Aidan Follestad (afollestad)
 */
class ThemeSeekBar(context: Context, attrs: AttributeSet) : AppCompatSeekBar(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }
    }
}
