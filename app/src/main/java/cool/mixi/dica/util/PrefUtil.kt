package cool.mixi.dica.util

import android.content.SharedPreferences
import android.preference.PreferenceManager
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.bean.Consts

class PrefUtil {

    companion object {
        private fun default(): SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(App.instance.applicationContext)

        private val defaultStickerUri = App.instance.getString(R.string.googleStickerUri)!!

        fun resetAll() {
            setApiUrl("")
            setLastStatus("")
            setPassword("")
            setSiteIcon("")
            setSiteName("")
            resetStickerUrl()
            clearSinceId()
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

        fun setTimelineSinceId(fragmentName: String, sinceId: Int){
            val currentSinceId = getTimelineSinceId(fragmentName)
            if(sinceId <= currentSinceId) return

            dLog("save sinceId $fragmentName, $sinceId")
            default().edit().putInt(fragmentName, sinceId).commit()
        }

        fun getTimelineSinceId(fragmentName: String): Int {
            return default().getInt(fragmentName, 0)
        }

        fun getStickerUrl(): String {
            return default().getString("sticker", defaultStickerUri)
        }

        fun resetStickerUrl() {
            setStickerUrl(defaultStickerUri)
        }

        fun setStickerUrl(uri: String){
            default().edit().putString("sticker", uri).commit()
        }

        fun setPollNotification(isEnable: Boolean){
            default().edit().putBoolean("pollNotification", isEnable).commit()
        }

        fun isPollNotification(): Boolean {
            return default().getBoolean("pollNotification", true)
        }

        //TODO: ugly here
        private fun clearSinceId(){
            default().edit().putInt("TimelineFavoritesFragment", 0).apply()
            default().edit().putInt("TimelineFriendsFragment", 0).apply()
            default().edit().putInt("TimelineMyFragment", 0).apply()
            default().edit().putInt("TimelinePublicFragment", 0).apply()
        }
    }
}