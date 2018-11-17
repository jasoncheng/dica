package cool.mixi.dica.util

import android.util.Patterns
import cool.mixi.dica.bean.Meta
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

interface IHtmlCrawler {
    fun done(meta: Meta)
}

open class HtmlCrawler {

    private var cached = HashMap<String, Meta>()

    companion object {
        @Volatile private var instance: HtmlCrawler? = null
        fun getInstance(): HtmlCrawler {
            return instance?: synchronized(this){
                HtmlCrawler().also { instance = it }
            }
        }
    }

    fun run(url: String, callback: IHtmlCrawler) {
        cached[url]?.let {
            callback.done(it)
            return
        }

        if(!Patterns.WEB_URL.matcher(url).matches()) {
            iLog("ignore url $url")
            return
        }

        var meta = Meta(url, null, null, null)
        cached[url] = meta
        doAsync {
            dLog("fetch $url")
            var agent = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"
            var doc: Document? = null
            try {
                doc = Jsoup.connect(meta.url).userAgent(agent).followRedirects(true).get()
                meta.title = doc.select("title").text()
                meta.description = doc?.select("meta[property=og:description]")?.attr("content")
                meta.icon = doc?.select("meta[property=og:image]")?.attr("content")
                if(meta.icon == null){
                    meta.icon = doc?.select("link[rel=apple-touch-icon]")?.attr("href")
                }
            } catch (e: Exception) {}

            uiThread {
                meta.title?.let {
                    dLog("cache size: ${cached.size}, caching ${meta.url}, $url, ${doc?.location()}")
                    callback.done(meta)
                }
            }
        }
    }

    fun get(url: String): Meta? {
        return cached[url]
    }
}