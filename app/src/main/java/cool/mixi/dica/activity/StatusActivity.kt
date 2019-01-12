package cool.mixi.dica.activity

import android.os.Bundle
import android.util.ArrayMap
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.bean.Status
import cool.mixi.dica.util.ApiService
import cool.mixi.dica.util.IStatusDataSource
import cool.mixi.dica.util.StatusTimeline
import cool.mixi.dica.util.dLog
import kotlinx.android.synthetic.main.activity_status.*
import retrofit2.Call
import java.util.*

class StatusActivity : BaseActivity(), IStatusDataSource {

    private var statusId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)
        processIntent()
    }

    private fun processIntent() {
        if (intent == null) {
            return
        }
        statusId = intent.getIntExtra(Consts.ID_STATUS, 0)
        if (statusId == 0) {
            App.instance.toast(getString(R.string.status_not_found))
            finish()
            return
        }

        stl = StatusTimeline(this, rv_statuses_list, home_srl, this).init()
        home_srl.setOnRefreshListener { requireRefresh() }

        stl?.loadNewest(this)
    }

    override fun requireRefresh() {
        home_srl.isRefreshing = true
        stl?.loadNewest(this)
    }

    override fun loaded(data: List<Status>) {
        if (data.isEmpty()) {
            App.instance.toast(getString(R.string.status_not_exists))
            finish()
            return
        }

        stl?.allLoaded = true
        Collections.reverse(data)
        stl?.clear()
        stl?.addAll(data!!)
        rv_statuses_list.adapter?.notifyDataSetChanged()

        /*
        [Handle Forum Thread]
        1. constructor data
        2. sort array && flat
        Original Status:
          [SecondLevel ID1] = [..]
          [SecondLevel ID2] = [..]
         */
        var firstStatus: Status = data[0]
        val firstId = firstStatus.id
        val map: ArrayMap<Int, ArrayList<Status>> = ArrayMap()
        data.forEachIndexed outerLoop@{ index, status ->
            if (index == 0) return@outerLoop
            if (firstId == status.in_reply_to_status_id) {
                status.firstCommentId = firstId
                var tmpArray: ArrayList<Status> = ArrayList()
                tmpArray.add(status)
                map[status.id] = tmpArray
                return@outerLoop
            }

            map.keys.forEachIndexed insideLoop@{ _, i ->
                if (i == status.in_reply_to_status_id) {
                    status.indent++
                    status.firstCommentId = i
                    map[i]?.add(status)
                    return@insideLoop
                }

                map[i]?.forEach {
                    if (it.id == status.in_reply_to_status_id) {
                        status.indent = it.indent+1
                        status.firstCommentId = i
                        map[i]?.add(status)
                        return@insideLoop
                    }
                }
            }
        }

        val finalArray = ArrayList<Status>()
        finalArray.add(firstStatus)

        map.keys.forEach { statusId ->
            val arr = map[statusId]
            arr?.sortWith(compareBy { it.id })
            map[statusId]?.let {
                finalArray.addAll(it)
            }
        }

        finalArray.forEach {
            var space = ""
            for(i in 0..it.indent){
                space += "  "
            }
            dLog("$space |- ${it.id} - ${it.firstCommentId} - ${it.in_reply_to_status_id} ${it.friendica_owner.screen_name}")
        }
    }

    override fun sourceOld(): Call<List<Status>>? {
        return null
    }

    override fun sourceNew(): Call<List<Status>>? {
        return ApiService.create().statusShow(statusId!!, 1)
    }
}