package ltd.finelink.read.ui.book.info.edit

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseActivity
import ltd.finelink.read.constant.BookType
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.databinding.ActivityBookInfoEditBinding
import ltd.finelink.read.help.book.BookHelp
import ltd.finelink.read.help.book.isAudio
import ltd.finelink.read.help.book.isImage
import ltd.finelink.read.help.book.isLocal
import ltd.finelink.read.ui.book.changecover.ChangeCoverDialog
import ltd.finelink.read.utils.*
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.MD5Utils
import ltd.finelink.read.utils.SelectImageContract
import ltd.finelink.read.utils.externalFiles
import ltd.finelink.read.utils.inputStream
import ltd.finelink.read.utils.launch
import ltd.finelink.read.utils.readUri
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.toastOnUi
import splitties.init.appCtx
import java.io.FileOutputStream

class BookInfoEditActivity :
    VMBaseActivity<ActivityBookInfoEditBinding, BookInfoEditViewModel>(fullScreen = false),
    ChangeCoverDialog.CallBack {

    private val selectCover = registerForActivityResult(SelectImageContract()) {
        it.uri?.let { uri ->
            coverChangeTo(uri)
        }
    }

    override val binding by viewBinding(ActivityBookInfoEditBinding::inflate)
    override val viewModel by viewModels<BookInfoEditViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.bookData.observe(this) { upView(it) }
        if (viewModel.bookData.value == null) {
            intent.getStringExtra("bookUrl")?.let {
                viewModel.loadBook(it)
            }
        }
        initEvent()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_info_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> saveData()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initEvent() = binding.run {
        tvChangeCover.setOnClickListener {
            viewModel.bookData.value?.let {
                showDialogFragment(
                    ChangeCoverDialog(it.name, it.author)
                )
            }
        }
        tvSelectCover.setOnClickListener {
            selectCover.launch()
        }
        tvRefreshCover.setOnClickListener {
            viewModel.book?.customCoverUrl = tieCoverUrl.text?.toString()
            upCover()
        }
    }

    private fun upView(book: Book) = binding.run {
        tieBookName.setText(book.name)
        tieBookAuthor.setText(book.author)
        spType.setSelection(
            when {
                book.isImage -> 2
                book.isAudio -> 1
                else -> 0
            }
        )
        tieCoverUrl.setText(book.getDisplayCover())
        tieBookIntro.setText(book.getDisplayIntro())
        upCover()
    }

    private fun upCover() {
        viewModel.book.let {
            binding.ivCover.load(it?.getDisplayCover(), it?.name, it?.author)
        }
    }

    private fun saveData() = binding.run {
        val book = viewModel.book ?: return@run
        val oldBook = book.copy()
        book.name = tieBookName.text?.toString() ?: ""
        book.author = tieBookAuthor.text?.toString() ?: ""
        val local = if (book.isLocal) BookType.local else 0
        book.type = when (spType.selectedItemPosition) {
            2 -> BookType.image or local
            1 -> BookType.audio or local
            else -> BookType.text or local
        }
        val customCoverUrl = tieCoverUrl.text?.toString()
        book.customCoverUrl = if (customCoverUrl == book.coverUrl) null else customCoverUrl
        book.customIntro = tieBookIntro.text?.toString()
        BookHelp.updateCacheFolder(oldBook, book)
        viewModel.saveBook(book) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun coverChangeTo(coverUrl: String) {
        viewModel.book?.customCoverUrl = coverUrl
        binding.tieCoverUrl.setText(coverUrl)
        upCover()
    }

    private fun coverChangeTo(uri: Uri) {
        readUri(uri) { fileDoc, inputStream ->
            runCatching {
                inputStream.use {
                    var file = this.externalFiles
                    val suffix = fileDoc.name.substringAfterLast(".")
                    val fileName = uri.inputStream(this).getOrThrow().use {
                        MD5Utils.md5Encode(it) + ".$suffix"
                    }
                    file = FileUtils.createFileIfNotExist(file, "covers", fileName)
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    coverChangeTo(file.absolutePath)
                }
            }.onFailure {
                appCtx.toastOnUi(it.localizedMessage)
            }
        }
    }

}