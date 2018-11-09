package cool.mixi.dica.util

import android.text.TextUtils
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

        fun stripStatusTextProxyUrl(status: Status) {
            if(status.text == null || status.text.isEmpty()) {
                return
            }

            var urls = status.text.urls()
            urls.forEach {
                while(proxyImagePattern.matcher(it).find()){
                    dLog("MatchProxyImage: ${it}")
                    status.text = status.text.replace(it, "", true)
                    break
                }
            }
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