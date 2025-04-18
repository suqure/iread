package ltd.finelink.read.help.coroutine

import kotlin.coroutines.cancellation.CancellationException

class ActivelyCancelException : CancellationException() {

    override fun fillInStackTrace(): Throwable {
        stackTrace = emptyArray()
        return this
    }

}
