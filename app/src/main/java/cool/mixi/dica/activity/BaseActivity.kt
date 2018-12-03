package cool.mixi.dica.activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import cool.mixi.dica.R
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.fragment.ComposeDialogFragment
import cool.mixi.dica.util.StatusTimeline

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
        getComposeDialog()?.let {
            it.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun getComposeDialog(): ComposeDialogFragment? {
        return supportFragmentManager.findFragmentByTag(Consts.FG_COMPOSE) as ComposeDialogFragment
    }
}