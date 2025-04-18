package ltd.finelink.read.ui.main.bookshelf

import android.annotation.SuppressLint
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseFragment
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.BookGroup
import ltd.finelink.read.databinding.DialogBookshelfConfigBinding
import ltd.finelink.read.databinding.DialogEditTextBinding
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.lib.dialogs.alert
import ltd.finelink.read.ui.about.AppLogDialog
import ltd.finelink.read.ui.book.cache.CacheActivity
import ltd.finelink.read.ui.book.group.GroupManageDialog
import ltd.finelink.read.ui.book.import.local.ImportBookActivity
import ltd.finelink.read.ui.book.manage.BookshelfManageActivity
import ltd.finelink.read.ui.book.read.config.ReadAloudConfigDialog
import ltd.finelink.read.ui.book.search.SearchActivity
import ltd.finelink.read.ui.file.HandleFileContract
import ltd.finelink.read.ui.main.MainFragmentInterface
import ltd.finelink.read.ui.main.MainViewModel
import ltd.finelink.read.ui.widget.dialog.WaitDialog
import ltd.finelink.read.utils.checkByIndex
import ltd.finelink.read.utils.getCheckedIndex
import ltd.finelink.read.utils.postEvent
import ltd.finelink.read.utils.readText
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.startActivity
import ltd.finelink.read.utils.toastOnUi

abstract class BaseBookshelfFragment(layoutId: Int) : VMBaseFragment<BookshelfViewModel>(layoutId),
    MainFragmentInterface {

    override val position: Int? get() = arguments?.getInt("position")

    val activityViewModel by activityViewModels<MainViewModel>()
    override val viewModel by viewModels<BookshelfViewModel>()

    private val importBookshelf = registerForActivityResult(HandleFileContract()) {
        kotlin.runCatching {
            it.uri?.readText(requireContext())?.let { text ->
                viewModel.importBookshelf(text, groupId)
            }
        }.onFailure {
            toastOnUi(it.localizedMessage ?: "ERROR")
        }
    }

    abstract val groupId: Long
    abstract val books: List<Book>
    private var groupsLiveData: LiveData<List<BookGroup>>? = null
    private val waitDialog by lazy {
        WaitDialog(requireContext()).apply {
            setOnCancelListener {
                viewModel.addBookJob?.cancel()
            }
        }
    }

    abstract fun gotoTop()

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_bookshelf, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        when (item.itemId) {
            R.id.menu_search -> startActivity<SearchActivity>()
            R.id.menu_update_toc -> activityViewModel.upToc(books)
            R.id.menu_bookshelf_layout -> configBookshelf()
            R.id.menu_group_manage -> showDialogFragment<GroupManageDialog>()
            R.id.menu_aloud_config->  ReadAloudConfigDialog().show(childFragmentManager, "readAloudConfigDialog")
            R.id.menu_add_local -> startActivity<ImportBookActivity>()
            R.id.menu_bookshelf_manage -> startActivity<BookshelfManageActivity> {
                putExtra("groupId", groupId)
            }

            R.id.menu_download -> startActivity<CacheActivity> {
                putExtra("groupId", groupId)
            }
            R.id.menu_log -> showDialogFragment<AppLogDialog>()
        }
    }

    protected fun initBookGroupData() {
        groupsLiveData?.removeObservers(viewLifecycleOwner)
        groupsLiveData = appDb.bookGroupDao.show.apply {
            observe(viewLifecycleOwner) {
                upGroup(it)
            }
        }
    }

    abstract fun upGroup(data: List<BookGroup>)

    abstract fun upSort()

    override fun observeLiveBus() {
        viewModel.addBookProgressLiveData.observe(this) { count ->
            if (count < 0) {
                waitDialog.dismiss()
            } else {
                waitDialog.setText("添加中... ($count)")
            }
        }
    }

    @SuppressLint("InflateParams")
    fun showAddBookByUrlAlert() {
        alert(titleResource = R.string.add_book_url) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url"
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    waitDialog.setText("添加中...")
                    waitDialog.show()
                    viewModel.addBookByUrl(it)
                }
            }
            noButton()
        }
    }

    @SuppressLint("InflateParams")
    fun configBookshelf() {
        alert(titleResource = R.string.bookshelf_layout) {
            val bookshelfLayout = AppConfig.bookshelfLayout
            val bookshelfSort = AppConfig.bookshelfSort
            val alertBinding =
                DialogBookshelfConfigBinding.inflate(layoutInflater)
                    .apply {
                        spGroupStyle.setSelection(AppConfig.bookGroupStyle)
                        swShowUnread.isChecked = AppConfig.showUnread
                        swShowLastUpdateTime.isChecked = AppConfig.showLastUpdateTime
                        swShowWaitUpBooks.isChecked = AppConfig.showWaitUpCount
                        swShowBookshelfFastScroller.isChecked = AppConfig.showBookshelfFastScroller
                        rgLayout.checkByIndex(bookshelfLayout)
                        rgSort.checkByIndex(bookshelfSort)
                    }
            customView { alertBinding.root }
            okButton {
                alertBinding.apply {
                    if (AppConfig.bookGroupStyle != spGroupStyle.selectedItemPosition) {
                        AppConfig.bookGroupStyle = spGroupStyle.selectedItemPosition
                        postEvent(EventBus.NOTIFY_MAIN, false)
                    }
                    if (AppConfig.showUnread != swShowUnread.isChecked) {
                        AppConfig.showUnread = swShowUnread.isChecked
                        postEvent(EventBus.BOOKSHELF_REFRESH, "")
                    }
                    if (AppConfig.showLastUpdateTime != swShowLastUpdateTime.isChecked) {
                        AppConfig.showLastUpdateTime = swShowLastUpdateTime.isChecked
                        postEvent(EventBus.BOOKSHELF_REFRESH, "")
                    }
                    if (AppConfig.showWaitUpCount != swShowWaitUpBooks.isChecked) {
                        AppConfig.showWaitUpCount = swShowWaitUpBooks.isChecked
                        activityViewModel.postUpBooksLiveData(true)
                    }
                    if (AppConfig.showBookshelfFastScroller != swShowBookshelfFastScroller.isChecked) {
                        AppConfig.showBookshelfFastScroller = swShowBookshelfFastScroller.isChecked
                        postEvent(EventBus.BOOKSHELF_REFRESH, "")
                    }
                    if (bookshelfSort != rgSort.getCheckedIndex()) {
                        AppConfig.bookshelfSort = rgSort.getCheckedIndex()
                        upSort()
                    }
                    if (bookshelfLayout != rgLayout.getCheckedIndex()) {
                        AppConfig.bookshelfLayout = rgLayout.getCheckedIndex()
                        postEvent(EventBus.RECREATE, "")
                    }
                }
            }
            cancelButton()
        }
    }


    private fun importBookshelfAlert(groupId: Long) {
        alert(titleResource = R.string.import_bookshelf) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url/json"
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    viewModel.importBookshelf(it, groupId)
                }
            }
            noButton()
            neutralButton(R.string.select_file) {
                importBookshelf.launch {
                    mode = HandleFileContract.FILE
                    allowExtensions = arrayOf("txt", "json")
                }
            }
        }
    }

}