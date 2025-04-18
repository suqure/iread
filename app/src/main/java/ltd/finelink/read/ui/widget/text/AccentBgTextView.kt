package ltd.finelink.read.ui.widget.text

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import ltd.finelink.read.R
import ltd.finelink.read.lib.theme.Selector
import ltd.finelink.read.lib.theme.ThemeStore
import ltd.finelink.read.utils.ColorUtils
import ltd.finelink.read.utils.dpToPx
import ltd.finelink.read.utils.getCompatColor

class AccentBgTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    private var radius = 0

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AccentBgTextView)
        radius = typedArray.getDimensionPixelOffset(R.styleable.AccentBgTextView_radius, radius)
        typedArray.recycle()
        upBackground()
    }

    fun setRadius(radius: Int) {
        this.radius = radius.dpToPx()
        upBackground()
    }

    private fun upBackground() {
        val accentColor = if (isInEditMode) {
            context.getCompatColor(R.color.accent)
        } else {
            ThemeStore.accentColor(context)
        }
        background = Selector.shapeBuild()
            .setCornerRadius(radius)
            .setDefaultBgColor(accentColor)
            .setPressedBgColor(ColorUtils.darkenColor(accentColor))
            .create()
        setTextColor(
            if (ColorUtils.isColorLight(accentColor)) {
                Color.BLACK
            } else {
                Color.WHITE
            }
        )
    }
}
