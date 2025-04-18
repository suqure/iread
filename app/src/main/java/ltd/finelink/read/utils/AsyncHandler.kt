package ltd.finelink.read.utils

import ltd.finelink.read.help.globalExecutor
import java.util.logging.Handler
import java.util.logging.LogRecord

class AsyncHandler(private val delegate: Handler) : Handler() {

    override fun publish(record: LogRecord?) {
        globalExecutor.execute {
            delegate.publish(record)
        }
    }

    override fun flush() {
        delegate.flush()
    }

    override fun close() {
        delegate.close()
    }

}

fun Handler.asynchronous() = AsyncHandler(this)
