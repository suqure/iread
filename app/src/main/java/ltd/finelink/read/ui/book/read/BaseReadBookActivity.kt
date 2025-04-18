package ltd.finelink.read.ui.book.read

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.view.isVisible
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseActivity
import ltd.finelink.read.constant.AppConst.charsets
import ltd.finelink.read.constant.PreferKey
import ltd.finelink.read.databinding.ActivityBookReadBinding
import ltd.finelink.read.databinding.DialogDownloadChoiceBinding
import ltd.finelink.read.databinding.DialogEditTextBinding
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.help.config.LocalConfig
import ltd.finelink.read.help.config.ReadBookConfig
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.dialogs.selector
import ltd.finelink.read.lib.theme.ThemeStore
import ltd.finelink.read.lib.theme.backgroundColor
import ltd.finelink.read.lib.theme.bottomBackground
import ltd.finelink.read.model.CacheBook
import ltd.finelink.read.model.ReadBook
import ltd.finelink.read.ui.book.read.config.BgTextConfigDialog
import ltd.finelink.read.ui.book.read.config.ClickActionConfigDialog
import ltd.finelink.read.ui.book.read.config.PaddingConfigDialog
import ltd.finelink.read.ui.book.read.config.PageKeyDialog
import ltd.finelink.read.ui.file.HandleFileContract
import ltd.finelink.read.utils.ColorUtils
import ltd.finelink.read.utils.FileDoc
import ltd.finelink.read.utils.find
import ltd.finelink.read.utils.getPrefString
import ltd.finelink.read.utils.gone
import ltd.finelink.read.utils.isTv
import ltd.finelink.read.utils.navigationBarGravity
import ltd.finelink.read.utils.navigationBarHeight
import ltd.finelink.read.utils.setLightStatusBar
import ltd.finelink.read.utils.setNavigationBarColorAuto
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import ltd.finelink.read.utils.visible

/**
 * 阅读界面
 */
abstract class BaseReadBookActivity :
    VMBaseActivity<ActivityBookReadBinding, ReadBookViewModel>(imageBg = false) {

    override val binding by viewBinding(ActivityBookReadBinding::inflate)
    override val viewModel by viewModels<ReadBookViewModel>()
    var bottomDialog = 0
        set(value) {
            if (field != value) {
                field = value
                onBottomDialogChange()
            }
        }
    private val selectBookFolderResult = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            ReadBook.book?.let { book ->
                FileDoc.fromUri(uri, true).find(book.originName)?.let { doc ->
                    book.bookUrl = doc.uri.toString()
                    book.save()
                    viewModel.loadChapterList(book)
                } ?: ReadBook.upMsg("找不到文件")
            }
        } ?: ReadBook.upMsg("没有权限访问")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ReadBook.msg = null
        setOrientation()
        upLayoutInDisplayCutoutMode()
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.navigationBar.setBackgroundColor(bottomBackground)
        viewModel.permissionDenialLiveData.observe(this) {
            selectBookFolderResult.launch {
                mode = HandleFileContract.DIR_SYS
                title = "选择书籍所在文件夹"
            }
        }
        if (!LocalConfig.readHelpVersionIsLast) {
            if (isTv) {
                showCustomPageKeyConfig()
            } else {
                showClickRegionalConfig()
            }
        }
    }

    private fun onBottomDialogChange() {
        when (bottomDialog) {
            0 -> onMenuHide()
            1 -> onMenuShow()
        }
    }

    open fun onMenuShow() {

    }

    open fun onMenuHide() {

    }

    fun showPaddingConfig() {
        showDialogFragment<PaddingConfigDialog>()
    }

    fun showBgTextConfig() {
        showDialogFragment<BgTextConfigDialog>()
    }

    fun showClickRegionalConfig() {
        showDialogFragment<ClickActionConfigDialog>()
    }

    private fun showCustomPageKeyConfig() {
        PageKeyDialog(this).show()
    }

    /**
     * 屏幕方向
     */
    @SuppressLint("SourceLockedOrientationActivity")
    fun setOrientation() {
        when (AppConfig.screenOrientation) {
            "0" -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            "1" -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "2" -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "3" -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            "4" -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        }
    }

    /**
     * 更新状态栏,导航栏
     */
    fun upSystemUiVisibility(
        isInMultiWindow: Boolean,
        toolBarHide: Boolean = true,
        useBgMeanColor: Boolean = false
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.run {
                if (toolBarHide && ReadBookConfig.hideNavigationBar) {
                    hide(WindowInsets.Type.navigationBars())
                } else {
                    show(WindowInsets.Type.navigationBars())
                }
                if (toolBarHide && ReadBookConfig.hideStatusBar) {
                    hide(WindowInsets.Type.statusBars())
                } else {
                    show(WindowInsets.Type.statusBars())
                }
            }
        }
        upSystemUiVisibilityO(isInMultiWindow, toolBarHide)
        if (toolBarHide) {
            setLightStatusBar(ReadBookConfig.durConfig.curStatusIconDark())
        } else {
            val statusBarColor =
                if (AppConfig.readBarStyleFollowPage
                    && ReadBookConfig.durConfig.curBgType() == 0
                    || useBgMeanColor
                ) {
                    ReadBookConfig.bgMeanColor
                } else {
                    ThemeStore.statusBarColor(this, AppConfig.isTransparentStatusBar)
                }
            setLightStatusBar(ColorUtils.isColorLight(statusBarColor))
        }
    }

    @Suppress("DEPRECATION")
    private fun upSystemUiVisibilityO(
        isInMultiWindow: Boolean,
        toolBarHide: Boolean = true
    ) {
        var flag = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        if (!isInMultiWindow) {
            flag = flag or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        if (ReadBookConfig.hideNavigationBar) {
            flag = flag or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            if (toolBarHide) {
                flag = flag or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            }
        }
        if (ReadBookConfig.hideStatusBar && toolBarHide) {
            flag = flag or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        window.decorView.systemUiVisibility = flag
    }

    override fun upNavigationBarColor() {
        upNavigationBar()
        when {
            binding.readMenu.isVisible -> super.upNavigationBarColor()
            bottomDialog > 0 -> super.upNavigationBarColor()
            !AppConfig.immNavigationBar -> super.upNavigationBarColor()
            else -> setNavigationBarColorAuto(ReadBookConfig.bgMeanColor)
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun upNavigationBar() {
        binding.navigationBar.run {
            if (bottomDialog > 0 || binding.readMenu.isVisible) {
                val navigationBarHeight =
                    if (ReadBookConfig.hideNavigationBar) navigationBarHeight else 0
                when (navigationBarGravity) {
                    Gravity.BOTTOM -> layoutParams =
                        (layoutParams as FrameLayout.LayoutParams).apply {
                            height = navigationBarHeight
                            width = MATCH_PARENT
                            gravity = Gravity.BOTTOM
                        }

                    Gravity.LEFT -> layoutParams =
                        (layoutParams as FrameLayout.LayoutParams).apply {
                            height = MATCH_PARENT
                            width = navigationBarHeight
                            gravity = Gravity.LEFT
                        }

                    Gravity.RIGHT -> layoutParams =
                        (layoutParams as FrameLayout.LayoutParams).apply {
                            height = MATCH_PARENT
                            width = navigationBarHeight
                            gravity = Gravity.RIGHT
                        }
                }
                visible()
            } else {
                gone()
            }
        }
    }

    /**
     * 保持亮屏
     */
    fun keepScreenOn(on: Boolean) {
        val isScreenOn =
            (window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        if (on == isScreenOn) return
        if (on) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * 适配刘海
     */
    private fun upLayoutInDisplayCutoutMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ReadBookConfig.readBodyToLh) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    @SuppressLint("InflateParams", "SetTextI18n")
    fun showDownloadDialog() {
        ReadBook.book?.let { book ->
            alert(titleResource = R.string.offline_cache) {
                val alertBinding = DialogDownloadChoiceBinding.inflate(layoutInflater).apply {
                    root.setBackgroundColor(root.context.backgroundColor)
                    editStart.setText((book.durChapterIndex + 1).toString())
                    editEnd.setText(book.totalChapterNum.toString())
                }
                customView { alertBinding.root }
                yesButton {
                    alertBinding.run {
                        val start = editStart.text!!.toString().let {
                            if (it.isEmpty()) 0 else it.toInt()
                        }
                        val end = editEnd.text!!.toString().let {
                            if (it.isEmpty()) book.totalChapterNum else it.toInt()
                        }
                        CacheBook.start(this@BaseReadBookActivity, book, start - 1, end - 1)
                    }
                }
                noButton()
            }
        }
    }

    fun showCharsetConfig() {
        alert(R.string.set_charset) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "charset"
                editView.setFilterValues(charsets)
                editView.setText(ReadBook.book?.charset)
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    ReadBook.setCharset(it)
                }
            }
            cancelButton()
        }
    }

    fun showPageAnimConfig(success: () -> Unit) {
        val items = arrayListOf<String>()
        items.add(getString(R.string.btn_default_s))
        items.add(getString(R.string.page_anim_cover))
        items.add(getString(R.string.page_anim_slide))
        items.add(getString(R.string.page_anim_simulation))
        items.add(getString(R.string.page_anim_scroll))
        items.add(getString(R.string.page_anim_none))
        selector(R.string.page_anim, items) { _, i ->
            ReadBook.book?.setPageAnim(i - 1)
            success()
        }
    }

    fun isPrevKey(keyCode: Int): Boolean {
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return false
        }
        val prevKeysStr = getPrefString(PreferKey.prevKeys)
        return prevKeysStr?.split(",")?.contains(keyCode.toString()) ?: false
    }

    fun isNextKey(keyCode: Int): Boolean {
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return false
        }
        val nextKeysStr = getPrefString(PreferKey.nextKeys)
        return nextKeysStr?.split(",")?.contains(keyCode.toString()) ?: false
    }
}