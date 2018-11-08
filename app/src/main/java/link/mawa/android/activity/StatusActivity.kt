package link.mawa.android.activity

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_status.*
import link.mawa.android.App
import link.mawa.android.R
import link.mawa.android.bean.Consts
import link.mawa.android.bean.Status
import link.mawa.android.util.ApiService
import link.mawa.android.util.IStatusDataSouce
import link.mawa.android.util.StatusTimeline
import retrofit2.Call
import java.util.*

class StatusActivity: BaseActivity(), IStatusDataSouce {

    private var statusId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)
        processIntent()
    }

    private fun processIntent(){
        if(intent == null) {return}
        statusId = intent.getIntExtra(Consts.ID_STATUS, 0)
        if(statusId == 0){
            App.instance.toast(getString(R.string.status_not_found))
        }

        stl = StatusTimeline(this, rv_statuses_list, home_srl, this).init()
        stl?.loadNewest(null)
    }


    override fun loaded(data: List<Status>) {
        Collections.reverse(data)
        stl?.clear()
        stl?.addAll(data!!)
        rv_statuses_list.adapter.notifyDataSetChanged()
    }

    override fun sourceOld(): Call<List<Status>>? {
        stl?.allLoaded = true
        return null
    }

    override fun sourceNew(): Call<List<Status>>? {
        return ApiService.create().statusShow(statusId!!, 1)
    }
}