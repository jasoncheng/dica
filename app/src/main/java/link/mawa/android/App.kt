package link.mawa.android

import android.app.Application
import android.widget.Toast
import link.mawa.android.bean.Profile

class App: Application() {

    var myself: Profile? = null

    companion object {
        lateinit var instance: App private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}