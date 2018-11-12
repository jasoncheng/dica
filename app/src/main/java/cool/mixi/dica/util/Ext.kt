package cool.mixi.dica.util

import android.util.Log
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import cool.mixi.dica.App
import cool.mixi.dica.R
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.regex.Pattern


private val urlPattern = Pattern.compile(
    "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
            + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
            + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
    Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL)

fun Any.eLog(log: String) = Log.e(this::class.java.simpleName, "-----> $log")
fun Any.iLog(log: String) = Log.i(this::class.java.simpleName, "-----> $log")
fun Any.dLog(log: String) = Log.d(this::class.java.simpleName, "-----> $log")

fun String.md5(): String {
    try {
        val instance: MessageDigest = MessageDigest.getInstance("MD5")
        val digest:ByteArray = instance.digest(this.toByteArray())
        var sb = StringBuffer()
        for (b in digest) {
            var i :Int = b.toInt() and 0xff
            var hexString = Integer.toHexString(i)
            if (hexString.length < 2) {
                hexString = "0" + hexString
            }
            sb.append(hexString)
        }
        return sb.toString()

    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    }

    return ""
}

fun String.glideUrl(): GlideUrl {
    dLog(this)
    val headersBuilder = LazyHeaders.Builder()
    headersBuilder.addHeader("user-agent", App.instance.getString(R.string.app_name))
    headersBuilder.addHeader("accept", "*/*")
    headersBuilder.addHeader("Pragma", "no-cache")
    headersBuilder.addHeader("cache-control", "no-cache")
    ApiService.sessionCookie?.let {headersBuilder.addHeader("Cookie", it) }
    return GlideUrl(this, headersBuilder.build())
}

fun String.urls(): ArrayList<String> {
    var urls = ArrayList<String>()
    val matcher = urlPattern.matcher(this)
    while (matcher.find()) {
        val matchStart = matcher.start(1)
        val matchEnd = matcher.end()
        urls.add(this.substring(matchStart, matchEnd))
    }
    return urls
}
