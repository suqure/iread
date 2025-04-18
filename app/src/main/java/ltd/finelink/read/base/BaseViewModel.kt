package ltd.finelink.read.base

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import ltd.finelink.read.App
import ltd.finelink.read.help.coroutine.Coroutine
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
open class BaseViewModel(application: Application) : AndroidViewModel(application) {

    val context: Context by lazy { this.getApplication<App>() }

    fun <T> execute(
        scope: CoroutineScope = viewModelScope,
        context: CoroutineContext = Dispatchers.IO,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        executeContext: CoroutineContext = Dispatchers.Main,
        block: suspend CoroutineScope.() -> T
    ): Coroutine<T> {
        return Coroutine.async(scope, context, start, executeContext, block)
    }

    fun <R> submit(
        scope: CoroutineScope = viewModelScope,
        context: CoroutineContext = Dispatchers.IO,
        block: suspend CoroutineScope.() -> Deferred<R>
    ): Coroutine<R> {
        return Coroutine.async(scope, context) { block().await() }
    }

}