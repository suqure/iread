package ltd.finelink.read.ui.main.my

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.preference.Preference
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseFragment
import ltd.finelink.read.constant.PreferKey
import ltd.finelink.read.databinding.FragmentMyConfigBinding
import ltd.finelink.read.help.config.ThemeConfig
import ltd.finelink.read.lib.prefs.NameListPreference
import ltd.finelink.read.lib.prefs.fragment.PreferenceFragment
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.ui.about.AboutActivity
import ltd.finelink.read.ui.about.ReadRecordActivity
import ltd.finelink.read.ui.book.bookmark.AllBookmarkActivity
import ltd.finelink.read.ui.book.source.manage.BookSourceActivity
import ltd.finelink.read.ui.book.toc.rule.TxtTocRuleActivity
import ltd.finelink.read.ui.config.ConfigActivity
import ltd.finelink.read.ui.config.ConfigTag
import ltd.finelink.read.ui.dict.rule.DictRuleActivity
import ltd.finelink.read.ui.file.FileManageActivity
import ltd.finelink.read.ui.main.MainFragmentInterface
import ltd.finelink.read.ui.replace.ReplaceRuleActivity
import ltd.finelink.read.ui.widget.dialog.TextDialog
import ltd.finelink.read.utils.LogUtils
import ltd.finelink.read.utils.setEdgeEffectColor
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.startActivity
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding

class MyFragment() : BaseFragment(R.layout.fragment_my_config), MainFragmentInterface {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    private val binding by viewBinding(FragmentMyConfigBinding::bind)

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        val fragmentTag = "prefFragment"
        var preferenceFragment = childFragmentManager.findFragmentByTag(fragmentTag)
        if (preferenceFragment == null) preferenceFragment = MyPreferenceFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.pre_fragment, preferenceFragment, fragmentTag).commit()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_my, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_help -> {
                val text = String(requireContext().assets.open(getString(R.string.help_app_file)).readBytes())
                showDialogFragment(TextDialog(getString(R.string.help), text, TextDialog.Mode.MD))
            }
        }
    }

    /**
     * 配置
     */
    class MyPreferenceFragment : PreferenceFragment(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_main)

            findPreference<NameListPreference>(PreferKey.themeMode)?.let {
                it.setOnPreferenceChangeListener { _, _ ->
                    view?.post { ThemeConfig.applyDayNight(requireContext()) }
                    true
                }
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.setEdgeEffectColor(primaryColor)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
            when (key) {
                "recordLog" -> LogUtils.upLevel()
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                "bookSourceManage" -> startActivity<BookSourceActivity>()
                "replaceManage" -> startActivity<ReplaceRuleActivity>()
                "dictRuleManage" -> startActivity<DictRuleActivity>()
                "txtTocRuleManage" -> startActivity<TxtTocRuleActivity>()
                "bookmark" -> startActivity<AllBookmarkActivity>()
                "setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.OTHER_CONFIG)
                }

                "web_dav_setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.BACKUP_CONFIG)
                }

                "theme_setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.THEME_CONFIG)
                }

                "fileManage" -> startActivity<FileManageActivity>()
                "readRecord" -> startActivity<ReadRecordActivity>()
                "about" -> startActivity<AboutActivity>()
                "exit" -> activity?.finish()
            }
            return super.onPreferenceTreeClick(preference)
        }
    }
}