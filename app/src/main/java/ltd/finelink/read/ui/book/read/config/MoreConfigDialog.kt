package ltd.finelink.read.ui.book.read.config

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import ltd.finelink.read.R
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.constant.PreferKey
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.help.config.ReadBookConfig
import ltd.finelink.read.lib.prefs.fragment.PreferenceFragment
import ltd.finelink.read.lib.theme.bottomBackground
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.model.ReadBook
import ltd.finelink.read.ui.book.read.ReadBookActivity
import ltd.finelink.read.ui.book.read.page.provider.ChapterProvider
import ltd.finelink.read.ui.widget.number.NumberPickerDialog
import ltd.finelink.read.utils.dpToPx
import ltd.finelink.read.utils.getPrefBoolean
import ltd.finelink.read.utils.postEvent
import ltd.finelink.read.utils.setEdgeEffectColor

class MoreConfigDialog : DialogFragment() {
    private val readPreferTag = "readPreferenceFragment"

    override fun onStart() {
        super.onStart()
        dialog?.window?.run {
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setBackgroundDrawableResource(R.color.background)
            decorView.setPadding(0, 0, 0, 0)
            val attr = attributes
            attr.dimAmount = 0.0f
            attr.gravity = Gravity.BOTTOM
            attributes = attr
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 360.dpToPx())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as ReadBookActivity).bottomDialog++
        val view = LinearLayout(context)
        view.setBackgroundColor(requireContext().bottomBackground)
        view.id = R.id.tag1
        container?.addView(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var preferenceFragment = childFragmentManager.findFragmentByTag(readPreferTag)
        if (preferenceFragment == null) preferenceFragment = ReadPreferenceFragment()
        childFragmentManager.beginTransaction()
            .replace(view.id, preferenceFragment, readPreferTag)
            .commit()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as ReadBookActivity).bottomDialog--
    }

    class ReadPreferenceFragment : PreferenceFragment(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        private val slopSquare by lazy { ViewConfiguration.get(requireContext()).scaledTouchSlop }

        @SuppressLint("RestrictedApi")
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_config_read)
            upPreferenceSummary(PreferKey.pageTouchSlop, slopSquare.toString())
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.setEdgeEffectColor(primaryColor)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager
                .sharedPreferences
                ?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager
                .sharedPreferences
                ?.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
            when (key) {
                PreferKey.readBodyToLh -> activity?.recreate()
                PreferKey.hideStatusBar -> {
                    ReadBookConfig.hideStatusBar = getPrefBoolean(PreferKey.hideStatusBar)
                    postEvent(EventBus.UP_CONFIG, arrayListOf(0))
                }

                PreferKey.hideNavigationBar -> {
                    ReadBookConfig.hideNavigationBar = getPrefBoolean(PreferKey.hideNavigationBar)
                    postEvent(EventBus.UP_CONFIG, arrayListOf(0))
                }

                PreferKey.keepLight -> postEvent(key, true)
                PreferKey.textSelectAble -> postEvent(key, getPrefBoolean(key))
                PreferKey.screenOrientation -> {
                    (activity as? ReadBookActivity)?.setOrientation()
                }

                PreferKey.textFullJustify,
                PreferKey.textBottomJustify,
                PreferKey.useZhLayout -> {
                    postEvent(EventBus.UP_CONFIG, arrayListOf(5))
                }

                PreferKey.showBrightnessView -> {
                    postEvent(PreferKey.showBrightnessView, "")
                }

                PreferKey.expandTextMenu -> {
                    (activity as? ReadBookActivity)?.textActionMenu?.upMenu()
                }

                PreferKey.doublePageHorizontal -> {
                    ChapterProvider.upLayout()
                    ReadBook.loadContent(false)
                }

                PreferKey.showReadTitleAddition,
                PreferKey.readBarStyleFollowPage -> {
                    postEvent(EventBus.UPDATE_READ_ACTION_BAR, true)
                }

                PreferKey.progressBarBehavior -> {
                    postEvent(EventBus.UP_SEEK_BAR, true)
                }

                PreferKey.noAnimScrollPage -> {
                    ReadBook.callBack?.upPageAnim()
                }

                PreferKey.optimizeRender -> {
                    ReadBook.callBack?.upPageAnim(true)
                    ReadBook.loadContent(false)
                }
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                "customPageKey" -> PageKeyDialog(requireContext()).show()
                "clickRegionalConfig" -> {
                    (activity as? ReadBookActivity)?.showClickRegionalConfig()
                }

                PreferKey.pageTouchSlop -> {
                    NumberPickerDialog(requireContext())
                        .setTitle(getString(R.string.page_touch_slop_dialog_title))
                        .setMaxValue(9999)
                        .setMinValue(0)
                        .setValue(AppConfig.pageTouchSlop)
                        .show {
                            AppConfig.pageTouchSlop = it
                            postEvent(EventBus.UP_CONFIG, arrayListOf(4))
                        }
                }
            }
            return super.onPreferenceTreeClick(preference)
        }

        @Suppress("SameParameterValue")
        private fun upPreferenceSummary(preferenceKey: String, value: String?) {
            val preference = findPreference<Preference>(preferenceKey) ?: return
            when (preferenceKey) {
                PreferKey.pageTouchSlop -> preference.summary =
                    getString(R.string.page_touch_slop_summary, value)
            }
        }

    }
}