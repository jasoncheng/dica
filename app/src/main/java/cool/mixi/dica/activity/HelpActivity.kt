package cool.mixi.dica.activity

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import com.google.android.material.snackbar.Snackbar
import cool.mixi.dica.R
import kotlinx.android.synthetic.main.activity_help.*

/* Simple Menu item using the WebView class to allow for quick changes to text by using a HTML Editor*/

class HelpActivity : BaseActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setTitle(R.string.menu_help)

        /*Using a WebView as it makes it easier to have the links clickable!!*/
        var wv = findViewById<View>(R.id.helpWebView) as WebView

        /*The pre-written text for the help page content from @string/menu_help_text*/
        var dataString = getString(R.string.menu_help_text)

        wv.loadData(
            dataString,
            "text/html",
            "charset=UTF-8"
        )

    }


}

