package link.mawa.android.activity

import android.content.Intent
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import link.mawa.android.App
import link.mawa.android.R
import link.mawa.android.bean.Consts
import link.mawa.android.bean.Status
import link.mawa.android.util.ApiService
import link.mawa.android.util.iLog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference

open class BaseActivity: AppCompatActivity() {

    var statuses = ArrayList<Status>()
    var allLoaded: Boolean = false

    // for pull newest status
    var sinceId = 0

    // for load old status
    var maxId = 0

    // UI
    var alertDialog:AlertDialog? = null

    // UI
    fun loading(message: String) {
        var builder = AlertDialog.Builder(this)
        var view = layoutInflater.inflate(R.layout.loading_dialog, null)
        view.findViewById<TextView>(R.id.tv_loading).text = message
        builder.setCancelable(true)
        builder.setView(view)
        alertDialog = builder.show()
    }

    // UI
    fun loaded(){
        alertDialog?.dismiss()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        supportFragmentManager.findFragmentByTag(Consts.FG_COMPOSE).onActivityResult(requestCode, resultCode, data)
    }

    class OnStatusTableScrollListener(activity: BaseActivity): RecyclerView.OnScrollListener() {
        private val ref = WeakReference<BaseActivity>(activity)
        var lastVisibleItem: Int? = 0
        override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if(ref.get() == null) {
                recyclerView?.removeOnScrollListener(this)
                return
            }
            if(newState == RecyclerView.SCROLL_STATE_IDLE &&
                lastVisibleItem!! + 1 == recyclerView?.adapter?.itemCount) {
                ref.get()!!.loadMoreStatues()
            }
        }

        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView?.layoutManager as LinearLayoutManager
            lastVisibleItem = layoutManager.findLastVisibleItemPosition()
        }
    }

    class StatuesCallback(activity: BaseActivity, insertMode: Boolean): Callback<List<Status>> {
        private val ref = WeakReference<BaseActivity>(activity)
        private val insertMode = insertMode
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
            act.home_srl.isRefreshing = false
            if(response.body() == null) {return}

            val res = response.body()


            // handle sinceId & maxId
            res?.forEach {
                if(it.id > act.sinceId) act.sinceId = it.id
                if(act.maxId == 0 || it.id < act.maxId) act.maxId = it.id

                // any more old status ?
                if(!insertMode && res?.count()!! <= 1 && act.statuses.contains(it)){
                    act.allLoaded = true
                    return
                }
            }

            if(insertMode) {
                res?.forEach continuing@ {
                    if(act.statuses.contains(it)) {return@continuing}
                    act.statuses.add(0, it)
                    act.rv_statuses_list.adapter.notifyItemInserted(0)
                }
                act.rv_statuses_list.scrollToPosition(0)
            } else {
                res?.forEach continuing@ {
                    if(act.statuses.contains(it)) {return@continuing}
                    act.statuses.add(it)
                    act.rv_statuses_list.adapter.notifyItemInserted(act.statuses.size-1)
                }
            }
        }
    }

    @Synchronized fun loadNewestStatuses(){
        iLog("========> loadNewestStatuses sinceId ${sinceId}")
        ApiService.create().statusPublicTimeline("${sinceId}", "")
            .enqueue(StatuesCallback(this, true))
    }

    @Synchronized fun loadMoreStatues(){
        if(allLoaded){
            App.instance.toast(getString(R.string.all_data_load))
            home_srl.isRefreshing = false
            return
        }

        home_srl.isRefreshing = true
        iLog("========> loadMoreStatues maxId ${maxId}")
        ApiService.create().statusPublicTimeline("", "${maxId}")
            .enqueue(StatuesCallback(this, false))
    }
}