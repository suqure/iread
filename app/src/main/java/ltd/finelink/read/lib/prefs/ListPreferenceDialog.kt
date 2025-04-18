package ltd.finelink.read.lib.prefs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreferenceDialogFragmentCompat
import androidx.preference.PreferenceDialogFragmentCompat
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.lib.theme.filletBackground

class ListPreferenceDialog : ListPreferenceDialogFragmentCompat() {

    companion object {

        fun newInstance(key: String?): ListPreferenceDialog {
            val fragment = ListPreferenceDialog()
            val b = Bundle(1)
            b.putString(PreferenceDialogFragmentCompat.ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(requireContext().filletBackground)
        dialog.window?.decorView?.post {
            (dialog as AlertDialog).run {
                getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(accentColor)
                getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(accentColor)
                getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(accentColor)
            }
        }
        return dialog
    }

}