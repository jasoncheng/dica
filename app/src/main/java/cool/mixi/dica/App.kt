package cool.mixi.dica

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.widget.Toast
import cool.mixi.dica.bean.*
import cool.mixi.dica.database.AppDatabase
import cool.mixi.dica.util.ApiService
import cool.mixi.dica.util.dLog
import org.jetbrains.anko.doAsync
import pl.aprilapps.easyphotopicker.EasyImage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class App: Application() {

    var notifications: ArrayList<Notification> = ArrayList()
    var myself: Profile? = null
    var mygroup: ArrayList<Group>? = null
    var selectedGroup: ArrayList<Int> = ArrayList()
    var webFingerUrlCache: HashMap<String, String> = HashMap()
    var cachedUser: ArrayList<User> = ArrayList()

    var mediaUris: ArrayList<String> = ArrayList()

    companion object {
        lateinit var instance: App private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        EasyImage.configuration(this).setAllowMultiplePickInGallery(true)
        doAsync {
            val tags = AppDatabase.getInstance().hashTagDao().getAll()
            tags?.forEach {
                dLog("TAG $it")
            }

//            val metas = AppDatabase.getInstance().metaDao().getAll()
//            metas?.forEach {
//                dLog("META $it")
//            }
//
//            val users = AppDatabase.getInstance().userDao().getAll()
//            users?.forEach {
//                dLog("USER $it")
//            }
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

    fun clear(){
        mediaUris.clear()
    }

    fun checkIfRequireClearAllNotification(){
        if(getUnSeenNotificationCount() > 0) return
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
    }

    fun addNotification(data: List<Notification>){
        notifications.clear()
        data.forEach {
            notifications.add(it)
        }
    }

    fun getUnSeenNotificationCount(): Int {
        var c = 0
        notifications.forEach {
            if(it.seen == 0) c++
        }
        return c
    }

}