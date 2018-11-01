package link.mawa.android.activity

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import link.mawa.android.R

open class BaseActivity: AppCompatActivity() {

    var alertDialog:AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun loading(message: String) {
        var builder = AlertDialog.Builder(this)
        var view = layoutInflater.inflate(R.layout.loading_dialog, null)
        view.findViewById<TextView>(R.id.tv_loading).text = message
        builder.setCancelable(true)
        builder.setView(view)
        alertDialog = builder.show()
    }

    fun loaded(){
        alertDialog?.dismiss()
    }
}