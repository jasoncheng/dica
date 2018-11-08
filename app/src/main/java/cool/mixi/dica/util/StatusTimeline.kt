package cool.mixi.dica.util

import android.content.Context
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.adapter.StatusesAdapter
import cool.mixi.dica.bean.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.SoftReference

interface IStatusDataSouce {
    fun sourceOld(): Call<List<Status>>?
    fun sourceNew(): Call<List<Status>>?
    fun loaded(data: List<Status>)
}

class StatusTimeline(val context: Context, val table: RecyclerView,
                     private val swipeRefreshLayout: SwipeRefreshLayout,
                     private val dataSouce: IStatusDataSouce
) : SwipeRefreshLayout.OnRefreshListener {

    private var statuses = ArrayList<Status>()
    // is load more toast show
    var noMoreDataToastShow = false

    // if everything is loaded
    var allLoaded: Boolean = false

    // for pull newest status
    var sinceId = 0

    // for load sourceOld status
    var maxId = 0

    fun init(): StatusTimeline {
        table.layoutManager = LinearLayoutManager(context)
        table.adapter = StatusesAdapter(statuses, context)
        table.setOnScrollListener(OnStatusTableScrollListener(this))
        swipeRefreshLayout.setOnRefreshListener(this)
        swipeRefreshLayout.isRefreshing = true
        return this
    }

    fun clear(){
        statuses.clear()
    }

    fun addAll(data: List<Status>) {
        statuses.addAll(data)
    }

    fun add(status: Status) {
        statuses.add(status)
    }

    override fun onRefresh() {
        loadNewest(null)
    }

    @Synchronized open fun loadNewest(callback: IStatusDataSouce?){
        iLog("loadNewest sinceId ${sinceId}")
        dataSouce.sourceNew()?.enqueue(StatuesCallback(this, true, callback))
    }

    @Synchronized open fun loadMore(callback: IStatusDataSouce?){
        if(allLoaded){
            if(!noMoreDataToastShow){
                App.instance.toast(context.getString(R.string.all_data_load))
                noMoreDataToastShow = true
            }
            swipeRefreshLayout.isRefreshing = false
            return
        }

        iLog("loadMore maxId ${maxId}")
        dataSouce.sourceOld()?.enqueue(StatuesCallback(this, false, callback))
        swipeRefreshLayout.isRefreshing = true
    }

    class OnStatusTableScrollListener(stl: StatusTimeline): RecyclerView.OnScrollListener() {
        private val ref = SoftReference<StatusTimeline>(stl)
        var lastVisibleItem: Int? = 0
        override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if(ref.get() == null) {
                recyclerView?.removeOnScrollListener(this)
                return
            }

            if(newState == RecyclerView.SCROLL_STATE_IDLE &&
                lastVisibleItem!! + 1 == recyclerView?.adapter?.itemCount) {
                ref.get()!!.loadMore(null)
            }
        }

        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView?.layoutManager as LinearLayoutManager
            lastVisibleItem = layoutManager.findLastVisibleItemPosition()
        }
    }

    class StatuesCallback(timeline: StatusTimeline, insertMode: Boolean, val callback: IStatusDataSouce?):
        Callback<List<Status>> {
        private val ref = SoftReference<StatusTimeline>(timeline)
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
            act.swipeRefreshLayout.isRefreshing = false
            if(response.body() == null) {return}

            val res = response.body()

            // handle sinceId & maxId
            res?.forEach {
                if(it.id > act.sinceId) act.sinceId = it.id
                if(act.maxId == 0 || it.id < act.maxId) act.maxId = it.id

                FriendicaUtil.stripStatusTextProxyUrl(it)

                // any more sourceOld status ?
                if(!insertMode && res?.count()!! <= 1 && act.statuses.contains(it)){
                    act.allLoaded = true
                    return
                }
            }

            // handle data by itself
            if(callback != null){
                callback.loaded(res!!)
                return
            }

            // common process
            if(insertMode) {
                res?.forEach continuing@ {
                    if(act.statuses.contains(it)) {return@continuing}
                    act.statuses.add(0, it)
                }
                act.table.adapter.notifyDataSetChanged()
                act.table.scrollToPosition(0)
            } else {
                res?.forEach continuing@ {
                    if(act.statuses.contains(it)) {return@continuing}
                    act.statuses.add(it)
                }
                act.table.adapter.notifyDataSetChanged()
            }
        }
    }
}