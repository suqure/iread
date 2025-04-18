package ltd.finelink.read.lib.cronet

import org.chromium.net.CronetException

import java.nio.ByteBuffer


data class CallbackResult(
    val callbackStep: CallbackStep,
    val buffer: ByteBuffer? = null,
    val exception: CronetException? = null
)
