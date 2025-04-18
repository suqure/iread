package ltd.finelink.read.ui.book.info

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseActivity
import ltd.finelink.read.constant.BookType
import ltd.finelink.read.constant.Theme
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.BookChapter
import ltd.finelink.read.data.entities.BookSource
import ltd.finelink.read.databinding.ActivityBookInfoBinding
import ltd.finelink.read.help.book.isAudio
import ltd.finelink.read.help.book.isLocal
import ltd.finelink.read.help.book.isLocalTxt
import ltd.finelink.read.help.book.isWebFile
import ltd.finelink.read.help.book.removeType
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.help.config.LocalConfig
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.lib.dialogs.selector
import ltd.finelink.read.lib.theme.accentColor
import ltd.finelink.read.lib.theme.backgroundColor
import ltd.finelink.read.lib.theme.bottomBackground
import ltd.finelink.read.lib.theme.getPrimaryTextColor
import ltd.finelink.read.model.BookCover
import ltd.finelink.read.ui.book.audio.AudioPlayActivity
import ltd.finelink.read.ui.book.changecover.ChangeCoverDialog
import ltd.finelink.read.ui.book.changesource.ChangeBookSourceDialog
import ltd.finelink.read.ui.book.group.GroupSelectDialog
import ltd.finelink.read.ui.book.info.edit.BookInfoEditActivity
import ltd.finelink.read.ui.book.read.ReadBookActivity
import ltd.finelink.read.ui.book.search.SearchActivity
import ltd.finelink.read.ui.book.source.edit.BookSourceEditActivity
import ltd.finelink.read.ui.book.toc.TocActivityResult
import ltd.finelink.read.ui.file.HandleFileContract
import ltd.finelink.read.ui.login.SourceLoginActivity
import ltd.finelink.read.ui.widget.dialog.PhotoDialog
import ltd.finelink.read.ui.widget.dialog.VariableDialog
import ltd.finelink.read.ui.widget.dialog.WaitDialog
import ltd.finelink.read.utils.ColorUtils
import ltd.finelink.read.utils.StartActivityContract
import ltd.finelink.read.utils.dpToPx
import ltd.finelink.read.utils.gone
import ltd.finelink.read.utils.longToastOnUi
import ltd.finelink.read.utils.openFileUri
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.startActivity
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import ltd.finelink.read.utils.visible

class BookInfoActivity :
    VMBaseActivity<ActivityBookInfoBinding, BookInfoViewModel>(toolBarTheme = Theme.Dark),
    GroupSelectDialog.CallBack,
    ChangeBookSourceDialog.CallBack,
    ChangeCoverDialog.CallBack,
    VariableDialog.Callback {

    private val tocActivityResult = registerForActivityResult(TocActivityResult()) {
        it?.let {
            viewModel.getBook(false)?.let { book ->
                lifecycleScope.launch {
                    withContext(IO) {
                        book.durChapterIndex = it.first
                        book.durChapterPos = it.second
                        chapterChanged = it.third
                        appDb.bookDao.update(book)
                    }
                    startReadActivity(book)
                }
            }
        } ?: let {
            if (!viewModel.inBookshelf) {
                viewModel.delBook()
            }
        }
    }
    private val localBookTreeSelect = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { treeUri ->
            AppConfig.defaultBookTreeUri = treeUri.toString()
        }
    }
    private val readBookResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.upBook(intent)
        if (it.resultCode == RESULT_OK) {
            viewModel.inBookshelf = true
            upTvBookshelf()
        }
    }
    private val infoEditResult = registerForActivityResult(
        StartActivityContract(BookInfoEditActivity::class.java)
    ) {
        if (it.resultCode == RESULT_OK) {
            viewModel.upEditBook()
        }
    }
    private val editSourceResult = registerForActivityResult(
        StartActivityContract(BookSourceEditActivity::class.java)
    ) {
        if (it.resultCode == RESULT_CANCELED) {
            return@registerForActivityResult
        }
        book?.let { book ->
            viewModel.bookSource = appDb.bookSourceDao.getBookSource(book.origin)
            viewModel.refreshBook(book)
        }
    }
    private var tocChanged = false
    private var chapterChanged = false
    private val waitDialog by lazy { WaitDialog(this) }
    private var editMenuItem: MenuItem? = null
    private val book get() = viewModel.getBook(false)

    override val binding by viewBinding(ActivityBookInfoBinding::inflate)
    override val viewModel by viewModels<BookInfoViewModel>()

    @SuppressLint("PrivateResource")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.setBackgroundResource(R.color.transparent)
        binding.refreshLayout?.setColorSchemeColors(accentColor)
        binding.arcView.setBgColor(backgroundColor)
        binding.llInfo.setBackgroundColor(backgroundColor)
        binding.flAction.setBackgroundColor(bottomBackground)
        binding.tvShelf.setTextColor(getPrimaryTextColor(ColorUtils.isColorLight(bottomBackground)))
        binding.tvToc.text = getString(R.string.toc_s, getString(R.string.loading))
        viewModel.bookData.observe(this) { showBook(it) }
        viewModel.chapterListData.observe(this) { upLoading(false, it) }
        viewModel.waitDialogData.observe(this) { upWaitDialogStatus(it) }
        viewModel.initData(intent)

        initViewEvent()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_info, menu)
        editMenuItem = menu.findItem(R.id.menu_edit)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_can_update)?.isChecked =
            viewModel.bookData.value?.canUpdate ?: true
        menu.findItem(R.id.menu_split_long_chapter)?.isChecked =
            viewModel.bookData.value?.getSplitLongChapter() ?: true
        menu.findItem(R.id.menu_login)?.isVisible =
            !viewModel.bookSource?.loginUrl.isNullOrBlank()
        menu.findItem(R.id.menu_set_source_variable)?.isVisible =
            viewModel.bookSource != null
        menu.findItem(R.id.menu_set_book_variable)?.isVisible =
            viewModel.bookSource != null
        menu.findItem(R.id.menu_can_update)?.isVisible =
            viewModel.bookSource != null
        menu.findItem(R.id.menu_split_long_chapter)?.isVisible =
            viewModel.bookData.value?.isLocalTxt ?: false
        menu.findItem(R.id.menu_delete_alert)?.isChecked =
            LocalConfig.bookInfoDeleteAlert
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_edit -> {
                viewModel.getBook()?.let {
                    infoEditResult.launch {
                        putExtra("bookUrl", it.bookUrl)
                    }
                }
            }

            R.id.menu_delete_it -> {
                viewModel.getBook()?.let {
                    deleteBook()
                }
            }

            R.id.menu_refresh -> {
                refreshBook()
            }

            R.id.menu_login -> viewModel.bookSource?.let {
                startActivity<SourceLoginActivity> {
                    putExtra("type", "bookSource")
                    putExtra("key", it.bookSourceUrl)
                }
            }

            R.id.menu_top -> viewModel.topBook()
            R.id.menu_set_source_variable -> setSourceVariable()
            R.id.menu_set_book_variable -> setBookVariable()


            R.id.menu_can_update -> {
                viewModel.getBook()?.let {
                    it.canUpdate = !it.canUpdate
                    if (viewModel.inBookshelf) {
                        if (!it.canUpdate) {
                            it.removeType(BookType.updateError)
                        }
                        viewModel.saveBook(it)
                    }
                }
            }

            R.id.menu_clear_cache -> viewModel.clearCache()
            R.id.menu_clear_tts_cache -> alert(
                titleResource = R.string.draw,
                messageResource = R.string.sure_del_book_cache
            ) {
                yesButton {
                    viewModel.clearTtsCache()
                }
                noButton()
            }
            R.id.menu_split_long_chapter -> {
                upLoading(true)
                tocChanged = true
                viewModel.getBook()?.let {
                    it.setSplitLongChapter(!item.isChecked)
                    viewModel.loadBookInfo(it, false)
                }
                item.isChecked = !item.isChecked
                if (!item.isChecked) longToastOnUi(R.string.need_more_time_load_content)
            }

            R.id.menu_delete_alert -> LocalConfig.bookInfoDeleteAlert = !item.isChecked
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun observeLiveBus() {
        viewModel.actionLive.observe(this) {
            when (it) {
                "selectBooksDir" -> localBookTreeSelect.launch {
                    title = getString(R.string.select_book_folder)
                }
            }
        }
    }

    private fun refreshBook() {
        upLoading(true)
        viewModel.getBook()?.let {
            viewModel.refreshBook(it)
        }
    }


    private fun showBook(book: Book) = binding.run {
        showCover(book)
        tvName.text = book.name
        tvAuthor.text = getString(R.string.author_show, book.getRealAuthor())
        tvOrigin.text = getString(R.string.origin_show, book.originName)
        tvLasted.text = getString(R.string.lasted_show, book.latestChapterTitle)
        tvIntro.text = book.getDisplayIntro()
        llToc?.visible(!book.isWebFile)
        upTvBookshelf()
        val kinds = book.getKindList()
        if (kinds.isEmpty()) {
            lbKind.gone()
        } else {
            lbKind.visible()
            lbKind.setLabels(kinds)
        }
        upGroup(book.group)
    }

    private fun showCover(book: Book) {
        binding.ivCover.load(book.getDisplayCover(), book.name, book.author, false, book.origin)
        if (!AppConfig.isEInkMode) {
            BookCover.loadBlur(this, book.getDisplayCover())
                .into(binding.bgBook)
        }
    }

    private fun upLoading(isLoading: Boolean, chapterList: List<BookChapter>? = null) {
        when {
            isLoading -> {
                binding.tvToc.text = getString(R.string.toc_s, getString(R.string.loading))
            }

            chapterList.isNullOrEmpty() -> {
                binding.tvToc.text = getString(
                    R.string.toc_s,
                    getString(R.string.error_load_toc)
                )
            }

            else -> {
                book?.let {
                    binding.tvToc.text = getString(R.string.toc_s, it.durChapterTitle)
                    binding.tvLasted.text = getString(R.string.lasted_show, it.latestChapterTitle)
                }
            }
        }
    }

    private fun upTvBookshelf() {
        editMenuItem?.isVisible = viewModel.inBookshelf
    }

    private fun upGroup(groupId: Long) {
        viewModel.loadGroup(groupId) {
            if (it.isNullOrEmpty()) {
                binding.tvGroup.text = if (book?.isLocal == true) {
                    getString(R.string.group_s, getString(R.string.local_no_group))
                } else {
                    getString(R.string.group_s, getString(R.string.no_group))
                }
            } else {
                binding.tvGroup.text = getString(R.string.group_s, it)
            }
        }
    }

    private fun initViewEvent() = binding.run {
        ivCover.setOnClickListener {
            viewModel.getBook()?.let {
                showDialogFragment(
                    ChangeCoverDialog(it.name, it.author)
                )
            }
        }
        ivCover.setOnLongClickListener {
            viewModel.getBook()?.getDisplayCover()?.let { path ->
                showDialogFragment(PhotoDialog(path))
            }
            true
        }
        tvRead.setOnClickListener {
            viewModel.getBook()?.let { book ->
                if (book.isWebFile) {
                    showWebFileDownloadAlert {
                        readBook(it)
                    }
                } else {
                    readBook(book)
                }
            }
        }
        tvShelf.setOnClickListener {
            viewModel.getBook()?.let { book ->
                if (book.isWebFile) {
                    showWebFileDownloadAlert {
                        readAloudBook(it)
                    }
                } else {
                    readAloudBook(book)
                }
            }
        }
        tvOrigin.setOnClickListener {
            viewModel.getBook()?.let { book ->
                if (book.isLocal) return@let
                editSourceResult.launch {
                    putExtra("sourceUrl", book.origin)
                }
            }
        }
        tvChangeSource.setOnClickListener {
            viewModel.getBook()?.let { book ->
                showDialogFragment(ChangeBookSourceDialog(book.name, book.author))
            }
        }
        tvTocView.setOnClickListener {
            if (viewModel.chapterListData.value.isNullOrEmpty()) {
                toastOnUi(R.string.chapter_list_empty)
                return@setOnClickListener
            }
            viewModel.getBook()?.let { book ->
                if (!viewModel.inBookshelf) {
                    viewModel.saveBook(book) {
                        viewModel.saveChapterList {
                            openChapterList()
                        }
                    }
                } else {
                    openChapterList()
                }
            }
        }
        tvChangeGroup.setOnClickListener {
            viewModel.getBook()?.let {
                showDialogFragment(
                    GroupSelectDialog(it.group)
                )
            }
        }
        tvAuthor.setOnClickListener {
            viewModel.getBook(false)?.let { book ->
                startActivity<SearchActivity> {
                    putExtra("key", book.author)
                }
            }
        }
        tvName.setOnClickListener {
            viewModel.getBook(false)?.let { book ->
                startActivity<SearchActivity> {
                    putExtra("key", book.name)
                }
            }
        }
        refreshLayout?.setOnRefreshListener {
            refreshLayout.isRefreshing = false
            refreshBook()
        }
    }

    private fun setSourceVariable() {
        lifecycleScope.launch {
            val source = viewModel.bookSource
            if (source == null) {
                toastOnUi("书源不存在")
                return@launch
            }
            val comment =
                source.getDisplayVariableComment("源变量可在js中通过source.getVariable()获取")
            val variable = withContext(IO) { source.getVariable() }
            showDialogFragment(
                VariableDialog(
                    getString(R.string.set_source_variable),
                    source.getKey(),
                    variable,
                    comment
                )
            )
        }
    }

    private fun setBookVariable() {
        lifecycleScope.launch {
            val source = viewModel.bookSource
            if (source == null) {
                toastOnUi("书源不存在")
                return@launch
            }
            val book = viewModel.getBook() ?: return@launch
            val variable = withContext(IO) { book.getCustomVariable() }
            val comment =
                source.getDisplayVariableComment("""书籍变量可在js中通过book.getVariable("custom")获取""")
            showDialogFragment(
                VariableDialog(
                    getString(R.string.set_book_variable),
                    book.bookUrl,
                    variable,
                    comment
                )
            )
        }
    }

    override fun setVariable(key: String, variable: String?) {
        when (key) {
            viewModel.bookSource?.getKey() -> viewModel.bookSource?.setVariable(variable)
            viewModel.bookData.value?.bookUrl -> viewModel.bookData.value?.let {
                it.putCustomVariable(variable)
                viewModel.saveBook(it)
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun deleteBook() {
        viewModel.getBook()?.let {
            if (LocalConfig.bookInfoDeleteAlert) {
                alert(
                    titleResource = R.string.draw,
                    messageResource = R.string.sure_del
                ) {
                    var checkBox: CheckBox? = null
                    if (it.isLocal) {
                        checkBox = CheckBox(this@BookInfoActivity).apply {
                            setText(R.string.delete_book_file)
                            isChecked = LocalConfig.deleteBookOriginal
                        }
                        val view = LinearLayout(this@BookInfoActivity).apply {
                            setPadding(16.dpToPx(), 0, 16.dpToPx(), 0)
                            addView(checkBox)
                        }
                        customView { view }
                    }
                    yesButton {
                        if (checkBox != null) {
                            LocalConfig.deleteBookOriginal = checkBox.isChecked
                        }
                        viewModel.delBook(LocalConfig.deleteBookOriginal) {
                            finish()
                        }
                    }
                    noButton()
                }
            } else {
                viewModel.delBook(LocalConfig.deleteBookOriginal) {
                    finish()
                }
            }
        }
    }

    private fun openChapterList() {
        viewModel.getBook()?.let {
            tocActivityResult.launch(it.bookUrl)
        }
    }

    private fun showWebFileDownloadAlert(
        onClick: ((Book) -> Unit)? = null
    ) {
        val webFiles = viewModel.webFiles
        if (webFiles.isEmpty()) {
            toastOnUi("Unexpected webFileData")
            return
        }
        selector(
            R.string.download_and_import_file,
            webFiles
        ) { _, webFile, _ ->
            if (webFile.isSupported) {
                /* import */
                viewModel.importOrDownloadWebFile<Book>(webFile) {
                    onClick?.invoke(it)
                }
            } else if (webFile.isSupportDecompress) {
                /* 解压筛选后再选择导入项 */
                viewModel.importOrDownloadWebFile<Uri>(webFile) { uri ->
                    viewModel.getArchiveFilesName(uri) { fileNames ->
                        if (fileNames.size == 1) {
                            viewModel.importArchiveBook(uri, fileNames[0]) {
                                onClick?.invoke(it)
                            }
                        } else {
                            showDecompressFileImportAlert(uri, fileNames, onClick)
                        }
                    }
                }
            } else {
                alert(
                    title = getString(R.string.draw),
                    message = getString(R.string.file_not_supported, webFile.name)
                ) {
                    neutralButton(R.string.open_fun) {
                        /* download only */
                        viewModel.importOrDownloadWebFile<Uri>(webFile) {
                            openFileUri(it, "*/*")
                        }
                    }
                    noButton()
                }
            }
        }
    }

    private fun showDecompressFileImportAlert(
        archiveFileUri: Uri,
        fileNames: List<String>,
        success: ((Book) -> Unit)? = null
    ) {
        if (fileNames.isEmpty()) {
            toastOnUi(R.string.unsupport_archivefile_entry)
            return
        }
        selector(
            R.string.import_select_book,
            fileNames
        ) { _, name, _ ->
            viewModel.importArchiveBook(archiveFileUri, name) {
                success?.invoke(it)
            }
        }
    }

    private fun readBook(book: Book) {
        if (!viewModel.inBookshelf) {
            viewModel.saveBook(book) {
                viewModel.saveChapterList {
                    startReadActivity(book)
                }
            }
        } else {
            viewModel.saveBook(book) {
                startReadActivity(book)
            }
        }
    }

    private fun readAloudBook(book: Book) {
        if (!viewModel.inBookshelf) {
            viewModel.saveBook(book) {
                viewModel.saveChapterList {
                    startReadAloudActivity(book)
                }
            }
        } else {
            viewModel.saveBook(book) {
                startReadAloudActivity(book)
            }
        }
    }

    private fun startReadActivity(book: Book) {
        when {
            book.isAudio -> readBookResult.launch(
                Intent(this, AudioPlayActivity::class.java)
                    .putExtra("bookUrl", book.bookUrl)
                    .putExtra("inBookshelf", viewModel.inBookshelf)
            )

            else -> readBookResult.launch(
                Intent(this, ReadBookActivity::class.java)
                    .putExtra("bookUrl", book.bookUrl)
                    .putExtra("inBookshelf", viewModel.inBookshelf)
                    .putExtra("tocChanged", tocChanged)
                    .putExtra("chapterChanged", chapterChanged)
            )
        }
        tocChanged = false
    }

    private fun startReadAloudActivity(book: Book) {
        readBookResult.launch(
            Intent(this, ReadBookActivity::class.java)
                .putExtra("bookUrl", book.bookUrl)
                .putExtra("inBookshelf", viewModel.inBookshelf)
                .putExtra("tocChanged", tocChanged)
                .putExtra("chapterChanged", chapterChanged)
                .putExtra("readAloud",true)
        )
        tocChanged = false
    }

    override val oldBook: Book?
        get() = viewModel.bookData.value

    override fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        viewModel.changeTo(source, book, toc)
    }

    override fun coverChangeTo(coverUrl: String) {
        viewModel.bookData.value?.let { book ->
            book.customCoverUrl = coverUrl
            showCover(book)
            if (viewModel.inBookshelf) {
                viewModel.saveBook(book)
            }
        }
    }

    override fun upGroup(requestCode: Int, groupId: Long) {
        upGroup(groupId)
        viewModel.getBook()?.let { book ->
            book.group = groupId
            if (viewModel.inBookshelf) {
                viewModel.saveBook(book)
            } else if (groupId > 0) {
                viewModel.addToBookshelf {
                    upTvBookshelf()
                }
            }
        }
    }

    private fun upWaitDialogStatus(isShow: Boolean) {
        val showText = "Loading....."
        if (isShow) {
            waitDialog.run {
                setText(showText)
                show()
            }
        } else {
            waitDialog.dismiss()
        }
    }

}