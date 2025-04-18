package ltd.finelink.read.utils

import android.util.Log
import ltd.finelink.read.BuildConfig

object DebugLog {

    fun e(tag: String, throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, throwable.stackTraceToString())
        }
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable == null) {
                Log.e(tag, msg)
            } else {
                Log.e(tag, msg, throwable)
            }
        }
    }

    fun d(tag: String, msg: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable == null) {
                Log.d(tag, msg)
            } else {
                Log.d(tag, msg, throwable)
            }
        }
    }

    fun i(tag: String, msg: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable == null) {
                Log.i(tag, msg)
            } else {
                Log.i(tag, msg, throwable)
            }
        }
    }

    fun w(tag: String, msg: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable == null) {
                Log.w(tag, msg)
            } else {
                Log.w(tag, msg, throwable)
            }
        }
    }

}