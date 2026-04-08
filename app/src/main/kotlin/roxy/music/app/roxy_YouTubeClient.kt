package roxy.music.app

enum class RoxyClientName {
    WEB_REMIX, IOS, ANDROID_MUSIC, ANDROID_VR, TVHTML5, WEB
}

data class RoxyYouTubeClient(
    val clientName: RoxyClientName,
    val ytClientName: String,
    val clientId: Int,
    val clientVersion: String,
    val userAgent: String,
    val hl: String = "en",
    val gl: String = "US"
)

object RoxyClientIdentities {
    val WEB_REMIX = RoxyYouTubeClient(
        clientName = RoxyClientName.WEB_REMIX,
        ytClientName = "WEB_REMIX",
        clientId = 67,
        clientVersion = "1.20250101.01.00",
        userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    )
    val IOS = RoxyYouTubeClient(
        clientName = RoxyClientName.IOS,
        ytClientName = "IOS",
        clientId = 5,
        clientVersion = "19.45.4",
        userAgent = "com.google.ios.youtube/19.45.4(iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X; en_US)"
    )
    val ANDROID_MUSIC = RoxyYouTubeClient(
        clientName = RoxyClientName.ANDROID_MUSIC,
        ytClientName = "ANDROID_MUSIC",
        clientId = 21,
        clientVersion = "6.31.55",
        userAgent = "com.google.android.apps.youtube.music/6.31.55 (Linux; U; Android 13; en_US)"
    )
    val ANDROID_VR = RoxyYouTubeClient(
        clientName = RoxyClientName.ANDROID_VR,
        ytClientName = "ANDROID_VR",
        clientId = 28,
        clientVersion = "1.60.19",
        userAgent = "com.google.android.apps.youtube.vr/1.60.19 (Linux; U; Android 10; en_US)"
    )
    val TVHTML5 = RoxyYouTubeClient(
        clientName = RoxyClientName.TVHTML5,
        ytClientName = "TVHTML5",
        clientId = 85,
        clientVersion = "7.20230531.05.00",
        userAgent = "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.5) AppleWebKit/537.36 Chrome/69.0.3497.106.1 TV Safari/537.36"
    )
    
    val fallbackList = listOf(ANDROID_MUSIC, TVHTML5, IOS, ANDROID_VR, WEB_REMIX)
}
