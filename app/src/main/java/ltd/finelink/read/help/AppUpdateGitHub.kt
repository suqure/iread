package ltd.finelink.read.help

import android.os.Build
import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineScope
import ltd.finelink.read.R
import ltd.finelink.read.constant.AppConst
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.help.coroutine.Coroutine
import ltd.finelink.read.help.http.newCallStrResponse
import ltd.finelink.read.help.http.okHttpClient
import ltd.finelink.read.utils.jsonPath
import ltd.finelink.read.utils.readString
import splitties.init.appCtx

@Keep
@Suppress("unused")
object AppUpdateGitHub : AppUpdate.AppUpdateInterface {

    override fun check(
        scope: CoroutineScope,
    ): Coroutine<AppUpdate.UpdateInfo> {
        return Coroutine.async(scope) {
            val lastReleaseUrl = "https://app.finelink.ltd/api/releases/iread/latest"
            val body = okHttpClient.newCallStrResponse {
                url(lastReleaseUrl)
            }.body
            if (body.isNullOrBlank()) {
                throw NoStackTraceException(appCtx.getString(R.string.error_version))
            }
            val rootDoc = jsonPath.parse(body)
            val tagName = "1.0.0"
            if (tagName > AppConst.appInfo.versionName) {
                val updateBody = rootDoc.readString("$.data.description")
                    ?: throw NoStackTraceException(appCtx.getString(R.string.error_version))
                var downloadUrl = rootDoc.readString("$.data.path")
                    ?: throw NoStackTraceException(appCtx.getString(R.string.error_version))
                var x64 = rootDoc.readString("$.data.x64")
                var arm64 = rootDoc.readString("$.data.arm64")
                var arm = rootDoc.readString("$.data.arm")
                val abi = Build.SUPPORTED_ABIS[0]
                if(abi=="x86_64" && !x64.isNullOrEmpty()){
                    downloadUrl = x64
                }
                if(abi=="arm64-v8a" && !arm64.isNullOrEmpty()){
                    downloadUrl = arm64
                }
                if(abi=="armeabi-v7a" && !arm.isNullOrEmpty()){
                    downloadUrl = arm
                }
                val fileName = rootDoc.readString("$.data.fileName")
                    ?: throw NoStackTraceException(appCtx.getString(R.string.error_version))
                return@async AppUpdate.UpdateInfo(tagName, updateBody, downloadUrl, fileName)
            } else {
                throw NoStackTraceException(appCtx.getString(R.string.latest_version))
            }
        }.timeout(10000)
    }



}