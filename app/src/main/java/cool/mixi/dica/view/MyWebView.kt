package cool.mixi.dica.view

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import cool.mixi.dica.App
import cool.mixi.dica.activity.StickerActivity
import cool.mixi.dica.util.PrefUtil
import cool.mixi.dica.util.eLog
import java.lang.ref.SoftReference
import java.net.URLDecoder

interface IStickerPicker {
    fun stickerSelected(url: String)
}

class MyWebView(context: Context, attrs: AttributeSet): WebView(context, attrs){
    private val googleSearch = "https://www.google.com/search?"
    private val ref: SoftReference<StickerActivity> = SoftReference(context as StickerActivity)
    init {
        this.settings.javaScriptEnabled = true
        this.settings.domStorageEnabled = true
        this.settings.setAppCacheEnabled(true)
        this.webViewClient = MyWebViewClient(ref)

        try {
            val uri = Uri.parse(googleSearch+PrefUtil.getStickerUrl()).toString()
            this.loadUrl(uri)
        }catch (e: Exception){ eLog("${e.message}")}
    }
}

class MyWebViewClient(private val ref: SoftReference<StickerActivity>): WebViewClient(){

    private var urlCapture: Boolean = false

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url.toString()
        if(!url?.contains("tbm=isch")){
            App.instance.toast("not support")
            return true
        }
        return false
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        ref.get()?.loaded()

        if(url == null) return
        if (url.indexOf("imgrc=") > -1 && !url.contains("imgrc=_")) {
            urlCapture = true
        }
    }

    override fun onLoadResource(view: WebView?, url: String?) {
        super.onLoadResource(view, url)
        ref.get().let {
            if(it == null) return
        }

        if (!urlCapture) { return }

        url?.let {
            if ((it.contains("google.com") && !it.contains("imgurl"))
                || it.contains("gstatic.com")) {
                return
            }

            var finalImageUrl = it
            if (it.contains("imgurl") && it.contains("google")) {
                val uri = Uri.parse(it)
                val encodeUrl = uri.getQueryParameter("imgurl")
                try {
                    finalImageUrl = URLDecoder.decode(encodeUrl!!, "UTF-8")
                } catch (e: Exception) {}
            }

            ref.get()?.stickerSelected(finalImageUrl)
            urlCapture = false
        }
    }
}