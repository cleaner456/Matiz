package roxy.music.app.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import roxy.music.app.constants.InnerTubeCookieKey
import roxy.music.app.constants.YtmSyncKey
import roxy.music.app.utils.dataStore
import roxy.music.app.utils.get
import roxy.music.app.utils.parseCookieString

fun Context.isSyncEnabled(): Boolean {
    return dataStore.get(YtmSyncKey, true) && isUserLoggedIn()
}

fun Context.isUserLoggedIn(): Boolean {
    val cookie = dataStore[InnerTubeCookieKey] ?: ""
    return "SAPISID" in parseCookieString(cookie) && isInternetConnected()
}

fun Context.isInternetConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
}
