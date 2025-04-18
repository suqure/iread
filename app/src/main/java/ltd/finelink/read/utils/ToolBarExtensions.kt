@file:Suppress("unused")

package ltd.finelink.read.utils

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.widget.Toolbar
import androidx.core.content.ContextCompat
import ltd.finelink.read.R

/**
 * 设置toolBar更多图标颜色
 */
@SuppressLint("ObsoleteSdkInt")
fun Toolbar.setMoreIconColor(color: Int) {
    val moreIcon = ContextCompat.getDrawable(context, R.drawable.ic_more)
    if (moreIcon != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        moreIcon.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        overflowIcon = moreIcon
    }
}