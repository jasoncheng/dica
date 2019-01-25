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

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        setTitle(R.string.menu_help)

        //Using a WebView as it makes it easier to have the links clickable!!
        var wv = findViewById<View>(R.id.helpWebView) as WebView

        //The pre-written text for the help page content
        var dataString =
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n<html>\n<head>\n\t<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\"/>\n\t<title></title>\n\t<meta name=\"generator\" content=\"LibreOffice 6.1.4.2 (Linux)\"/>\n\t<meta name=\"created\" content=\"2019-01-25T17:06:01.977786995\"/>\n\t<meta name=\"changed\" content=\"2019-01-25T17:11:11.882527657\"/>\n\t<style type=\"text/css\">\n\t\t@page { size: 21cm 29.7cm; margin: 2cm }\n\t\tp { margin-bottom: 0.25cm; line-height: 115%; background: transparent }\n\t\th3 { margin-top: 0.25cm; margin-bottom: 0.21cm; background: transparent; page-break-after: avoid }\n\t\th3.western { font-family: \"Liberation Sans\", sans-serif; font-size: 14pt; font-weight: bold }\n\t\th3.cjk { font-family: \"Noto Sans CJK SC Regular\"; font-size: 14pt; font-weight: bold }\n\t\th3.ctl { font-family: \"Lohit Devanagari\"; font-size: 14pt; font-weight: bold }\n\t\ta:link { color: #000080; so-language: zxx; text-decoration: underline }\n\t</style>\n</head>\n<body lang=\"en-AU\" link=\"#000080\" vlink=\"#800000\" dir=\"ltr\"><h3 class=\"western\">\nWelcome to DiCa</h3>\n<p>DiCa is a client for the Friendica Social Media Network.</p>\n<p>The main view contains each of the Friendica Timelines, and can be\nviewed by swiping left or right.</p>\n<p>The Help, Notification and Logout menu’s can be reached by\nclicking on your profile icon in the top right corner of the main\nscreen.</p>\n<p>For further help, or to highlight an issue or feature request,\nplease visit <a href=\"https://github.com/jasoncheng/dica\">https://github.com/jasoncheng/dica</a></p>\n<p><br/>\n<br/>\n\n</p>\n</body>\n</html>"

        wv.loadData(
            dataString,
            "text/html",
            "charset=UTF-8"
        )

    }


}

