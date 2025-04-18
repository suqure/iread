package ltd.finelink.read.base

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import ltd.finelink.read.R
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.ui.widget.TitleBar
import ltd.finelink.read.utils.applyTint

@Suppress("MemberVisibilityCanBePrivate")
abstract class BaseFragment(@LayoutRes layoutID: Int) : Fragment(layoutID) {

    var supportToolbar: Toolbar? = null
        private set

    val menuInflater: MenuInflater
        @SuppressLint("RestrictedApi")
        get() = SupportMenuInflater(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onMultiWindowModeChanged()
        observeLiveBus()
        onFragmentCreated(view, savedInstanceState)
    }

    abstract fun onFragmentCreated(view: View, savedInstanceState: Bundle?)

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        super.onMultiWindowModeChanged(isInMultiWindowMode)
        onMultiWindowModeChanged()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onMultiWindowModeChanged()
    }

    private fun onMultiWindowModeChanged() {
        (activity as? BaseActivity<*>)?.let {
            view?.findViewById<TitleBar>(R.id.title_bar)
                ?.onMultiWindowModeChanged(it.isInMultiWindow, it.fullScreen)
        }
    }

    fun setSupportToolbar(toolbar: Toolbar) {
        supportToolbar = toolbar
        supportToolbar?.let {
            it.menu.apply {
                onCompatCreateOptionsMenu(this)
                applyTint(requireContext())
            }

            it.setOnMenuItemClickListener { item ->
                onCompatOptionsItemSelected(item)
                true
            }
        }
    }

    fun checkDownloadService(success:()-> Unit){
        val state =  requireContext().packageManager.getApplicationEnabledSetting("com.android.providers.downloads")
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
                        requireContext().startActivity(intent)
                    }catch (ex: ActivityNotFoundException){
                        val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                        requireContext().startActivity(intent)
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

    open fun onCompatCreateOptionsMenu(menu: Menu) {
    }

    open fun onCompatOptionsItemSelected(item: MenuItem) {
    }

}
