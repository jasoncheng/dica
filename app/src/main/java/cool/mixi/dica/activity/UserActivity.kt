package cool.mixi.dica.activity

import android.os.Bundle
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.adapter.StatusesAdapter
import cool.mixi.dica.bean.*
import cool.mixi.dica.util.*
import kotlinx.android.synthetic.main.activity_user.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class UserActivity: BaseActivity(), IStatusDataSource {

    var user: User? = null
    var userId: String? = null
    var userEmail: String? = null
    var userUrl: String? = null
    var userNotFoundStr = ""
    var serviceNotAvailable = ""
    var strFetchingRemoteUser = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        userNotFoundStr = getString(R.string.user_not_found)
        serviceNotAvailable = getString(R.string.common_error)
        strFetchingRemoteUser = getString(R.string.fetch_remote_user)
        handleIntent()
    }

    private fun handleIntent(){
        user = intent.extras.get(Consts.EXTRA_USER) as? User
        userId = intent.getStringExtra(Consts.EXTRA_USER_ID)
        userEmail = intent.getStringExtra(Consts.EXTRA_USER_EMAIL)
        userUrl = intent.getStringExtra(Consts.EXTRA_USER_URL)
        if(user != null){
            dLog(user.toString())
            initLoad()
            return
        }

        if(userId == null && user == null && userEmail == null){
            finish()
            return
        }

        when {
            userId != null -> ApiService.create().usersShow(userId!!).enqueue(CallbackUser(this))
            userEmail != null -> getUserInfoFromEmail(userEmail!!)
            userUrl != null -> {
                val email = userUrl!!.possibleNetworkAcctFromUrl()
                getUserInfoFromEmail(email)
            }
            else -> {
                App.instance.toast(userNotFoundStr)
                finish()
            }
        }
    }

    private fun getUserInfoFromEmail(email: String = "jasoncheng@mastodon.social"){
        home_srl.isRefreshing = true
        App.instance.toast(strFetchingRemoteUser)
        val atomUrl = App.instance.getWebFinger(email)
        if(atomUrl != null){
            val uri = URL(atomUrl)
            ApiService.createAP(atomUrl).apProfile(uri.path).enqueue(CallbackUserStream(this))
            return
        }

        ApiService.create(email).webFinger("acct:$email").enqueue(WebFingerCallback(email, this))
    }

    class WebFingerCallback(private val email: String, activity: UserActivity): Callback<WebFinger> {
        private val ref = WeakReference<UserActivity>(activity)
        override fun onFailure(call: Call<WebFinger>, t: Throwable) {
            ref.get()?.let { App.instance.toast(it.userNotFoundStr!!) }
            eLog("${t.message}")
        }

        override fun onResponse(call: Call<WebFinger>, response: Response<WebFinger>) {
            val finger = response.body()
            finger?.getATOMXMLUrl()?.let {
                val uri = URL(it)
                ApiService.createAP(it).apProfile(uri.path).enqueue(CallbackUserStream(ref.get()!!))
                App.instance.setWebFinger(email, it)
                return
            }

            ref.get()?.let {
                App.instance.toast(it.serviceNotAvailable.format(response.code().toString()))
                it.finish()
            }
        }
    }

    class CallbackUserStream(activity: UserActivity): Callback<AP> {
        private val ref = WeakReference<UserActivity>(activity)
        override fun onFailure(call: Call<AP>, t: Throwable) {
            ref.get()?.let { App.instance.toast(it.serviceNotAvailable?.format(t.message)) }
            eLog("${t.message}")
        }

        override fun onResponse(call: Call<AP>, response: Response<AP>) {
            if(ref.get() == null || response.code() != HttpsURLConnection.HTTP_OK) {
                ref.get()?.let { App.instance.toast(it.serviceNotAvailable?.format(response.errorBody())) }
                return
            }

            var activity = ref.get()!!
            val res = response.body()

            // UI
            activity.stl = StatusTimeline(activity, activity.rv_statuses_list, activity.home_srl, activity).init()
            activity.stl?.allLoaded = true
            activity.home_srl.isRefreshing = false
            activity.home_srl.isEnabled = false
            var adapter = activity.rv_statuses_list.adapter as StatusesAdapter

            // Author + Status
            activity.user = res?.toUser()
            adapter.ownerInfo = activity.user
            adapter.isOffSiteSN = true
            res?.entry?.forEach {
                val status = it.toStatus()
                status.avatar = res.link.getAvatar()
                activity.stl?.add(status)
            }
            adapter.notifyDataSetChanged()
        }

    }

    class CallbackUser(activity: UserActivity): Callback<User> {
        private val ref = WeakReference<UserActivity>(activity)
        private val errorMsg = ref.get()!!.getString(R.string.common_error)
        override fun onFailure(call: Call<User>, t: Throwable) {
            eLog(t.message.toString())
        }

        override fun onResponse(call: Call<User>, response: Response<User>) {
            if(ref.get() == null){ return }

            if(response.code() == HttpsURLConnection.HTTP_UNAUTHORIZED){
                App.instance.toast(errorMsg.format(response.errorBody().toString()))
                return
            }

            ref.get()!!.user = response.body()
            ref.get()!!.initLoad()
        }
    }

    fun initLoad(){
        stl = StatusTimeline(this, rv_statuses_list, home_srl, this).init()
        (rv_statuses_list.adapter as StatusesAdapter).ownerInfo = user
        stl?.table?.adapter?.notifyDataSetChanged()
        stl?.loadMore(this)
    }

    override fun loaded(data: List<Status>) {
        val adapter = rv_statuses_list.adapter as StatusesAdapter
        adapter.initLoaded = true
        data.forEach { stl?.add(it) }
        if(stl?.count() == 0){
            dLog("no data to load, start fetch webFinger")
            user?.statusnet_profile_url?.possibleNetworkAcctFromUrl().let {
                getUserInfoFromEmail(it!!)
            }
        } else {
            adapter.notifyDataSetChanged()
        }
    }

    override fun sourceOld(): Call<List<Status>>? {
        return ApiService.create().statusUserTimeline(user?.id!!, "","${stl?.maxId}")
    }

    override fun sourceNew(): Call<List<Status>>? {
        return ApiService.create().statusUserTimeline(user?.id!!,"${stl?.sinceId}", "")
    }
}