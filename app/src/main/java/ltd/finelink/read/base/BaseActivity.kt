package ltd.finelink.read.base

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import ltd.finelink.read.R
import ltd.finelink.read.constant.AppConst
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.constant.Theme
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.help.config.ThemeConfig
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.theme.ThemeStore
import ltd.finelink.read.lib.theme.backgroundColor
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.ui.widget.TitleBar
import ltd.finelink.read.utils.ColorUtils
import ltd.finelink.read.utils.applyBackgroundTint
import ltd.finelink.read.utils.applyOpenTint
import ltd.finelink.read.utils.applyTint
import ltd.finelink.read.utils.disableAutoFill
import ltd.finelink.read.utils.fullScreen
import ltd.finelink.read.utils.hideSoftInput
import ltd.finelink.read.utils.setLightStatusBar
import ltd.finelink.read.utils.setNavigationBarColorAuto
import ltd.finelink.read.utils.setStatusBarColorAuto
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.windowSize


abstract class BaseActivity<VB : ViewBinding>(
    val fullScreen: Boolean = true,
    private val theme: Theme = Theme.Auto,
    private val toolBarTheme: Theme = Theme.Auto,
    private val transparent: Boolean = false,
    private val imageBg: Boolean = true
) : AppCompatActivity() {

    protected abstract val binding: VB

    val isInMultiWindow: Boolean
        @SuppressLint("ObsoleteSdkInt")
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                isInMultiWindowMode
            } else {
                false
            }
        }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppContextWrapper.wrap(newBase))
    }

    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        if (AppConst.menuViewNames.contains(name) && parent?.parent is FrameLayout) {
            (parent.parent as View).setBackgroundColor(backgroundColor)
        }
        return super.onCreateView(parent, name, context, attrs)
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.disableAutoFill()
        initTheme()
        super.onCreate(savedInstanceState)
        setupSystemBar()
        setContentView(binding.root)
        upBackgroundImage()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            findViewById<TitleBar>(R.id.title_bar)
                ?.onMultiWindowModeChanged(isInMultiWindowMode, fullScreen)
        }
        observeLiveBus()
        onActivityCreated(savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        findViewById<TitleBar>(R.id.title_bar)
            ?.onMultiWindowModeChanged(isInMultiWindowMode, fullScreen)
        setupSystemBar()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        findViewById<TitleBar>(R.id.title_bar)
            ?.onMultiWindowModeChanged(isInMultiWindow, fullScreen)
        setupSystemBar()
    }

    abstract fun onActivityCreated(savedInstanceState: Bundle?)

    final override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val bool = onCompatCreateOptionsMenu(menu)
        menu.applyTint(this, toolBarTheme)
        return bool
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.applyOpenTint(this)
        return super.onMenuOpened(featureId, menu)
    }

    open fun onCompatCreateOptionsMenu(menu: Menu) = super.onCreateOptionsMenu(menu)

    final override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            supportFinishAfterTransition()
            return true
        }
        return onCompatOptionsItemSelected(item)
    }

    open fun onCompatOptionsItemSelected(item: MenuItem) = super.onOptionsItemSelected(item)

    open fun initTheme() {
        when (theme) {
            Theme.Transparent -> setTheme(R.style.AppTheme_Transparent)
            Theme.Dark -> {
                setTheme(R.style.AppTheme_Dark)
                window.decorView.applyBackgroundTint(backgroundColor)
            }
            Theme.Light -> {
                setTheme(R.style.AppTheme_Light)
                window.decorView.applyBackgroundTint(backgroundColor)
            }
            else -> {
                if (ColorUtils.isColorLight(primaryColor)) {
                    setTheme(R.style.AppTheme_Light)
                } else {
                    setTheme(R.style.AppTheme_Dark)
                }
                window.decorView.applyBackgroundTint(backgroundColor)
            }
        }
    }

    open fun upBackgroundImage() {
        if (imageBg) {
            try {
                ThemeConfig.getBgImage(this, windowManager.windowSize)?.let {
                    window.decorView.background = BitmapDrawable(resources, it)
                }
            } catch (e: OutOfMemoryError) {
                toastOnUi("背景图片太大,内存溢出")
            } catch (e: Exception) {
                AppLog.put("加载背景出错\n${e.localizedMessage}", e)
            }
        }
    }

    open fun setupSystemBar() {
        if (fullScreen && !isInMultiWindow) {
            fullScreen()
        }
        val isTransparentStatusBar = AppConfig.isTransparentStatusBar
        val statusBarColor = ThemeStore.statusBarColor(this, isTransparentStatusBar)
        setStatusBarColorAuto(statusBarColor, isTransparentStatusBar, fullScreen)
        if (toolBarTheme == Theme.Dark) {
            setLightStatusBar(false)
        } else if (toolBarTheme == Theme.Light) {
            setLightStatusBar(true)
        }
        upNavigationBarColor()
    }

    open fun upNavigationBarColor() {
        if (AppConfig.immNavigationBar) {
            setNavigationBarColorAuto(ThemeStore.navigationBarColor(this))
        } else {
            val nbColor = ColorUtils.darkenColor(ThemeStore.navigationBarColor(this))
            setNavigationBarColorAuto(nbColor)
        }
    }

    fun checkDownloadService(success:()-> Unit){
        val state =  packageManager.getApplicationEnabledSetting("com.android.providers.downloads")
        var alert = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            alert = state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                    state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
        }else{
            alert= state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
        }
        if(alert){
            alert(
                titleResource = R.string.draw,
                messageResource = R.string.download_disabled
            ) {
                yesButton{
                    try{
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.setData(Uri.parse("package:com.android.providers.downloads"))
                        startActivity(intent)
                    }catch (ex: ActivityNotFoundException){
                        val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                        startActivity(intent)
                    }
                }
                noButton()
            }
        }else{
            success.invoke()
        }
    }

    open fun observeLiveBus() {
    }

    override fun finish() {
        currentFocus?.hideSoftInput()
        super.finish()
    }
}