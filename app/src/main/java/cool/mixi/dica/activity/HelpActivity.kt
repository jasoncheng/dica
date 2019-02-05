package cool.mixi.dica.activity

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import com.google.android.material.snackbar.Snackbar
import cool.mixi.dica.R
import kotlinx.android.synthetic.main.activity_help.*

class HelpActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setTitle(R.string.menu_help)

        var wv = findViewById<View>(R.id.helpWebView) as WebView
        wv.loadUrl("file:///android_asset/help.html")

    }
}

