package ltd.finelink.read.lib.permission

interface OnPermissionsDeniedCallback {

    fun onPermissionsDenied(deniedPermissions: Array<String>)

}
