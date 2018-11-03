package link.mawa.android.activity

import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.activity_main.*
import link.mawa.android.App
import link.mawa.android.R
import link.mawa.android.adapter.StatusesAdapter
import link.mawa.android.bean.Consts
import link.mawa.android.bean.Profile
import link.mawa.android.fragment.ComposeDialogFragment
import link.mawa.android.util.ApiService
import link.mawa.android.util.PrefUtil
import link.mawa.android.util.dLog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference
import javax.net.ssl.HttpsURLConnection

class MainActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(App.instance.myself == null){
            if(!PrefUtil.didSetUserCredential()){
                logout()
                return
            }

            try {
                ApiService.create().friendicaProfileShow(null).enqueue(ProfileCallback(this))
            }catch (e: Exception){
                dLog("${e.message}")
                App.instance.toast(getString(R.string.login_error).format("${e.message}"))
                logout()
                return
            }
        }

        setContentView(R.layout.activity_main)

        // RecyclerView
        rv_statuses_list.layoutManager = LinearLayoutManager(this)
        rv_statuses_list.adapter = StatusesAdapter(statuses, this)
        rv_statuses_list.setOnScrollListener(OnStatusTableScrollListener(this))

        // Refresh
        home_srl.setOnRefreshListener(this)

        // Title
        val myTypeface = Typeface.createFromAsset(assets, "Hand_Of_Sean_Demo.ttf")
        home_title.typeface = myTypeface
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) run { home_title.letterSpacing = 1f }

        // compose
        iv_compose.setOnClickListener {
            ComposeDialogFragment().show(supportFragmentManager, Consts.FG_COMPOSE)
        }

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

        setAvatar()

        if(App.instance.myself != null){
            loadMoreStatues()
        }
    }

    private fun setAvatar(){
        Glide.with(applicationContext)
            .load(App.instance.myself?.friendica_owner?.profile_image_url_large)
            .apply(RequestOptions().circleCrop())
            .into(home_avatar)
    }

    private fun logout() {
        App.instance.myself = null
        PrefUtil.setApiUrl("")
        PrefUtil.setUsername("")
        goLogin()
        finish()
    }

    private fun goLogin(){
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
                act.logout()
                return
            }

            App.instance.myself = response.body()
            act.setAvatar()
            act.loadMoreStatues()
        }

    }

    override fun onRefresh() {
        loadNewestStatuses()
    }

}