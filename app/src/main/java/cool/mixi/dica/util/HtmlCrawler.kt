package cool.mixi.dica.util

import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.jsoup.Jsoup
import org.jsoup.select.Selector

data class Meta(
    var url: String,
    var title: String?,
    var icon: String?,
    var description: String?
)

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

        var meta = Meta(url, null, null, null)
        cached[url] = meta
        doAsync {
            dLog("fetch $url")
            var agent = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"
            val doc = Jsoup.connect(meta.url).userAgent(agent).followRedirects(true).get()
            try {
                meta.title = doc.select("title").text()
            } catch (e: Selector.SelectorParseException) {}
            try {
                meta.description = doc.select("meta[property=og:description]").attr("content")
            }catch (e: Selector.SelectorParseException){}
            try {
                meta.icon = doc.select("meta[property=og:image]").attr("content")
                if(meta.icon == null){
                    meta.icon = doc.select("link[rel=apple-touch-icon]").attr("href")
                }
            } catch (e: Selector.SelectorParseException) {}

            uiThread {
                meta.title?.let {
                    dLog("cache size: ${cached.size}, caching ${meta.url}, $url, ${doc.location()}")
                    callback.done(meta)
                }
            }
        }
    }

    fun get(url: String): Meta? {
        return cached[url]
    }
}