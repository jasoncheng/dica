package cool.mixi.dica.activity

import android.content.Intent
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.widget.PopupMenu
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.adapter.IndexPageAdapter
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.bean.Profile
import cool.mixi.dica.fragment.ComposeDialogFragment
import cool.mixi.dica.util.*
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference
import javax.net.ssl.HttpsURLConnection

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // if user not login yet
        if(App.instance.myself == null){
            if(!PrefUtil.didSetUserCredential()){
                logout()
                return
            }

            try {
                ApiService.create().friendicaProfileShow(null).enqueue(
                    ProfileCallback(
                        this
                    )
                )
            }catch (e: Exception){
                dLog("${e.message}")
                App.instance.toast(getString(R.string.common_error).format("${e.message}"))
                logout()
                return
            }
        }

        setContentView(R.layout.activity_main)

        // compose
        iv_compose.setOnClickListener {
            ComposeDialogFragment().show(supportFragmentManager, Consts.FG_COMPOSE)
        }

        // avatar
        home_avatar.setOnClickListener {
            var pop = PopupMenu(this, it)
            var inflater = pop.menuInflater
            inflater.inflate(R.menu.index_avatar_menu, pop.menu)
            pop.setOnMenuItemClickListener { it ->
                when(it.itemId) {
                    R.id.menu_logout -> logout()
                }
                true
            }
            pop.show()
        }

        setAvatar()

        if(App.instance.myself != null){
            initViewPager()
        }


        // TODO: fetch site information for update title (no API)
        val homeName = PrefUtil.getSiteName()
        if(!homeName.isNullOrEmpty() && homeName != getString(R.string.app_name)){
            home_title?.text = homeName
        } else {
            HtmlCrawler.run(
                PrefUtil.getApiUrl(),
                MyHtmlCrawler(this)
            )
        }
    }

    fun initViewPager(){
        val names = resources.getStringArray(R.array.index_tab)
        vp_index.adapter = IndexPageAdapter(this, supportFragmentManager)
        vp_index.setOnPageChangeListener(object: ViewPager.OnPageChangeListener{
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageSelected(position: Int) {
                tv_home_page_name.text = names[position]
            }
        })
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
        App.instance.mygroup = null
        PrefUtil.resetAll()
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
            act.initViewPager()
        }

    }
}