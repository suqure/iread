package ltd.finelink.read.ui.book.read.config

import android.content.DialogInterface
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.SeekBar
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseDialogFragment
import ltd.finelink.read.databinding.DialogAutoReadBinding
import ltd.finelink.read.help.config.ReadBookConfig
import ltd.finelink.read.lib.theme.bottomBackground
import ltd.finelink.read.lib.theme.getPrimaryTextColor
import ltd.finelink.read.model.ReadAloud
import ltd.finelink.read.model.ReadBook
import ltd.finelink.read.service.BaseReadAloudService
import ltd.finelink.read.ui.book.read.BaseReadBookActivity
import ltd.finelink.read.ui.book.read.ReadBookActivity
import ltd.finelink.read.ui.widget.seekbar.SeekBarChangeListener
import ltd.finelink.read.utils.ColorUtils
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding


class AutoReadDialog : BaseDialogFragment(R.layout.dialog_auto_read) {

    private val binding by viewBinding(DialogAutoReadBinding::bind)
    private val callBack: CallBack? get() = activity as? CallBack

    override fun onStart() {
        super.onStart()
        dialog?.window?.run {
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setBackgroundDrawableResource(R.color.background)
            decorView.setPadding(0, 0, 0, 0)
            val attr = attributes
            attr.dimAmount = 0.0f
            attr.gravity = Gravity.BOTTOM
            attributes = attr
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as ReadBookActivity).bottomDialog--
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) = binding.run {
        (activity as ReadBookActivity).bottomDialog++
        val bg = requireContext().bottomBackground
        val isLight = ColorUtils.isColorLight(bg)
        val textColor = requireContext().getPrimaryTextColor(isLight)
        root.setBackgroundColor(bg)
        tvReadSpeedTitle.setTextColor(textColor)
        tvReadSpeed.setTextColor(textColor)
        ivCatalog.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
        tvCatalog.setTextColor(textColor)
        ivMainMenu.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
        tvMainMenu.setTextColor(textColor)
        ivAutoPageStop.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
        tvAutoPageStop.setTextColor(textColor)
        ivSetting.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
        tvSetting.setTextColor(textColor)
        initOnChange()
        initData()
        initEvent()
    }

    private fun initData() {
        val speed = if (ReadBookConfig.autoReadSpeed < 2) 2 else ReadBookConfig.autoReadSpeed
        binding.tvReadSpeed.text = String.format("%ds", speed)
        binding.seekAutoRead.progress = speed
    }

    private fun initOnChange() {
        binding.seekAutoRead.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val speed = if (progress < 2) 2 else progress
                binding.tvReadSpeed.text = String.format("%ds", speed)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                ReadBookConfig.autoReadSpeed =
                    if (binding.seekAutoRead.progress < 2) 2 else binding.seekAutoRead.progress
                upTtsSpeechRate()
            }
        })
    }

    private fun initEvent() {
        binding.llMainMenu.setOnClickListener {
            callBack?.showMenuBar()
            dismissAllowingStateLoss()
        }
        binding.llSetting.setOnClickListener {
            (activity as BaseReadBookActivity).showPageAnimConfig {
                (activity as ReadBookActivity).upPageAnim()
                ReadBook.loadContent(false)
            }
        }
        binding.llCatalog.setOnClickListener { callBack?.openChapterList() }
        binding.llAutoPageStop.setOnClickListener {
            callBack?.autoPageStop()
            binding.llAutoPageStop.post {
                dismissAllowingStateLoss()
            }
        }
    }

    private fun upTtsSpeechRate() {
        ReadAloud.upTtsSpeechRate(requireContext())
        if (!BaseReadAloudService.pause) {
            ReadAloud.pause(requireContext())
            ReadAloud.resume(requireContext())
        }
    }

    interface CallBack {
        fun showMenuBar()
        fun openChapterList()
        fun autoPageStop()
    }
}