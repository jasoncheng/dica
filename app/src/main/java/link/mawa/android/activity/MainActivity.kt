package link.mawa.android.activity

import android.content.Intent
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
import link.mawa.android.util.*
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
                App.instance.toast(getString(R.string.common_error).format("${e.message}"))
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
            loadMore()
        }


        // TODO: update site information (no API)
        val homeName = PrefUtil.getSiteName()
        if(!homeName.isNullOrEmpty() && homeName != getString(R.string.app_name)){
            home_title?.text = homeName
        } else {
            HtmlCrawler.run(PrefUtil.getApiUrl(), MyHtmlCrawler(this))
        }
    }

    class MyHtmlCrawler(val activity: MainActivity): IHtmlCrawler {
        private val ref = WeakReference<MainActivity>(activity)
        override fun done(meta: Meta) {
            if(!meta.title.isNullOrEmpty()){
                PrefUtil.setSiteName(meta.title!!)
                if(ref.get() != null){
                    ref.get()?.home_title?.text = meta.title
                }
            }
            if(!meta.icon.isNullOrEmpty()){
                PrefUtil.setSiteIcon(meta.icon!!)
            }
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
            act.loadMore()
        }

    }

    override fun loadMore() {
        super.loadMore()
        ApiService.create().statusPublicTimeline("", "${maxId}")
            .enqueue(StatuesCallback(this, false, null))
    }

    override fun loadNewest() {
        super.loadNewest()
        ApiService.create().statusPublicTimeline("${sinceId}", "")
            .enqueue(StatuesCallback(this, true, null))
    }

    override fun onRefresh() {
        loadNewest()
    }
}