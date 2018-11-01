package link.mawa.android.activity

import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.activity_main.*
import link.mawa.android.App
import link.mawa.android.R
import link.mawa.android.adapter.StatusesAdapter
import link.mawa.android.bean.Profile
import link.mawa.android.bean.Status
import link.mawa.android.util.ApiService
import link.mawa.android.util.PrefUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference
import javax.net.ssl.HttpsURLConnection

class MainActivity : BaseActivity() {

    val tag = this.javaClass.simpleName!!
    var statuses = ArrayList<Status>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val callbackProfile = ProfileCallback(this)

        // get user profile
        try {
            ApiService.create().friendicaProfileShow().enqueue(callbackProfile)
        }catch (e: Exception){
            Log.e(tag, "${e.message}")
            App.instance.toast(getString(R.string.login_error).format("${e.message}"))
            logout()
            return
        }

        // RecyclerView
        rv_statuses_list.layoutManager = LinearLayoutManager(this)
        rv_statuses_list.adapter = StatusesAdapter(statuses, this)

        // Title
        val myTypeface = Typeface.createFromAsset(assets, "Hand_Of_Sean_Demo.ttf")
        home_title.typeface = myTypeface
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) run { home_title.letterSpacing = 1f }

        // avatar
        home_avatar.setOnClickListener {
            var pop = PopupMenu(this, it)
            var inflater = pop.menuInflater
            inflater.inflate(R.menu.index_avatar_menu, pop.menu)
            pop.setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.menu_logout -> logout()
                }
                true
            }
            pop.show()
        }
    }

    private fun logout() {
        PrefUtil.setApiUrl("")
        PrefUtil.setUsername("")
        goLogin()
        finish()
    }

    fun loadStatuses(){
        val callback = MyCallback(this)
        ApiService.create().statusPublicTimeline("0", "0").enqueue(callback)
    }

    fun goLogin(){
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    class ProfileCallback(activity: MainActivity): Callback<Profile> {
        private val ref = WeakReference<MainActivity>(activity)
        override fun onFailure(call: Call<Profile>, t: Throwable) {
            if(ref.get() == null){ return}
        }

        override fun onResponse(call: Call<Profile>, response: Response<Profile>) {
            if(ref.get() == null){ return}
            val act = ref.get()!!
            if( response.code() == HttpsURLConnection.HTTP_UNAUTHORIZED ){
                act.goLogin()
                return
            }

            val profile = response.body()
            Glide.with(act.applicationContext)
                .load(profile?.friendica_owner?.profile_image_url_large)
                .apply(RequestOptions().circleCrop())
                .into(act.home_avatar)

            act.loadStatuses()
        }

    }

    class MyCallback(activity: MainActivity): Callback<List<Status>> {

        private val ref = WeakReference<MainActivity>(activity)
        override fun onFailure(call: Call<List<Status>>, t: Throwable) {
            if(ref.get() == null){
                return
            }
        }

        override fun onResponse(call: Call<List<Status>>, response: Response<List<Status>>) {
            if(ref.get() == null){
                return
            }

            val act = ref.get()!!
            val res = response.body()
            res?.forEach {
                act.statuses.add(it)
                Log.i(act.tag, it.toString())
            }
            act.rv_statuses_list.adapter.notifyDataSetChanged()
        }
    }
}
