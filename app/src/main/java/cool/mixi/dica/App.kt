package cool.mixi.dica

import android.app.Application
import android.widget.Toast
import cool.mixi.dica.bean.Group
import cool.mixi.dica.bean.Profile
import cool.mixi.dica.bean.Status
import cool.mixi.dica.bean.User
import cool.mixi.dica.database.AppDatabase
import cool.mixi.dica.util.ApiService
import cool.mixi.dica.util.dLog
import org.jetbrains.anko.doAsync
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.collections.HashMap



class App: Application() {

    var myself: Profile? = null
    var mygroup: ArrayList<Group>? = null
    var selectedGroup: ArrayList<Int> = ArrayList()
    var webFingerUrlCache: HashMap<String, String> = HashMap()
    var cachedUser: ArrayList<User> = ArrayList()

    companion object {
        lateinit var instance: App private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        loadGroup()

        // Clean Data
        doAsync {
            AppDatabase.getInstance().metaDao().expireClean()
            AppDatabase.getInstance().userDao().expireClean()
            dLog("User info #${AppDatabase.getInstance().userDao().count()}")
            dLog("Meta info #${AppDatabase.getInstance().metaDao().count()}")
        }
    }

    fun getWebFinger(email: String): String? {
        webFingerUrlCache[email].let {
            dLog("webFinger cached? $email $it")
            return it
        }
    }

    fun setWebFinger(email: String, atomUrl: String){
        dLog("webFinger caching $email $atomUrl")
        webFingerUrlCache[email] = atomUrl
    }

    fun loadGroup(){
        ApiService.create().friendicaGroupShow().enqueue(object: Callback<ArrayList<Group>>{
            override fun onFailure(call: Call<ArrayList<Group>>, t: Throwable){}
            override fun onResponse(call: Call<ArrayList<Group>>, response: Response<ArrayList<Group>>) {
                if(response.code() != HttpsURLConnection.HTTP_OK || response.body() == null){
                    return
                }
                mygroup = response.body()!!
            }
        })
    }

    fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun addUserToDB(statuses: List<Status>){
        val userDao = AppDatabase.getInstance().userDao()
        val calendar = Calendar.getInstance()
        doAsync {
            statuses.forEach {
                if(cachedUser.contains(it.user)){
                    return@forEach
                }
                var user = it.user
                user.updatedAt = calendar.time
                dLog("addUserToDB: ${user.screen_name} ${user.url}")
                cachedUser.add(user)
                userDao.upsert(user)
            }
        }
    }
}