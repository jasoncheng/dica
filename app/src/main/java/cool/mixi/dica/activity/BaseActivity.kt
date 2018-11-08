package link.mawa.android.activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import link.mawa.android.R
import link.mawa.android.bean.Consts
import link.mawa.android.util.StatusTimeline

open class BaseActivity: AppCompatActivity() {

    var stl: StatusTimeline? = null
    var alertDialog:AlertDialog? = null

    fun loading(message: String) {
        var builder = AlertDialog.Builder(this)
        var view = layoutInflater.inflate(R.layout.loading_dialog, null)
        view.findViewById<TextView>(R.id.tv_loading).text = message
        builder.setCancelable(true)
        builder.setView(view)
        alertDialog = builder.show()
    }

    open fun loaded(){
        alertDialog?.dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        supportFragmentManager.findFragmentByTag(Consts.FG_COMPOSE).onActivityResult(requestCode, resultCode, data)
    }
}