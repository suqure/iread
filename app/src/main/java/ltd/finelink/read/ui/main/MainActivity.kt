@file:Suppress("DEPRECATION")

package ltd.finelink.read.ui.main

import android.os.Bundle
import android.text.format.DateUtils
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.finelink.read.BuildConfig
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseActivity
import ltd.finelink.read.constant.AppConst.appInfo
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.constant.PreferKey
import ltd.finelink.read.databinding.ActivityMainBinding
import ltd.finelink.read.help.AppUpdate
import ltd.finelink.read.help.AppWebDav
import ltd.finelink.read.help.book.BookHelp
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.help.config.LocalConfig
import ltd.finelink.read.help.coroutine.Coroutine
import ltd.finelink.read.help.storage.Backup
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.theme.elevation
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.service.BaseReadAloudService
import ltd.finelink.read.ui.about.CrashLogsDialog
import ltd.finelink.read.ui.about.UpdateDialog
import ltd.finelink.read.ui.main.bookshelf.BaseBookshelfFragment
import ltd.finelink.read.ui.main.bookshelf.style1.BookshelfFragment1
import ltd.finelink.read.ui.main.bookshelf.style2.BookshelfFragment2
import ltd.finelink.read.ui.main.explore.ExploreFragment
import ltd.finelink.read.ui.main.my.MyFragment
import ltd.finelink.read.ui.main.rss.RssFragment
import ltd.finelink.read.ui.main.store.StoreFragment
import ltd.finelink.read.ui.widget.dialog.ConfirmDialog
import ltd.finelink.read.ui.widget.dialog.TextDialog
import ltd.finelink.read.utils.hideSoftInput
import ltd.finelink.read.utils.observeEvent
import ltd.finelink.read.utils.setEdgeEffectColor
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import kotlin.collections.set
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 主界面
 */
class MainActivity : VMBaseActivity<ActivityMainBinding, MainViewModel>(),
    BottomNavigationView.OnNavigationItemSelectedListener,
    BottomNavigationView.OnNavigationItemReselectedListener {

    override val binding by viewBinding(ActivityMainBinding::inflate)
    override val viewModel by viewModels<MainViewModel>()
    private val idBookshelf = 0
    private val idBookshelf1 = 11
    private val idBookshelf2 = 12
    private val idStore = 1
    private val idExplore = 2
    private val idRss = 3
    private val idMy = 4
    private var exitTime: Long = 0
    private var bookshelfReselected: Long = 0
    private var exploreReselected: Long = 0
    private var pagePosition = 0
    private val fragmentMap = hashMapOf<Int, Fragment>()
    private var bottomMenuCount = 5
    private val realPositions = arrayOf(idBookshelf, idStore, idExplore, idRss, idMy)
    private val adapter by lazy {
        TabFragmentPageAdapter(supportFragmentManager)
    }
    private val onUpBooksBadgeView by lazy {
        binding.bottomNavigationView.addBadgeView(0)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        upBottomMenu()
        binding.run {
            viewPagerMain.setEdgeEffectColor(primaryColor)
            viewPagerMain.offscreenPageLimit = 3
            viewPagerMain.adapter = adapter
            viewPagerMain.addOnPageChangeListener(PageChangeCallback())
            bottomNavigationView.elevation = elevation
            bottomNavigationView.setOnNavigationItemSelectedListener(this@MainActivity)
            bottomNavigationView.setOnNavigationItemReselectedListener(this@MainActivity)
        }
        upHomePage()
        onBackPressedDispatcher.addCallback(this) {
            if (pagePosition != 0) {
                binding.viewPagerMain.currentItem = 0
                return@addCallback
            }
            (fragmentMap[getFragmentId(0)] as? BookshelfFragment2)?.let {
                if (it.back()) {
                    return@addCallback
                }
            }
            if (System.currentTimeMillis() - exitTime > 2000) {
                toastOnUi(R.string.double_click_exit)
                exitTime = System.currentTimeMillis()
            } else {
                if (BaseReadAloudService.pause) {
                    finish()
                } else {
                    moveTaskToBack(true)
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            currentFocus?.let {
                if (it is EditText) {
                    it.clearFocus()
                    it.hideSoftInput()
                }
            }
        }
        return try {
            super.dispatchTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            false
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        lifecycleScope.launch {
            //隐私协议
            if (!showPrivacy()) return@launch
            //版本更新
            upVersion()
            notifyAppCrash()
            //备份同步
            backupSync()
            //自动更新书籍
            val isAutoRefreshedBook = savedInstanceState?.getBoolean("isAutoRefreshedBook") ?: false
            if (AppConfig.autoRefreshBook && !isAutoRefreshedBook) {
                binding.viewPagerMain.postDelayed(1000) {
                    viewModel.upAllBookToc()
                }
            }
            binding.viewPagerMain.postDelayed(3000) {
                viewModel.postLoad()
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean = binding.run {
        when (item.itemId) {
            R.id.menu_bookshelf ->
                viewPagerMain.setCurrentItem(0, false)

            R.id.menu_store ->
                viewPagerMain.setCurrentItem(realPositions.indexOf(idStore), false)

            R.id.menu_discovery ->
                viewPagerMain.setCurrentItem(realPositions.indexOf(idExplore), false)

            R.id.menu_rss ->
                viewPagerMain.setCurrentItem(realPositions.indexOf(idRss), false)

            R.id.menu_my_config ->
                viewPagerMain.setCurrentItem(realPositions.indexOf(idMy), false)
        }
        return false
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_bookshelf -> {
                if (System.currentTimeMillis() - bookshelfReselected > 300) {
                    bookshelfReselected = System.currentTimeMillis()
                } else {
                    (fragmentMap[getFragmentId(0)] as? BaseBookshelfFragment)?.gotoTop()
                }
            }

            R.id.menu_discovery -> {
                if (System.currentTimeMillis() - exploreReselected > 300) {
                    exploreReselected = System.currentTimeMillis()
                } else {
                    (fragmentMap[1] as? ExploreFragment)?.compressExplore()
                }
            }
        }
    }


    /**
     * 用户隐私与协议
     */
    private suspend fun showPrivacy() = suspendCoroutine { block ->
        if (LocalConfig.privacyPolicyOk) {
            block.resume(true)
            return@suspendCoroutine
        }
        val privacyPolicy = String(assets.open(getString(R.string.privacy_policy_file)).readBytes())
        val dialog = ConfirmDialog(
            getString(R.string.privacy_policy),
            privacyPolicy,
            ConfirmDialog.Mode.MD,
            getString(R.string.agree),
            getString(R.string.refuse))
        val listener = object:ConfirmDialog.OnConfirmListener{
            override fun ok(){
                LocalConfig.privacyPolicyOk = true
                block.resume(true)
            }
            override fun cancel() {
                finish()
                block.resume(false)
            }
        }
        dialog.setOnConfirmListener(listener)
        showDialogFragment(dialog)
    }

    /**
     * 版本更新日志
     */
    private suspend fun upVersion() = suspendCoroutine { block ->
        if (LocalConfig.versionCode == appInfo.versionCode) {
            block.resume(null)
            return@suspendCoroutine
        }
        LocalConfig.versionCode = appInfo.versionCode
         if (!BuildConfig.DEBUG) {
            val log = String(assets.open("updateLog.md").readBytes())
            val dialog = TextDialog(getString(R.string.update_log), log, TextDialog.Mode.MD)
            dialog.setOnDismissListener {
                block.resume(null)
            }
            showDialogFragment(dialog)
        } else {
            block.resume(null)
        }
    }




    private fun notifyAppCrash() {
        if (!LocalConfig.appCrash || BuildConfig.DEBUG) {
            return
        }
        LocalConfig.appCrash = false
        alert(getString(R.string.draw), "检测到阅读发生了崩溃，是否打开崩溃日志以便报告问题？") {
            yesButton {
                showDialogFragment<CrashLogsDialog>()
            }
            noButton()
        }
    }

    /**
     * 备份同步
     */
    private fun backupSync() {
        lifecycleScope.launch {
            val lastBackupFile =
                withContext(IO) { AppWebDav.lastBackUp().getOrNull() } ?: return@launch
            if (lastBackupFile.lastModify - LocalConfig.lastBackup > DateUtils.MINUTE_IN_MILLIS) {
                LocalConfig.lastBackup = lastBackupFile.lastModify
                alert(
                    R.string.restore,
                    R.string.webdav_after_local_restore_confirm
                ) {
                    cancelButton()
                    okButton {
                        viewModel.restoreWebDav(lastBackupFile.displayName)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (AppConfig.autoRefreshBook) {
            outState.putBoolean("isAutoRefreshedBook", true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Coroutine.async {
            BookHelp.clearInvalidCache()
        }
        if (!BuildConfig.DEBUG) {
            Backup.autoBack(this)
        }
    }

    /**
     * 如果重启太快fragment不会重建,这里更新一下书架的排序
     */
    override fun recreate() {
        if (bottomMenuCount < 5) {
            (fragmentMap[getFragmentId(0)] as? BaseBookshelfFragment)?.run {
                upSort()
            }
        }
        super.recreate()
    }

    override fun observeLiveBus() {
        viewModel.onUpBooksLiveData.observe(this) {
            onUpBooksBadgeView.setBadgeCount(it)
        }
        observeEvent<String>(EventBus.RECREATE) {
            recreate()
        }
        observeEvent<Boolean>(EventBus.NOTIFY_MAIN) {
            binding.apply {
                upBottomMenu()
                if (it) {
                    viewPagerMain.setCurrentItem(bottomMenuCount - 1, false)
                }
            }
        }
        observeEvent<String>(PreferKey.threadCount) {
            viewModel.upPool()
        }
    }

    private fun upBottomMenu() {
        val showDiscovery = AppConfig.showDiscovery
        val showRss = AppConfig.showRSS
        val showVoiceStore = AppConfig.showVoiceStore
        binding.bottomNavigationView.menu.let { menu ->
            menu.findItem(R.id.menu_store).isVisible = showVoiceStore
            menu.findItem(R.id.menu_discovery).isVisible = showDiscovery
            menu.findItem(R.id.menu_rss).isVisible = showRss
        }
        var index = 0
        if (showVoiceStore) {
            index++
            realPositions[index] = idStore
        }
        if (showDiscovery) {
            index++
            realPositions[index] = idExplore
        }
        if (showRss) {
            index++
            realPositions[index] = idRss
        }
        index++
        realPositions[index] = idMy
        bottomMenuCount = index + 1
        adapter.notifyDataSetChanged()
    }

    private fun upHomePage() {
        when (AppConfig.defaultHomePage) {
            "bookshelf" -> {}
            "store" -> if (AppConfig.showVoiceStore) {
                binding.viewPagerMain.setCurrentItem(realPositions.indexOf(idStore), false)
            }

            "explore" -> if (AppConfig.showDiscovery) {
                binding.viewPagerMain.setCurrentItem(realPositions.indexOf(idExplore), false)
            }

            "rss" -> if (AppConfig.showRSS) {
                binding.viewPagerMain.setCurrentItem(realPositions.indexOf(idRss), false)
            }

            "my" -> binding.viewPagerMain.setCurrentItem(realPositions.indexOf(idMy), false)
        }
    }

    private fun getFragmentId(position: Int): Int {
        val id = realPositions[position]
        if (id == idBookshelf) {
            return if (AppConfig.bookGroupStyle == 1) idBookshelf2 else idBookshelf1
        }
        return id
    }

    private inner class PageChangeCallback : ViewPager.SimpleOnPageChangeListener() {

        override fun onPageSelected(position: Int) {
            pagePosition = position
            binding.bottomNavigationView.menu
                .getItem(realPositions[position]).isChecked = true
        }

    }

    @Suppress("DEPRECATION")
    private inner class TabFragmentPageAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private fun getId(position: Int): Int {
            return getFragmentId(position)
        }

        override fun getItemPosition(any: Any): Int {
            val position = (any as MainFragmentInterface).position
                ?: return POSITION_NONE
            val fragmentId = getId(position)
            if ((fragmentId == idBookshelf1 && any is BookshelfFragment1)
                || (fragmentId == idBookshelf2 && any is BookshelfFragment2)
                || (fragmentId == idStore && any is StoreFragment)
                || (fragmentId == idExplore && any is ExploreFragment)
                || (fragmentId == idRss && any is RssFragment)
                || (fragmentId == idMy && any is MyFragment)
            ) {
                return POSITION_UNCHANGED
            }
            return POSITION_NONE
        }

        override fun getItem(position: Int): Fragment {
            return when (getId(position)) {
                idBookshelf1 -> BookshelfFragment1(position)
                idBookshelf2 -> BookshelfFragment2(position)
                idStore -> StoreFragment(position)
                idExplore -> ExploreFragment(position)
                idRss -> RssFragment(position)
                else -> MyFragment(position)
            }
        }

        override fun getCount(): Int {
            return bottomMenuCount
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as Fragment
            fragmentMap[getId(position)] = fragment
            return fragment
        }

    }

}