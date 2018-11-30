package cool.mixi.dica.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import cool.mixi.dica.R
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.util.PrefUtil
import cool.mixi.dica.util.eLog
import cool.mixi.dica.view.IStickerPicker
import kotlinx.android.synthetic.main.activity_sticker.*

class StickerActivity: BaseActivity(), IStickerPicker {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sticker)
        loading()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveLastUrl()
    }

    private fun saveLastUrl() {
        var uri: Uri? = null
        try {
            uri = Uri.parse(web?.url)
        } catch (e: Exception) { eLog("${e.message}") }

        if (uri?.getQueryParameter("q") == null || uri.getQueryParameter("q") == "null") {
            PrefUtil.resetStickeUrl()
            return
        }

        val searchTerm = "q=" + uri.getQueryParameter("q") +
                "&tbm=" + uri.getQueryParameter("tbm") +
                "&tbs=" + uri.getQueryParameter("tbs")
        PrefUtil.setStickerUrl(searchTerm)
    }

    fun loading(){
        pb.visibility = View.VISIBLE
    }

    override fun loaded(){
        pb.visibility = View.GONE
    }

    override fun stickerSelected(url: String) {
        var i = Intent()
        i.putExtra(Consts.EXTRA_STICKER_URI, url)
        setResult(Activity.RESULT_OK, i)
        finish()
    }
}