package cool.mixi.dica.activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import cool.mixi.dica.R
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.fragment.ComposeDialogFragment
import cool.mixi.dica.util.StatusTimeline

open class BaseActivity: AppCompatActivity() {

    var stl: StatusTimeline? = null
    var alertDialog:AlertDialog? = null
    var loadingText: TextView? = null
    fun loading(message: String) {
        var builder = AlertDialog.Builder(this)
        var view = layoutInflater.inflate(R.layout.loading_dialog, null)
        loadingText = view.findViewById(R.id.tv_loading)
        loadingText?.text = message
        builder.setCancelable(true)
        builder.setView(view)
        alertDialog = builder.show()
    }

    open fun loaded(){
        alertDialog?.dismiss()
    }

    open fun loadingState(txt: String){
        loadingText?.let {
            it.text = txt
        }
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

    private fun getComposeDialog(): ComposeDialogFragment? {
        return supportFragmentManager.findFragmentByTag(Consts.FG_COMPOSE) as ComposeDialogFragment
    }
}