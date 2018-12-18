package cool.mixi.dica.util

import android.util.Log
import android.util.Patterns
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

fun Any.eLog(log: String) = Log.e(this::class.java.simpleName, "DICA $log")
fun Any.iLog(log: String) = Log.i(this::class.java.simpleName, "DICA $log")
fun Any.dLog(log: String) = Log.d(this::class.java.simpleName, "DICA $log")
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

fun String.urlEscapeQueryAndHash(): String {
    val endPos = when {
        this.indexOf("?") > 0 -> this.indexOf("?")
        this.indexOf("#") > 0 -> this.indexOf("#")
        else -> this.length
    }
    return this.substring(0, endPos)
}

fun String.emailGetDomain(): String{
    return this.substring(this.indexOf("@") + 1)
}

fun String.glideUrl(): GlideUrl {
    val headersBuilder = LazyHeaders.Builder()
    val host = URL(this).host.toLowerCase()
    headersBuilder.addHeader("user-agent", App.instance.getString(R.string.app_name))
    headersBuilder.addHeader("accept", "*/*")
    ApiService.cookies[host]?.let { headersBuilder.addHeader("Cookie", it) }
    return GlideUrl(this, headersBuilder.build())
}

fun String.possibleNetworkAcctFromUrl(): String {
    var uri = URL(this)
    return "${this.substring(this.lastIndexOf("/")+1)}@${uri.host}"
}

fun String.getBaseUri(): String {
    try {
        var uri = URL(this)
        return "${uri.protocol}://${uri.host}"
    } catch (e: Exception) {
        return ""
    }
}

// TODO: my god.....ugly.....too ugly; need to do refactor
fun String.dicaHTMLFilter(toBBCode: Boolean, baseUri: String): String {
    var sb = StringBuffer()
    var linkExists = ArrayList<String>()
    var textExists = ArrayList<String>()
    var ele = Jsoup.parse(
        Parser.unescapeEntities(this, true),
        baseUri
    ).body().allElements
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
                var link = e1.attr("href")
                if(link.contains("/tags/", true) || link.contains("search?tag", true)){
                    if(e1.text() == link){ continue }
                    sb.append(" ${e1.text()} ")
                    continue
                }

                // relative URL to absolute URL
                if(link.startsWith("/")){
                    link = "$baseUri$link"
                }

                if(linkExists.contains(link)){ continue }


                if(toBBCode){
                    sb.append(" [url=$link]${e1.text()}[/url] ")
                } else {
                    sb.append(" $link ")
                }

                linkExists.add(link)

            } else if(tag == "img") {
                var link = e1.attr("src")
                link += if(link.contains("?", true)){
                    "&ext=.jpg"
                } else {
                    "?ext=.jpg"
                }

                // relative URL to absolute URL
                if(link.startsWith("/")){
                    link = "$baseUri$link"
                }

                if(linkExists.contains(link)){ continue }

                if(toBBCode){
                    sb.append(" [img=$link][/img] ")
                } else {
                    sb.append(" $link ")
                }
                linkExists.add(link)
            } else if(tag == "strong" || tag == "b" || tag == "h3" || tag == "h2" || tag == "h1") {
                if(toBBCode){
                    sb.append("[b]${e1.text()}[/b]")
                } else {
                    sb.append("*${e1.text()}*")
                }
            } else if(tag == "br" || tag == "hr") {
                sb.append("\n")
            }
        }
    }
    linkExists.clear()
    textExists.clear()
    return sb.toString()
}

// 1. If URL or Image w/ newline
// 2. ignore duplicate url
// 3. *site title + url*
fun String.dicaRenderData(): String {

    // strip *site title+url*
    var newStr = this
    if(newStr.contains("\\*".toRegex()) && newStr.contains("http")){
        newStr = newStr.replace("\\*([^*]+)\\*".toRegex(), "")
    }

    val ar = ArrayList<String>()
    newStr.lines().forEach {
        var tmp = it
        var matcher = Patterns.WEB_URL.matcher(it)
        while (matcher.find()){
            val url = matcher.group()
            var newUrl = url.trim()

            if(!url.startsWith("http")) continue
            if(it == url) continue

            if(it.endsWith("*")){
                tmp = tmp.replace("*", "")
                continue
            }

            if("*$url" == it){
                tmp = tmp.replace("*", "")
                continue
            }

            if(matcher.start() > 0) newUrl = "\n$newUrl"
            if(matcher.end() < it.length) newUrl = "$newUrl\n"
            tmp = tmp.replace(url, newUrl)
        }
        ar.add(tmp)
    }
    val final = ar.joinToString("\n")
    ar.clear()
    dLog("output $final")
    return final
}

fun String.emails(): ArrayList<String> {
    var emails = ArrayList<String>()
    val matcher = Patterns.EMAIL_ADDRESS.matcher(this)
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