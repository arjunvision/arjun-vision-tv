package com.arjunvision.tvlauncher.platform.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Wi-Fi and internet are tracked separately: Android marks a network VALIDATED only after it
 * has reached the internet, so "Wi-Fi connected" never implies "online".
 */
data class NetworkState(
    val connected: Boolean = false,
    val wifi: Boolean = false,
    val internet: Boolean = false,
) {
    val label: String
        get() = when {
            internet && wifi -> "Online · Wi-Fi"
            internet -> "Online"
            wifi -> "Wi-Fi · no internet"
            connected -> "No internet"
            else -> "Offline"
        }
}

fun currentNetworkState(context: Context): NetworkState {
    val cm = context.getSystemService(ConnectivityManager::class.java)
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return NetworkState()
    return NetworkState(
        connected = true,
        wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
        internet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
    )
}

/** Emits a new [NetworkState] on every change, including recovery. The callback is removed when collection stops. */
fun networkStateFlow(context: Context): Flow<NetworkState> = callbackFlow {
    val cm = context.getSystemService(ConnectivityManager::class.java)
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(currentNetworkState(context)) }
        override fun onLost(network: Network) { trySend(currentNetworkState(context)) }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            trySend(currentNetworkState(context))
        }
    }
    trySend(currentNetworkState(context))
    cm.registerDefaultNetworkCallback(callback)
    awaitClose { cm.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
