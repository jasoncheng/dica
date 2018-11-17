package cool.mixi.dica.util

import android.util.Log
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.bean.APLink
import cool.mixi.dica.bean.Attachment
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser
import java.net.URL
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.regex.Pattern

private val urlPattern = Pattern.compile(
    "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
            + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
            + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
    Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL)

private val emailPattern = Pattern.compile("([a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+)",
    Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL)

fun Any.eLog(log: String) = Log.e(this::class.java.simpleName, "=====> $log")
fun Any.iLog(log: String) = Log.i(this::class.java.simpleName, "=====> $log")
fun Any.dLog(log: String) = Log.d(this::class.java.simpleName, "=====> $log")
fun String.md5(): String {
    try {
        val instance: MessageDigest = MessageDigest.getInstance("MD5")
        val digest:ByteArray = instance.digest(this.toByteArray())
        var sb = StringBuffer()
        for (b in digest) {
            var i :Int = b.toInt() and 0xff
            var hexString = Integer.toHexString(i)
            if (hexString.length < 2) {
                hexString = "0$hexString"
            }
            sb.append(hexString)
        }
        return sb.toString()

    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    }

    return ""
}

fun String.emailGetDomain(): String{
    return this.substring(this.indexOf("@") + 1)
}

fun String.glideUrl(): GlideUrl {
    val headersBuilder = LazyHeaders.Builder()
    val host = URL(this).host
    headersBuilder.addHeader("user-agent", App.instance.getString(R.string.app_name))
    headersBuilder.addHeader("accept", "*/*")
    headersBuilder.addHeader("Pragma", "no-cache")
    headersBuilder.addHeader("cache-control", "no-cache")
    ApiService.cookies[host]?.let {headersBuilder.addHeader("Cookie", it) }
    return GlideUrl(this, headersBuilder.build())
}

fun String.possibleNetworkAcctFromUrl(): String {
    dLog("possibleNetworkAcctFromUrl $this")
    var uri = URL(this)
    return "${this.substring(this.lastIndexOf("/")+1)}@${uri.host}"
}

// TODO: ugly.....too too ugly; need to do refactor
fun String.dicaHTMLFilter(): String {
    var sb = StringBuffer()
    var linkExists = ArrayList<String>()
    var textExists = ArrayList<String>()
    var ele = Jsoup.parse(
        Parser.unescapeEntities(this, true)).body().allElements
    for(e in ele){
        for(e1 in e.childNodes()){
            if(e1 is TextNode){
                if(e1.parent().nodeName() == "a"){
                    continue
                }
                if(textExists.contains(e1.text())){
                    continue
                }
                sb.append(e1.text())
                textExists.add(e1.text())
                continue
            }

            if(e1 !is Element) continue

            var tag = e1.tagName()
            if(tag == "a"){
                val link = e1.attr("href")
                if(link.contains("/tags/") || link.contains("search?tag")){
                    sb.append(e1.text())
                    continue
                }
                if(linkExists.contains(link)){ continue }
                sb.append(" $link ")
                linkExists.add(link)
            } else if(tag == "img") {
                var link = e1.attr("src")
                // do something on link, that adapter render, can treat as image
                if(!link.contains("?")){
                    link += "?ext=.jpg"
                } else if(link.contains("&")) {
                    link += "&ext=.jpg"
                }
                if(linkExists.contains(link)){ continue }
                sb.append(" $link ")
                linkExists.add(link)
            } else if(tag == "strong" || tag == "b" || tag == "h3" || tag == "h2" || tag == "h1") {
                sb.append("*${e1.text()}*")
            } else if(tag == "br" || tag == "hr") {
                sb.append("\n")
            }
        }
    }
    linkExists.clear()
    textExists.clear()
    return sb.toString()
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

fun String.emails(): ArrayList<String> {
    var emails = ArrayList<String>()
    val matcher = emailPattern.matcher(this)
    while (matcher.find()) {
        emails.add(matcher.group())
    }
    return emails
}

fun List<APLink>.getAvatar(): String {
    for(it in this){
        if(it.rel == "avatar"){
            return it.href
        }
    }
    return ""
}

fun List<APLink>.toAttachments(): ArrayList<Attachment> {
    val ar = ArrayList<Attachment>()
    for(it in this){
        if(!it.rel.contains("enclosure".toRegex())){
            continue
        }
        var attachment = Attachment()
        attachment.url = it.href
        attachment.mimetype = it.type
        ar.add(attachment)
    }
    return ar
}