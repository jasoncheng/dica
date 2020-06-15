package cool.mixi.dica.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import cool.mixi.dica.App


class NetworkUtil {
    companion object {
        fun isNetworkConnected(): Boolean {
            val mConnectivityManager = App.instance.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mNetworkInfo = mConnectivityManager.activeNetworkInfo
            if (mNetworkInfo != null && mNetworkInfo.isConnected) {
                return true
            }
            return false
        }
    }

}