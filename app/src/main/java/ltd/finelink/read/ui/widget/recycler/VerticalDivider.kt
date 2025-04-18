package ltd.finelink.read.ui.widget.recycler

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import ltd.finelink.read.R

class VerticalDivider(context: Context) : DividerItemDecoration(context, VERTICAL) {

    init {
        ContextCompat.getDrawable(context, R.drawable.ic_divider)?.let {
            this.setDrawable(it)
        }
    }

}