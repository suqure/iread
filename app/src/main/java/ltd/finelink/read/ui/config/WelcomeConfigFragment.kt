package ltd.finelink.read.ui.config

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import ltd.finelink.read.R
import ltd.finelink.read.constant.PreferKey
import ltd.finelink.read.lib.dialogs.selector
import ltd.finelink.read.lib.prefs.fragment.PreferenceFragment
import ltd.finelink.read.lib.theme.primaryColor
import ltd.finelink.read.model.BookCover
import ltd.finelink.read.utils.*
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.MD5Utils
import ltd.finelink.read.utils.SelectImageContract
import ltd.finelink.read.utils.externalFiles
import ltd.finelink.read.utils.getPrefString
import ltd.finelink.read.utils.inputStream
import ltd.finelink.read.utils.putPrefString
import ltd.finelink.read.utils.readUri
import ltd.finelink.read.utils.removePref
import ltd.finelink.read.utils.setEdgeEffectColor
import ltd.finelink.read.utils.toastOnUi
import splitties.init.appCtx
import java.io.FileOutputStream

class WelcomeConfigFragment : PreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val requestWelcomeImage = 221
    private val requestWelcomeImageDark = 222
    private val selectImage = registerForActivityResult(SelectImageContract()) {
        it.uri?.let { uri ->
            when (it.requestCode) {
                requestWelcomeImage -> setCoverFromUri(PreferKey.welcomeImage, uri)
                requestWelcomeImageDark -> setCoverFromUri(PreferKey.welcomeImageDark, uri)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_welcome)
        upPreferenceSummary(PreferKey.welcomeImage, getPrefString(PreferKey.welcomeImage))
        upPreferenceSummary(PreferKey.welcomeImageDark, getPrefString(PreferKey.welcomeImageDark))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.welcome_style)
        listView.setEdgeEffectColor(primaryColor)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            PreferKey.welcomeImage,
            PreferKey.welcomeImageDark -> {
                upPreferenceSummary(key, getPrefString(key))
            }
        }
    }

    @SuppressLint("PrivateResource")
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            PreferKey.welcomeImage ->
                if (getPrefString(preference.key).isNullOrEmpty()) {
                    selectImage.launch(requestWelcomeImage)
                } else {
                    context?.selector(
                        items = arrayListOf(
                            getString(R.string.delete),
                            getString(R.string.select_image)
                        )
                    ) { _, i ->
                        if (i == 0) {
                            removePref(preference.key)
                            BookCover.upDefaultCover()
                        } else {
                            selectImage.launch(requestWelcomeImage)
                        }
                    }
                }
            PreferKey.welcomeImageDark ->
                if (getPrefString(preference.key).isNullOrEmpty()) {
                    selectImage.launch(requestWelcomeImageDark)
                } else {
                    context?.selector(
                        items = arrayListOf(
                            getString(R.string.delete),
                            getString(R.string.select_image)
                        )
                    ) { _, i ->
                        if (i == 0) {
                            removePref(preference.key)
                            BookCover.upDefaultCover()
                        } else {
                            selectImage.launch(requestWelcomeImageDark)
                        }
                    }
                }
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun upPreferenceSummary(preferenceKey: String, value: String?) {
        val preference = findPreference<Preference>(preferenceKey) ?: return
        when (preferenceKey) {
            PreferKey.welcomeImage,
            PreferKey.welcomeImageDark -> preference.summary = if (value.isNullOrBlank()) {
                getString(R.string.select_image)
            } else {
                value
            }
            else -> preference.summary = value
        }
    }

    private fun setCoverFromUri(preferenceKey: String, uri: Uri) {
        readUri(uri) { fileDoc, inputStream ->
            kotlin.runCatching {
                var file = requireContext().externalFiles
                val suffix = fileDoc.name.substringAfterLast(".")
                val fileName = uri.inputStream(requireContext()).getOrThrow().use {
                    MD5Utils.md5Encode(it) + ".$suffix"
                }
                file = FileUtils.createFileIfNotExist(file, "covers", fileName)
                FileOutputStream(file).use {
                    inputStream.copyTo(it)
                }
                putPrefString(preferenceKey, file.absolutePath)
            }.onFailure {
                appCtx.toastOnUi(it.localizedMessage)
            }
        }
    }

}