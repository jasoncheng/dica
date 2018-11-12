package cool.mixi.dica.util

import android.text.TextUtils
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.bean.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import javax.net.ssl.HttpsURLConnection


interface ILike {
    fun done()
    fun fail()
}

interface ISeenNotify {
    fun done()
    fun fail()
}

class FriendicaUtil {

    companion object {
        private val proxyImagePattern = Pattern.compile("\\/proxy\\/([a-z0-9]{2})\\/",
            Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL)
        private val serviceUnavailable = App.instance.getString(R.string.common_error)

        fun stripStatusTextProxyUrl(status: Status) {
            if(status.text == null || status.text.isEmpty() || status.attachments == null) {
                return
            }

            // 1. strip proxy image
            // 2. strip attachments image
            var urls = status.text.urls()
            urls.forEach { url ->
                status.attachments.let { it ->
                    it.forEach {
                        if(it.url == url) {
                            status.text = status.text.replace(url, "")
                        }
                    }
                }
                while(proxyImagePattern.matcher(url).find()){
                    dLog("MatchProxyImage: $url")
                    status.text = status.text.replace(url, "", true)
                    break
                }
            }

            status.text = status.text.trim()
        }

        fun getProxyUrlPartial(originalUrl: String): String{
            var tmpUrl = TextUtils.htmlEncode(originalUrl)
            var shortpath = tmpUrl.md5()
            var longpath = shortpath.substring(0, 2)
            var base64 =
                String(android.util.Base64.encode(
                    tmpUrl.toByteArray(),
                    android.util.Base64.URL_SAFE), StandardCharsets.UTF_8)
            longpath+="/"+base64.replace("\\+\\/".toRegex(), "-_")
            return longpath
        }


        fun like(isLike: Boolean, id: Int, callback: ILike) {
            var fn= if(isLike) { ApiService.create()
                .like(id) } else { ApiService.create().unlike(id) }
            fn.enqueue(object: Callback<String> {
                override fun onFailure(call: Call<String>, t: Throwable) {
                    t.message?.let { eLog(it) }
                    callback.fail()
                }

                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if(response.code() == HttpsURLConnection.HTTP_OK
                        && response.body().toString().contains("ok")) {
                        callback.done()
                        return
                    }
                    callback.fail()
                }

            })
        }

        fun favorites(isFavorites: Boolean, id: Int) {
            var fn= if(isFavorites) {
                ApiService.create().favoritesCreate(id)
            } else { ApiService.create().favoritesDestroy(id) }

            fn.enqueue(object: Callback<Status> {
                override fun onResponse(call: Call<Status>, response: Response<Status>) {
                    if(response.code() != HttpsURLConnection.HTTP_OK){
                        eLog("favorites ${response.body()} ${response.errorBody()}")
                        App.instance.toast(serviceUnavailable.format(response.body()))
                    }
                }

                override fun onFailure(call: Call<Status>, t: Throwable) {
                    eLog("favorites ${t?.message}")
                    App.instance.toast(serviceUnavailable.format(t.message))
                }

            })
        }

        fun seen(nid: Int, callback: ISeenNotify?) {
            ApiService.create().friendicaNotificationSeen(nid).enqueue(object : Callback<String>{
                override fun onFailure(call: Call<String>, t: Throwable) {
                    eLog("Seen ${nid} ${t.message}")
                    callback?.fail()
                }

                override fun onResponse(call: Call<String>, response: Response<String>) {
                    dLog("Seen ${nid} ${response.body().toString()}")
                    if(response.code() != HttpsURLConnection.HTTP_OK
                        || !response.body().toString().contains("success")) {
                        callback?.fail()
                        return
                    }
                    callback?.done()
                }

            })
        }
    }
}