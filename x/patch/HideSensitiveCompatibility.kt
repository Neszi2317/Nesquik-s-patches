package app.crimera.patches.newx.utils
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
// Safety metadata only: behaviour is resolved from semantic APK contracts, never version routing.
internal val HIDE_SENSITIVE_COMPATIBILITY = Compatibility(
    name = "NewX", packageName = "com.twitter.android", apkFileType = ApkFileType.APKM,
    appIconColor = 0x000000, targets = listOf(AppTarget(version = "12.43.0-prod.01")),
)
