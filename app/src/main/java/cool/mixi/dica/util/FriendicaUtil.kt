package cool.mixi.dica.util

import android.net.Uri
import android.text.TextUtils
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.bean.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection


interface ILike {
    fun done()
    fun fail()
}

interface ISeenNotify {
    fun done()
    fun fail()
}

interface IRetweet {
    fun done()
    fun fail(reason: String)
}

class FriendicaUtil {

    companion object {
        private val serviceUnavailable = App.instance.getString(R.string.common_error)

        //TODO:
        // URL1: https://MELD.de/proxy/91/aHR0cHM6Ly93d3cuaGFuZGVsc2JsYXR0LmNvbS9pbWFnZXMvd2VsdGN1cC8yMzcxNjI1NC8xLWZvcm1hdDIwMDIuanBn.jpg
        // URL2: https://MELD.de/proxy/24/aHR0cHM6Ly93d3cuaGFuZGVsc2JsYXR0LmNvbS9pbWFnZXMvd2VsdGN1cC8yMzcxNjI1NC8xLWZvcm1hdDcuanBnP2Zvcm1hdD1mb3JtYXQ3.jpg
        // URL3: https://MELD.de/proxy/4a6e66e6d7695de4607fe32888fc1357?url=https%3A%2F%2Fpod.geraspora.de%2Fcamo%2F
        fun getProxyUrlPartial(originalUrl: String): String{
            var tmpUrl = TextUtils.htmlEncode(originalUrl)
            var shortpath = tmpUrl.md5()
//            var longpath = shortpath.substring(0, 2)
            var longpath = ""
            var base64 =
                String(android.util.Base64.encode(
                    tmpUrl.toByteArray(),
                    android.util.Base64.NO_WRAP), StandardCharsets.UTF_8)
            longpath+="/"+base64.replace("\\+\\/".toRegex(), "-_")
            return try {
                longpath.replace("\n".toRegex(), "").substring(0, 64)
            }catch (e: Exception){
                longpath
            }
        }

        fun getProxyUrlPartial2(proxyUrl: String): String {
            return try {
                var uri = Uri.parse(proxyUrl)
                URLDecoder.decode(uri.getQueryParameter("url"),  "UTF-8")
            }catch (e: Exception){
                ""
            }
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
            } else {
                ApiService.create().favoritesDestroy(id)
            }

            fn.enqueue(object: Callback<Status> {
                override fun onResponse(call: Call<Status>, response: Response<Status>) {
                    if(response.code() != HttpsURLConnection.HTTP_OK){
                        eLog("favorites $id ${response.body()} ${response.code()} ${response.message()}")
                        App.instance.toast(serviceUnavailable.format("id $id code ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<Status>, t: Throwable) {
                    eLog("favorites $id ${t?.message}")
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
                    if(response.code() != HttpsURLConnection.HTTP_OK
                        || !response.body().toString().contains("success")) {
                        callback?.fail()
                        return
                    }
                    callback?.done()
                }

            })
        }

        fun retweet(nid: Int, callback: IRetweet?) {
            ApiService.create().statusRetweet(nid).enqueue(object : Callback<Status>{
                override fun onFailure(call: Call<Status>, t: Throwable) {
                    eLog("retweet ${nid} ${t.message}")
                    callback?.fail("${t.message}")
                }

                override fun onResponse(call: Call<Status>, response: Response<Status>) {
                    if(response.code() != HttpsURLConnection.HTTP_OK) {
                        dLog(response.message())
                        callback?.fail(response.code().toString())
                        return
                    }
                    callback?.done()
                }

            })
        }
    }
}