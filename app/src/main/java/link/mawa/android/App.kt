package link.mawa.android

import android.app.Application
import android.widget.Toast
import link.mawa.android.bean.Group
import link.mawa.android.bean.Profile
import link.mawa.android.util.ApiService
import link.mawa.android.util.dLog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import javax.net.ssl.HttpsURLConnection



class App: Application() {

    // Caching
    var myself: Profile? = null
    var mygroup: ArrayList<Group>? = null
    var selectedGroup: ArrayList<Int> = ArrayList()

    companion object {
        lateinit var instance: App private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        loadGroup()
    }

    fun loadGroup(){
        ApiService.create().friendicaGroupShow().enqueue(object: Callback<ArrayList<Group>>{
            override fun onFailure(call: Call<ArrayList<Group>>, t: Throwable) {
            }

            override fun onResponse(call: Call<ArrayList<Group>>, response: Response<ArrayList<Group>>) {
                if(response.code() != HttpsURLConnection.HTTP_OK || response.body() == null){
                    return
                }
                mygroup = response.body()!!
                dLog(mygroup.toString())
            }
        })
    }

    fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}