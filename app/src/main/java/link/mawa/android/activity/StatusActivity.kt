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
import link.mawa.android.util.ApiService
import java.util.*

class StatusActivity: BaseActivity(), SwipeRefreshLayout.OnRefreshListener, BaseActivity.IStatusCallback {

    private var statusId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        // UI
        rv_statuses_list.layoutManager = LinearLayoutManager(this)
        rv_statuses_list.adapter = StatusesAdapter(statuses, this)
        rv_statuses_list.setOnScrollListener(OnStatusTableScrollListener(this))
        // Refresh UI
        home_srl.setOnRefreshListener(this)

        processIntent()
    }

    private fun processIntent(){
        if(intent == null) {return}
        statusId = intent.getIntExtra(Consts.ID_STATUS, 0)
        if(statusId == 0){
            App.instance.toast(getString(R.string.status_not_found))
        }

//        App.instance.toast("status #${statusId}")
        loadNewest()
    }

    override fun statusesLoaded(data: List<Status>?) {
        statuses.clear()

        Collections.reverse(data)
        statuses.addAll(data!!)
        rv_statuses_list.adapter.notifyDataSetChanged()
    }

    override fun loadNewest() {
        ApiService.create().statusShow(statusId!!, 1)
            .enqueue(StatuesCallback(this, false, this))
    }

    override fun loadMore() {
        allLoaded = true
    }

    override fun onRefresh() {
        loadNewest()
    }
}