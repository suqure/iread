package ltd.finelink.read.ui.login

import android.os.Bundle
import androidx.activity.viewModels
import ltd.finelink.read.R
import ltd.finelink.read.base.VMBaseActivity
import ltd.finelink.read.data.entities.BaseSource
import ltd.finelink.read.databinding.ActivitySourceLoginBinding
import ltd.finelink.read.utils.showDialogFragment
import ltd.finelink.read.utils.viewbindingdelegate.viewBinding


class SourceLoginActivity : VMBaseActivity<ActivitySourceLoginBinding, SourceLoginViewModel>() {

    override val binding by viewBinding(ActivitySourceLoginBinding::inflate)
    override val viewModel by viewModels<SourceLoginViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.initData(intent) { source ->
            initView(source)
        }
    }

    private fun initView(source: BaseSource) {
        if (source.loginUi.isNullOrEmpty()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fl_fragment, WebViewLoginFragment(), "webViewLogin")
                .commit()
        } else {
            showDialogFragment<SourceLoginDialog>()
        }
    }

}