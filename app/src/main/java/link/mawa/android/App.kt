package link.mawa.android

import android.app.Application
import android.widget.Toast
import link.mawa.android.bean.Profile
import link.mawa.android.util.ApiService
import link.mawa.android.util.dLog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class App: Application() {

    var myself: Profile? = null

    companion object {
        lateinit var instance: App private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        ApiService.create().unlike(1776).enqueue(object: Callback<String> {
            override fun onFailure(call: Call<String>, t: Throwable) {
                dLog("unLike ${t.message}")
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                dLog("unLike ${response.body().toString()}")
            }

        })
    }

    fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}