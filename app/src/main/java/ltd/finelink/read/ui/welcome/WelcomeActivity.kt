package ltd.finelink.read.ui.welcome

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.core.view.postDelayed
import ltd.finelink.read.base.BaseActivity
import ltd.finelink.read.constant.PreferKey
import ltd.finelink.read.constant.Theme
import ltd.finelink.read.databinding.ActivityWelcomeBinding
import ltd.finelink.read.help.config.ThemeConfig
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.lib.theme.backgroundColor
import ltd.finelink.read.ui.book.read.ReadBookActivity
import ltd.finelink.read.ui.main.MainActivity
import ltd.finelink.read.utils.*
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import ltd.finelink.read.utils.BitmapUtils
import ltd.finelink.read.utils.fullScreen
import ltd.finelink.read.utils.getPrefBoolean
import ltd.finelink.read.utils.getPrefString
import ltd.finelink.read.utils.setStatusBarColorAuto
import ltd.finelink.read.utils.startActivity
import ltd.finelink.read.utils.windowSize

open class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {

    override val binding by viewBinding(ActivityWelcomeBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.ivBook.setColorFilter(accentColor)
        binding.vwTitleLine.setBackgroundColor(accentColor)
        // 避免从桌面启动程序后，会重新实例化入口类的activity
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
        } else {
            binding.root.postDelayed(600) { startMainActivity() }
        }
    }

    override fun setupSystemBar() {
        fullScreen()
        setStatusBarColorAuto(backgroundColor, true, fullScreen)
        upNavigationBarColor()
    }

    override fun upBackgroundImage() {
        if (getPrefBoolean(PreferKey.customWelcome)) {
            kotlin.runCatching {
                when (ThemeConfig.getTheme()) {
                    Theme.Dark -> getPrefString(PreferKey.welcomeImageDark)?.let { path ->
                        val size = windowManager.windowSize
                        BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels).let {
                            binding.tvLegado.visible(getPrefBoolean(PreferKey.welcomeShowTextDark))
                            binding.ivBook.visible(getPrefBoolean(PreferKey.welcomeShowIconDark))
                            binding.tvGzh.visible(getPrefBoolean(PreferKey.welcomeShowTextDark))
                            window.decorView.background = BitmapDrawable(resources, it)
                            return
                        }
                    }
                    else -> getPrefString(PreferKey.welcomeImage)?.let { path ->
                        val size = windowManager.windowSize
                        BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels).let {
                            binding.tvLegado.visible(getPrefBoolean(PreferKey.welcomeShowText))
                            binding.ivBook.visible(getPrefBoolean(PreferKey.welcomeShowIcon))
                            binding.tvGzh.visible(getPrefBoolean(PreferKey.welcomeShowText))
                            window.decorView.background = BitmapDrawable(resources, it)
                            return
                        }
                    }
                }
            }
        }
        super.upBackgroundImage()
    }

    private fun startMainActivity() {
        startActivity<MainActivity>()
        if (getPrefBoolean(PreferKey.defaultToRead)) {
            startActivity<ReadBookActivity>()
        }
        finish()
    }

}

class Launcher1 : WelcomeActivity()
class Launcher2 : WelcomeActivity()
class Launcher3 : WelcomeActivity()
class Launcher4 : WelcomeActivity()
class Launcher5 : WelcomeActivity()
class Launcher6 : WelcomeActivity()