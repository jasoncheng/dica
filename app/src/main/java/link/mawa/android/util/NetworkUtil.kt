package link.mawa.android.util

import android.content.Context
import android.net.ConnectivityManager
import link.mawa.android.App


class NetworkUtil {
    companion object {
        fun isNetworkConnected(): Boolean {
            val mConnectivityManager = App.instance.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mNetworkInfo = mConnectivityManager.activeNetworkInfo
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable
            }
            return false
        }
    }

}