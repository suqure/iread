package ltd.finelink.read.lib.prefs

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.EditTextPreference.OnBindEditTextListener
import androidx.preference.PreferenceViewHolder
import ltd.finelink.read.R
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.utils.applyTint

class EditTextPreference(context: Context, attrs: AttributeSet) :
    androidx.preference.EditTextPreference(context, attrs) {

    private var mOnBindEditTextListener: OnBindEditTextListener? = null
    private val onBindEditTextListener = OnBindEditTextListener { editText ->
        editText.applyTint(context.accentColor)
        mOnBindEditTextListener?.onBindEditText(editText)
    }

    init {
        // isPersistent = true
        layoutResource = R.layout.view_preference
        super.setOnBindEditTextListener(onBindEditTextListener)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        Preference.bindView<TextView>(context, holder, icon, title, summary, null, null)
        super.onBindViewHolder(holder)
    }

    override fun setOnBindEditTextListener(onBindEditTextListener: OnBindEditTextListener?) {
        mOnBindEditTextListener = onBindEditTextListener
    }

}
