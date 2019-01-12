package cool.mixi.dica.util

import android.net.Uri
import android.text.TextUtils
import android.util.Patterns
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.bean.Status
import cool.mixi.dica.bean.User
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

class FriendicaUtil {

    companion object {
        private val serviceUnavailable = App.instance.getString(R.string.common_error)
        fun getProxyUrlPartial(originalUrl: String): String{
            var tmpUrl = TextUtils.htmlEncode(originalUrl)
            var longpath = ""
            var base64 =
                String(android.util.Base64.encode(
                    tmpUrl.toByteArray(),
                    android.util.Base64.NO_WRAP), StandardCharsets.UTF_8)
            longpath+="/"+base64.replace("\\+\\/".toRegex(), "-_")
            return try {
                longpath.replace("\n".toRegex(), "").substring(0, 48)
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

        fun filterDuplicateLike(status: Status){
            val ar = ArrayList<User>()
            status.friendica_activities.like.forEach {
                if(!ar.contains(it)) ar.add(it)
            }
            status.friendica_activities.like = ar
        }

        // Hyper link/Image should newline
        // remove url end with /
        // remove duplicate link
        // remove original link that already proxy
        // remove  *site name+link*
        // remove attachment once content include
        // remove duplicate url but different protocol (http & https)
        // remove useless feedburner link
        // attachment process, add extension if not exists
        fun statusPreProcess(status: Status){
            var newStr = status.text
            if(newStr.contains("\\*.*http.*\\*".toRegex())){
                newStr = newStr.replaceFirst("\\*([^*]+)\\*".toRegex(), "")
            }
            newStr = newStr.replace("\\(http([^)]+)\\)".toRegex(), "")
            var matcher = Patterns.WEB_URL.matcher(newStr)
            var displayedUrl = ArrayList<String>()
            while (matcher.find()){
                var url = matcher.group()
                url = url.replace("(\\*|\\))$".toRegex(), "")

                // Email
                if(!url.startsWith("http", true)) continue

                // pod_feeder & github
                if(url.contains("github") && url.contains("pod_feeder")){
                    newStr = newStr.replace(url, "", true)
                    continue
                }

                val decodeUrl = URLDecoder.decode(url, "UTF-8")
                val pureUrl = url.urlEscapeQueryAndHash()
                if(displayedUrl.contains(url)) {
                    newStr = newStr.replaceAfter(url, "")
                } else if(displayedUrl.contains(pureUrl) || displayedUrl.contains(decodeUrl)){
                    newStr = newStr.replace(url, "")
                }

                val proxy2 = FriendicaUtil.getProxyUrlPartial2(url)
                displayedUrl.add(pureUrl)
                displayedUrl.add(proxy2)
                displayedUrl.add(url)
                if(pureUrl.startsWith("http:", true)) displayedUrl.add(pureUrl.replace("http:", "https:"))
                if(pureUrl.startsWith("https:", true)) displayedUrl.add(pureUrl.replace("https:", "http:"))

                // NewLine
                if(!newStr.contains("\n$url", true)) newStr = newStr.replaceFirst(url, "\n$url")
                if(!newStr.contains("$url\n", true)) newStr = newStr.replaceFirst(url, "$url\n")
                val it = status.attachments?.iterator()
                it?.let {
                    while (it.hasNext()){
                        val att = it.next()
                        val urlContain = FriendicaUtil.getProxyUrlPartial(att.url)

                        if(att.url.contains("feed burner", true)) it.remove()
                        if(url.contains(urlContain, true)) it.remove()
                        if(displayedUrl.contains(att.url)) it.remove()
                    }
                }
            }

            status.text = newStr
            status.attachments?.forEach {
                if(displayedUrl.contains(it.url)) return@forEach
                if(!it.mimetype.contains("image")) return@forEach
                if(it.url.contains("\\.(jpg|jpeg|png|gif)$".toRegex())) return@forEach
                it.url = if(it.url.contains("?")) {
                    "${it.url}&ext=.jpg"
                } else {
                    "${it.url}?ext=.jpg"
                }
            }
        }
    }
}