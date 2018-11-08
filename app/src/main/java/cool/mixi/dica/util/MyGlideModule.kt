package cool.mixi.dica.util

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.*
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader
import com.bumptech.glide.module.AppGlideModule
import okhttp3.Credentials
import java.io.InputStream


@GlideModule
class MyGlideModule : AppGlideModule() {

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry?.append(String::class.java,InputStream::class.java,
            HeaderLoader.Factory()
        )
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }

    class HeaderLoader(concreteLoader: ModelLoader<GlideUrl, InputStream>?) :
        BaseGlideUrlLoader<String>(concreteLoader) {
        override fun handles(model: String): Boolean {
            return true
        }

        override fun getUrl(model: String?, width: Int, height: Int, options: Options?): String {
            return model!!
        }

        override fun getHeaders(model: String?, width: Int, height: Int, options: Options?): Headers? {
            val headersBuilder = LazyHeaders.Builder()
            headersBuilder.addHeader("Authorization", Credentials.basic(
                PrefUtil.getUsername(),
                PrefUtil.getUsername()
            ))
            headersBuilder.addHeader("Referer", "https://mawa.link")
            headersBuilder.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36")
            return headersBuilder.build()
//            return super.getHeaders(model, width, height, options)
        }

        class Factory: ModelLoaderFactory<String, InputStream> {
            override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<String, InputStream> {
                val loader = multiFactory?.build(GlideUrl::class.java, InputStream::class.java)
                return HeaderLoader(loader)
            }

            override fun teardown() {
            }
        }

    }

}