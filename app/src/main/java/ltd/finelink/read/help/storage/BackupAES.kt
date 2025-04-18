package ltd.finelink.read.help.storage

import cn.hutool.crypto.symmetric.AES
import ltd.finelink.read.help.config.LocalConfig
import ltd.finelink.read.utils.MD5Utils

class BackupAES : AES(
    MD5Utils.md5Encode(LocalConfig.password ?: "").encodeToByteArray(0, 16)
)