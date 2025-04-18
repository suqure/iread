package ltd.finelink.read.ui.rss.read

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.view.size
import androidx.lifecycle.lifecycleScope
import com.script.rhino.RhinoScriptEngine
import kotlinx.coroutines.launch
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseActivity
import ltd.finelink.read.constant.AppConst
import ltd.finelink.read.constant.AppConst.imagePathKey
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.RssSource
import ltd.finelink.read.databinding.ActivityRssReadBinding
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.lib.dialogs.SelectItem
import ltd.finelink.read.lib.dialogs.selector
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.lib.theme.primaryTextColor
import ltd.finelink.read.model.Download
import ltd.finelink.read.ui.association.OnLineImportActivity
import ltd.finelink.read.ui.file.HandleFileContract
import ltd.finelink.read.ui.login.SourceLoginActivity
import ltd.finelink.read.ui.rss.read.rule.RssHtmlDialog
import ltd.finelink.read.utils.ACache
import ltd.finelink.read.utils.NetworkUtils
import ltd.finelink.read.utils.get
import ltd.finelink.read.utils.gone
import ltd.finelink.read.utils.invisible
import ltd.finelink.read.utils.isTrue
import ltd.finelink.read.utils.longSnackbar
import ltd.finelink.read.utils.observeEvent
import ltd.finelink.read.utils.openUrl
import ltd.finelink.read.utils.setDarkeningAllowed
import ltd.finelink.read.utils.setTintMutate
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.splitNotBlank
import ltd.finelink.read.utils.startActivity
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import ltd.finelink.read.utils.visible
import org.apache.commons.text.StringEscapeUtils
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import java.util.regex.PatternSyntaxException

/**
 * rss阅读界面
 */
class ReadRssActivity : VMBaseActivity<ActivityRssReadBinding, ReadRssViewModel>(false) {

    override val binding by viewBinding(ActivityRssReadBinding::inflate)
    override val viewModel by viewModels<ReadRssViewModel>()

    private var starMenuItem: MenuItem? = null
    private var ttsMenuItem: MenuItem? = null
    private var customWebViewCallback: WebChromeClient.CustomViewCallback? = null
    private val selectImageDir = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            ACache.get().put(imagePathKey, uri.toString())
            viewModel.saveImage(it.value, uri)
        }
    }
    private val rssJsExtensions by lazy { RssJsExtensions(this) }

    fun getSource(): RssSource? {
        return viewModel.rssSource
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.upStarMenuData.observe(this) { upStarMenu() }
        viewModel.upTtsMenuData.observe(this) { upTtsMenu(it) }
        binding.titleBar.title = intent.getStringExtra("title")
        initWebView()
        initLiveData()
        viewModel.initData(intent)
        onBackPressedDispatcher.addCallback(this) {
            if (binding.customWebView.size > 0) {
                customWebViewCallback?.onCustomViewHidden()
                return@addCallback
            } else if (binding.webView.canGoBack()
                && binding.webView.copyBackForwardList().size > 1
            ) {
                binding.webView.goBack()
                return@addCallback
            }
            finish()
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SwitchIntDef")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }

            Configuration.ORIENTATION_PORTRAIT -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            }
        }
    }
    override fun observeLiveBus() {
        observeEvent<String>(EventBus.UP_RSS_READ_ALOUD_RULE) {
            viewModel.rssSource?.let {
                appDb.rssReadAloudRuleDao.get(it.sourceUrl)?.let { rule->
                    viewModel.rssReadAloudRule = rule
                }
            }

        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_read, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        starMenuItem = menu.findItem(R.id.menu_rss_star)
        ttsMenuItem = menu.findItem(R.id.menu_aloud)
        upStarMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_login)?.isVisible = !viewModel.rssSource?.loginUrl.isNullOrBlank()
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_rss_refresh -> viewModel.refresh {
                binding.webView.reload()
            }
            R.id.menu_aloud_rule->{
                binding.webView.settings.javaScriptEnabled = true
                binding.webView.evaluateJavascript("document.documentElement.outerHTML") {
                    val html = StringEscapeUtils.unescapeJson(it)
                        .replace("^\"|\"$".toRegex(), "")
                    viewModel.rssSource?.let {source->
                        showDialogFragment(RssHtmlDialog(source.sourceUrl,html))
                    }

                }
            }
            R.id.menu_rss_star -> viewModel.favorite()

            R.id.menu_aloud -> readAloud()
            R.id.menu_clear_rule->{
                viewModel.rssSource?.let {
                    appDb.rssReadAloudRuleDao.delete(it.sourceUrl)
                    viewModel.rssReadAloudRule = null
                }

            }
            R.id.menu_login -> startActivity<SourceLoginActivity> {
                putExtra("type", "rssSource")
                putExtra("key", viewModel.rssSource?.loginUrl)
            }

            R.id.menu_browser_open -> binding.webView.url?.let {
                openUrl(it)
            } ?: toastOnUi("url null")
        }
        return super.onCompatOptionsItemSelected(item)
    }

    @JavascriptInterface
    fun isNightTheme(): Boolean {
        return AppConfig.isNightTheme
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        binding.progressBar.fontColor = accentColor
        binding.webView.webChromeClient = CustomWebChromeClient()
        binding.webView.webViewClient = CustomWebViewClient()
        binding.webView.settings.apply {
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            domStorageEnabled = true
            allowContentAccess = true
            builtInZoomControls = true
            displayZoomControls = false
            setDarkeningAllowed(AppConfig.isNightTheme)
        }
        binding.webView.addJavascriptInterface(this, "thisActivity")
        viewModel.rssSource?.let {
            binding.webView.addJavascriptInterface(it, "thisSource")
        }
        binding.webView.setOnLongClickListener {
            val hitTestResult = binding.webView.hitTestResult
            if (hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE ||
                hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
            ) {
                hitTestResult.extra?.let { webPic ->
                    selector(
                        arrayListOf(
                            SelectItem(getString(R.string.action_save), "save"),
                            SelectItem(getString(R.string.select_folder), "selectFolder")
                        )
                    ) { _, charSequence, _ ->
                        when (charSequence.value) {
                            "save" -> saveImage(webPic)
                            "selectFolder" -> selectSaveFolder(null)
                        }
                    }
                    return@setOnLongClickListener true
                }
            }
            return@setOnLongClickListener false
        }
        binding.webView.setDownloadListener { url, _, contentDisposition, _, _ ->
            var fileName = URLUtil.guessFileName(url, contentDisposition, null)
            fileName = URLDecoder.decode(fileName, "UTF-8")
            binding.llView.longSnackbar(fileName, getString(R.string.action_download)) {
                Download.start(this, url, fileName)
            }
        }

    }

    private fun saveImage(webPic: String) {
        val path = ACache.get().getAsString(imagePathKey)
        if (path.isNullOrEmpty()) {
            selectSaveFolder(webPic)
        } else {
            viewModel.saveImage(webPic, Uri.parse(path))
        }
    }

    private fun selectSaveFolder(webPic: String?) {
        val default = arrayListOf<SelectItem<Int>>()
        val path = ACache.get().getAsString(imagePathKey)
        if (!path.isNullOrEmpty()) {
            default.add(SelectItem(path, -1))
        }
        selectImageDir.launch {
            otherActions = default
            value = webPic
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initLiveData() {
        viewModel.contentLiveData.observe(this) { content ->
            viewModel.rssArticle?.let {
                upJavaScriptEnable()
                val url = NetworkUtils.getAbsoluteURL(it.origin, it.link)
                val html = viewModel.clHtml(content)
                binding.webView.settings.userAgentString =
                    viewModel.rssSource?.getHeaderMap()?.get(AppConst.UA_NAME, true)
                        ?: AppConfig.userAgent
                if (viewModel.rssSource?.loadWithBaseUrl == true) {
                    binding.webView
                        .loadDataWithBaseURL(url, html, "text/html", "utf-8", url)//不想用baseUrl进else
                } else {
                    binding.webView
                        .loadDataWithBaseURL(null, html, "text/html;charset=utf-8", "utf-8", url)
                }
            }
        }
        viewModel.urlLiveData.observe(this) {
            upJavaScriptEnable()
            binding.webView.settings.userAgentString = it.getUserAgent()
            binding.webView.loadUrl(it.url, it.headerMap)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun upJavaScriptEnable() {
        if (viewModel.rssSource?.enableJs == true) {
            binding.webView.settings.javaScriptEnabled = true
        }
    }

    private fun upStarMenu() {
        starMenuItem?.isVisible = viewModel.rssArticle != null
        if (viewModel.rssStar != null) {
            starMenuItem?.setIcon(R.drawable.ic_star)
            starMenuItem?.setTitle(R.string.in_favorites)
        } else {
            starMenuItem?.setIcon(R.drawable.ic_star_border)
            starMenuItem?.setTitle(R.string.out_favorites)
        }
        starMenuItem?.icon?.setTintMutate(primaryTextColor)
    }

    private fun upTtsMenu(isPlaying: Boolean) {
        lifecycleScope.launch {
            if (isPlaying) {
                ttsMenuItem?.setIcon(R.drawable.ic_stop_black_24dp)
                ttsMenuItem?.setTitle(R.string.aloud_stop)
            } else {
                ttsMenuItem?.setIcon(R.drawable.ic_volume_up)
                ttsMenuItem?.setTitle(R.string.read_aloud)
            }
            ttsMenuItem?.icon?.setTintMutate(primaryTextColor)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun readAloud() {
        if(viewModel.exoPlayer.isPlaying){
            viewModel.stopPlay()
            viewModel.upTtsMenuData.postValue(false)
            return
        }
        if (viewModel.tts?.isSpeaking == true) {
            viewModel.tts?.stop()
            viewModel.upTtsMenuData.postValue(false)
        } else {
            binding.webView.settings.javaScriptEnabled = true
            binding.webView.evaluateJavascript("document.documentElement.outerHTML") {
                val html = StringEscapeUtils.unescapeJson(it)
                    .replace("^\"|\"$".toRegex(), "")
                viewModel.readAloud(
                    viewModel.readAloudRuleFile(html)
                )
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
    }

    inner class CustomWebChromeClient : WebChromeClient() {

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            binding.progressBar.setDurProgress(newProgress)
            binding.progressBar.gone(newProgress == 100)
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            binding.llView.invisible()
            binding.customWebView.addView(view)
            customWebViewCallback = callback
        }

        override fun onHideCustomView() {
            binding.customWebView.removeAllViews()
            binding.llView.visible()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    inner class CustomWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            return shouldOverrideUrlLoading(request.url)
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "KotlinRedundantDiagnosticSuppress")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return shouldOverrideUrlLoading(Uri.parse(url))
        }

        /**
         * 如果有黑名单,黑名单匹配返回空白,
         * 没有黑名单再判断白名单,在白名单中的才通过,
         * 都没有不做处理
         */
        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            val url = request.url.toString()
            val source = viewModel.rssSource ?: return super.shouldInterceptRequest(view, request)
            val blacklist = source.contentBlacklist?.splitNotBlank(",")
            if (!blacklist.isNullOrEmpty()) {
                blacklist.forEach {
                    try {
                        if (url.startsWith(it) || url.matches(it.toRegex())) {
                            return createEmptyResource()
                        }
                    } catch (e: PatternSyntaxException) {
                        AppLog.put("黑名单规则正则语法错误 源名称:${source.sourceName} 正则:$it", e)
                    }
                }
            } else {
                val whitelist = source.contentWhitelist?.splitNotBlank(",")
                if (!whitelist.isNullOrEmpty()) {
                    whitelist.forEach {
                        try {
                            if (url.startsWith(it) || url.matches(it.toRegex())) {
                                return super.shouldInterceptRequest(view, request)
                            }
                        } catch (e: PatternSyntaxException) {
                            val msg = "白名单规则正则语法错误 源名称:${source.sourceName} 正则:$it"
                            AppLog.put(msg, e)
                        }
                    }
                    return createEmptyResource()
                }
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            view.title?.let { title ->
                if (title != url && title != view.url && title.isNotBlank() && url != "about:blank") {
                    binding.titleBar.title = title
                    viewModel.title= title
                } else {
                    binding.titleBar.title = intent.getStringExtra("title")
                    viewModel.title= title
                }
            }
            viewModel.currentUrl = url
            viewModel.rssSource?.injectJs?.let {
                if (it.isNotBlank()) {
                    view.evaluateJavascript(it, null)
                }
            }
            viewModel.upTtsMenuData.value?.let {
                if(it){
                    viewModel.stopPlay()
                    readAloud()
                }
            }
        }

        private fun createEmptyResource(): WebResourceResponse {
            return WebResourceResponse(
                "text/plain",
                "utf-8",
                ByteArrayInputStream("".toByteArray())
            )
        }

        private fun shouldOverrideUrlLoading(url: Uri): Boolean {
            val source = viewModel.rssSource
            val js = source?.shouldOverrideUrlLoading
            if (!js.isNullOrBlank()) {
                val result = RhinoScriptEngine.runCatching {
                    eval(js) {
                        put("java", rssJsExtensions)
                        put("url", url.toString())
                    }.toString()
                }.onFailure {
                    AppLog.put("url跳转拦截js出错", it)
                }.getOrNull()
                if (result.isTrue()) {
                    return true
                }
            }
            when (url.scheme) {
                "http", "https", "jsbridge" -> {
                    return false
                }

                "legado", "yuedu" -> {
                    startActivity<OnLineImportActivity> {
                        data = url
                    }
                    return true
                }

                else -> {
                    binding.root.longSnackbar(R.string.jump_to_another_app, R.string.confirm) {
                        openUrl(url)
                    }
                    return true
                }
            }
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            handler?.proceed()
        }

    }

}
