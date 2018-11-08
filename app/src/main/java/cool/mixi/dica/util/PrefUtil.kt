package cool.mixi.dica.util

import android.content.SharedPreferences
import android.preference.PreferenceManager
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.bean.Consts

class PrefUtil {

    companion object {
        fun default(): SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(App.instance.applicationContext)

        fun resetAll() {
            setApiUrl("")
            setLastStatus("")
            setPassword("")
            setSiteIcon("")
            setSiteName("")
        }

        fun didSetUserCredential(): Boolean {
            return !getUsername().isEmpty() && !getPassword().isEmpty()
        }

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

        fun setLastStatus(text: String) {
            default().edit().putString("text", text).commit()
        }

        fun getLastStatus(): String {
            return default().getString("text", "")
        }

        fun setSiteName(text: String) {
            default().edit().putString("sitename", text).commit()
        }

        fun getSiteName(): String {
            return default().getString("sitename", App.instance.getString(R.string.app_name))
        }

        fun setSiteIcon(text: String) {
            default().edit().putString("favicon", text).commit()
        }

        fun getSiteIcon(): String {
            return default().getString("favicon", "")
        }
    }
}