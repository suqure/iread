package ltd.finelink.read.exception

import ltd.finelink.read.R
import splitties.init.appCtx

class NoBooksDirException: NoStackTraceException(appCtx.getString(R.string.no_books_dir))