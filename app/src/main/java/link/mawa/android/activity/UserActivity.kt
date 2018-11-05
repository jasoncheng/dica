package link.mawa.android.activity

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_status.*
import link.mawa.android.App
import link.mawa.android.R
import link.mawa.android.adapter.StatusesAdapter
import link.mawa.android.bean.Consts
import link.mawa.android.bean.Status
import link.mawa.android.bean.User
import link.mawa.android.util.ApiService
import link.mawa.android.util.dLog
import link.mawa.android.util.eLog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference
import javax.net.ssl.HttpsURLConnection

class UserActivity: BaseActivity(), SwipeRefreshLayout.OnRefreshListener, BaseActivity.IStatusCallback {

    var user: User? = null
    var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        handleIntent()
    }

    fun doLayout(){
        dLog("doLayout: ${user.toString()}")
        // UI
        rv_statuses_list.layoutManager = LinearLayoutManager(this)
        rv_statuses_list.adapter = StatusesAdapter(statuses, this)
        (rv_statuses_list.adapter as StatusesAdapter).ownerInfo = user
        rv_statuses_list.setOnScrollListener(OnStatusTableScrollListener(this))
        rv_statuses_list.adapter.notifyDataSetChanged()
        // Refresh UI
        home_srl.setOnRefreshListener(this)
    }

    private fun handleIntent(){
        user = intent.extras.get(Consts.EXTRA_USER) as User
        userId = intent.getStringExtra(Consts.EXTRA_USER_ID)

        if(user != null){
            doLayout()
            initLoad()
            return
        }

        if(userId == null && user == null){
            App.instance.toast(getString(R.string.user_not_found))
            finish()
            return
        }

        doLayout()
        ApiService.create().usersShow(userId!!).enqueue(CallbackUser(this))
    }

    class CallbackUser(activity: UserActivity): Callback<User> {
        private val ref = WeakReference<UserActivity>(activity)
        private val errorMsg = ref.get()!!.getString(R.string.common_error)
        override fun onFailure(call: Call<User>, t: Throwable) {
            eLog(t.message.toString())
        }

        override fun onResponse(call: Call<User>, response: Response<User>) {
            if(ref.get() == null){
                return
            }

            if(response.code() == HttpsURLConnection.HTTP_UNAUTHORIZED){
                App.instance.toast(errorMsg.format(response.errorBody().toString()))
                return
            }

            ref.get()!!.user = response.body()
            ref.get()!!.initLoad()
        }
    }

    fun initLoad(){
        ApiService.create().statusUserTimeline(user?.id!!, "","")
            .enqueue(StatuesCallback(this, false, this))
    }

    override fun statusesLoaded(data: List<Status>?) {
        if(data == null){ return }
        data.forEach {
            statuses.add(it)
        }
        rv_statuses_list.adapter.notifyDataSetChanged()
    }

    override fun loadMore() {
        super.loadMore()
        dLog("loadMore ${user?.id!!}")
        ApiService.create().statusUserTimeline(user?.id!!, "","${maxId}")
            .enqueue(StatuesCallback(this, false, null))
    }

    override fun loadNewest() {
        super.loadNewest()
        ApiService.create().statusUserTimeline(user?.id!!,"${sinceId}", "")
            .enqueue(StatuesCallback(this, true, null))
    }

    override fun onRefresh() {
        loadNewest()
    }
}