package ltd.finelink.read.ui.association

import android.os.Bundle
import ltd.finelink.read.base.BaseActivity
import ltd.finelink.read.databinding.ActivityTranslucenceBinding
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding

/**
 * 验证码
 */
class VerificationCodeActivity :
    BaseActivity<ActivityTranslucenceBinding>() {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.getStringExtra("imageUrl")?.let {
            val sourceOrigin = intent.getStringExtra("sourceOrigin")
            val sourceName = intent.getStringExtra("sourceName")
            showDialogFragment(
                VerificationCodeDialog(it, sourceOrigin, sourceName)
            )
        } ?: finish()
    }

}