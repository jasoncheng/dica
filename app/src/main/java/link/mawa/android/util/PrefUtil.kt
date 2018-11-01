package link.mawa.android.util

import android.content.SharedPreferences
import android.preference.PreferenceManager
import link.mawa.android.App
import link.mawa.android.bean.Consts

class PrefUtil {

    companion object {
        fun default(): SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(App.instance.applicationContext)

        fun setUsername(username: String) {
            default().edit().putString("username", username).commit()
        }

        fun getUsername(): String {
            return default().getString("username", "")
        }

        fun setPassword(password: String) {
            default().edit().putString("password", password).commit()
        }

        fun getPassword(): String {
            return default().getString("password", "")
        }

        fun setApiUrl(url: String) {
            default().edit().putString("api", url).commit()
        }

        fun getApiUrl(): String {
            var api = default().getString("api", Consts.API_HOST)
            return if (api.isEmpty()) Consts.API_HOST else api
        }
    }
}