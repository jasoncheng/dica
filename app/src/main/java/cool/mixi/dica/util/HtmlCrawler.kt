package cool.mixi.dica.util

import android.util.Patterns
import cool.mixi.dica.bean.Meta
import cool.mixi.dica.database.AppDatabase
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import java.util.*

interface IHtmlCrawler {
    fun done(meta: Meta)
}

open class HtmlCrawler {

    private var cached = HashMap<String, Meta>()
    companion object {
        val ignoreExt = "\\.(mp4|mp3|flv|avi|3gpp|3gp|mpg|mpeg|rmvb|vob|webm|mov|xls|ppt)$".toRegex()
        @Volatile private var instance: HtmlCrawler? = null
        fun getInstance(): HtmlCrawler {
            return instance?: synchronized(this){
                HtmlCrawler().also {
                    instance = it

                    doAsync {
                        dLog("Meta total ${AppDatabase.getInstance().metaDao().count()}")
//                        AppDatabase.getInstance().metaDao().expire(AppDatabase.getDefaultExpire())?.forEach {
//                            dLog("Meta delete ${it.url} ${it.created}")
//                        }
                        AppDatabase.getInstance().metaDao().expireClean()
                    }
                }
            }
        }
    }

    fun run(url: String, callback: IHtmlCrawler) {
        // Not support extension
        if(url.contains(ignoreExt)){
            return
        }

        // Filter invalid URL
        if(!Patterns.WEB_URL.matcher(url).matches()) {
            iLog("ignore url $url")
            return
        }

        cached[url]?.let {
            dLog("cached memory $url")
            return callback.done(it)
        }

        var meta = Meta(url, null, null, null, Calendar.getInstance().time)
        var baseUri = url.getBaseUri()
        cached[url] = meta

        doAsync {
            var metaDao = AppDatabase.getInstance().metaDao().get(url)
            if(metaDao != null){
                meta.copy(metaDao)
            } else {
                dLog("fetch $url")
                var agent = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"
                var doc: Document?
                val uri = URL(url)
                try {
                    doc = Jsoup.connect(meta.url).userAgent(agent).followRedirects(true).get()
                    meta.title = doc.select("title").text()
                    meta.description = doc?.select("meta[property=og:description]")?.attr("content")
                    meta.icon = doc?.select("meta[property=og:image]")?.attr("content")
                    if(meta.icon == null){
                        meta.icon = doc?.select("link[rel=apple-touch-icon]")?.attr("href")
                    }
                    if(meta.icon == null){
                        meta.icon = doc?.select("meta[name=msapplication-TileImage]")?.attr("content")
                    }
                    meta.icon?.let {
                        if(it.startsWith("//")) {
                            meta.icon = "${uri.protocol}:$it"
                        } else if(it.startsWith("/")){
                            meta.icon = "$baseUri$it"
                        }
                    }

                } catch (e: Exception) {
                    eLog("${e.message}")
                } finally {
                    AppDatabase.getInstance().metaDao().add(meta)
                }
            }


            uiThread {
                meta.icon?.let { callback.done(meta) }
            }
        }
    }

    fun get(url: String): Meta? {
        return cached[url]
    }
}