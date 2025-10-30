package com.waifuvault.mobile.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    SLOW_4G,
    NONE
}

class NetworkManager(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun getNetworkType(): NetworkType {
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                // Check if it's slow 4G based on download speed
                val downSpeed = capabilities.linkDownstreamBandwidthKbps
                if (downSpeed < 2000) NetworkType.SLOW_4G else NetworkType.CELLULAR
            }

            else -> NetworkType.NONE
        }
    }

    fun observeNetworkState(): Flow<NetworkType> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getNetworkType())
            }

            override fun onLost(network: Network) {
                trySend(NetworkType.NONE)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(getNetworkType())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Send initial state
        trySend(getNetworkType())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    fun getOptimalChunkSize(): Int {
        return when (getNetworkType()) {
            NetworkType.WIFI -> 10 * 1024 * 1024       // 10 MB
            NetworkType.ETHERNET -> 10 * 1024 * 1024   // 10 MB
            NetworkType.CELLULAR -> 5 * 1024 * 1024    // 5 MB
            NetworkType.SLOW_4G -> 2 * 1024 * 1024     // 2 MB
            NetworkType.NONE -> 5 * 1024 * 1024        // Default 5 MB
        }
    }
}
