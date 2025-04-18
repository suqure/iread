package ltd.finelink.read.ui.widget.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.bumptech.glide.request.RequestOptions
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseDialogFragment
import ltd.finelink.read.databinding.DialogPhotoViewBinding
import ltd.finelink.read.help.book.BookHelp
import ltd.finelink.read.help.glide.ImageLoader
import ltd.finelink.read.help.glide.OkHttpModelLoader
import ltd.finelink.read.model.BookCover
import ltd.finelink.read.model.ImageProvider
import ltd.finelink.read.model.ReadBook
import ltd.finelink.read.utils.setLayout
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding

/**
 * 显示图片
 */
class PhotoDialog() : BaseDialogFragment(R.layout.dialog_photo_view) {

    constructor(src: String, sourceOrigin: String? = null) : this() {
        arguments = Bundle().apply {
            putString("src", src)
            putString("sourceOrigin", sourceOrigin)
        }
    }

    private val binding by viewBinding(DialogPhotoViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(1f, 1f)
    }

    @SuppressLint("CheckResult")
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val arguments = arguments ?: return
        arguments.getString("src")?.let { src ->
            ImageProvider.bitmapLruCache.get(src)?.let {
                binding.photoView.setImageBitmap(it)
                return
            }
            val file = ReadBook.book?.let { book ->
                BookHelp.getImage(book, src)
            }
            if (file?.exists() == true) {
                ImageLoader.load(requireContext(), file)
                    .error(R.drawable.image_loading_error)
                    .into(binding.photoView)
            } else {
                ImageLoader.load(requireContext(), src).apply {
                    arguments.getString("sourceOrigin")?.let { sourceOrigin ->
                        apply(
                            RequestOptions().set(
                                OkHttpModelLoader.sourceOriginOption,
                                sourceOrigin
                            )
                        )
                    }
                }.error(BookCover.defaultDrawable)
                    .into(binding.photoView)
            }
        }
    }

}
