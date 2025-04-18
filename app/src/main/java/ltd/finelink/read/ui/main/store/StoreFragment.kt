@file:Suppress("DEPRECATION")

package ltd.finelink.read.ui.main.store

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.android.material.tabs.TabLayout
import ltd.finelink.read.R
import ltd.finelink.read.databinding.FragmentStoreBinding
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.ui.main.store.model.ModelsFragment
import ltd.finelink.read.ui.main.store.voice.VoicesFragment
import ltd.finelink.read.utils.setEdgeEffectColor
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import kotlin.collections.set

/**
 * 书架界面
 */
class StoreFragment() : BaseStoreFragment(R.layout.fragment_store),
    TabLayout.OnTabSelectedListener{

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    private val idModel = 0
    private val idVoice = 1

    private val realPositions = arrayOf(idModel, idVoice)
    private val tabCount = 2

    private val binding by viewBinding(FragmentStoreBinding::bind)
    private val adapter by lazy { TabFragmentPageAdapter(childFragmentManager) }
    private val tabLayout: TabLayout by lazy {
        binding.titleBar.findViewById(R.id.tab_layout)
    }

    private val fragmentMap = hashMapOf<Int, Fragment>()


    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        initView()
    }


    private fun initView() {
        binding.viewPagerStore.setEdgeEffectColor(primaryColor)
        tabLayout.isTabIndicatorFullWidth = false
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        tabLayout.setSelectedTabIndicatorColor(requireContext().accentColor)
        tabLayout.setupWithViewPager(binding.viewPagerStore)
        binding.viewPagerStore.offscreenPageLimit = 1
        binding.viewPagerStore.adapter = adapter
    }


    override fun onTabReselected(tab: TabLayout.Tab) {

    }

    override fun onTabUnselected(tab: TabLayout.Tab) = Unit

    override fun onTabSelected(tab: TabLayout.Tab) {
        AppConfig.saveTabPosition = tab.position

    }


    private inner class TabFragmentPageAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getPageTitle(position: Int): CharSequence {
            var name = when (position) {
                idModel -> R.string.tts_model
                idVoice -> R.string.tts_voice
                else -> R.string.empty
            }

            return context?.getString(name)!!
        }

        /**
         * 确定视图位置是否更改时调用
         * @return POSITION_NONE 已更改,刷新视图. POSITION_UNCHANGED 未更改,不刷新视图
         */
        override fun getItemPosition(any: Any): Int {
            return POSITION_UNCHANGED
        }

        override fun getItem(position: Int): Fragment {
            return when (position) {
                idModel -> ModelsFragment(position)
                idVoice -> VoicesFragment(position)
                else -> ModelsFragment(position)
            }
        }

        override fun getCount(): Int {
            return tabCount
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {

            val fragment = super.instantiateItem(container, position) as Fragment
            fragmentMap[realPositions[position]] = fragment
            return fragment
        }

    }
}