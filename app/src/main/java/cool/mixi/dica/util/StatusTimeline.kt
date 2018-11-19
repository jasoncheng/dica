package cool.mixi.dica.util

import android.content.Context
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.activity.StatusActivity
import cool.mixi.dica.adapter.StatusesAdapter
import cool.mixi.dica.bean.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.SoftReference

interface IStatusDataSource {
    fun sourceOld(): Call<List<Status>>?
    fun sourceNew(): Call<List<Status>>?
    fun loaded(data: List<Status>)
}

class StatusTimeline(val context: Context, val table: RecyclerView,
                     private val swipeRefreshLayout: SwipeRefreshLayout,
                     private val dataSource: IStatusDataSource
) : SwipeRefreshLayout.OnRefreshListener {

    private var statuses = ArrayList<Status>()

    // is load more toast show
    private var noMoreDataToastShow = false

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

    fun count(): Int {
        return statuses.size
    }

    fun resetQuery(){
        maxId = 0
        sinceId = 0
        allLoaded = false
    }

    override fun onRefresh() {
        loadNewest(null)
    }

    @Synchronized open fun loadNewest(callback: IStatusDataSource?){
        iLog("loadNewest sinceId ${sinceId}")
        dataSource.sourceNew()?.enqueue(StatuesCallback(this, true, callback))
    }

    @Synchronized open fun loadMore(callback: IStatusDataSource?){
        if(allLoaded){
            if(!noMoreDataToastShow && context !is StatusActivity){
                App.instance.toast(context.getString(R.string.all_data_load))
                noMoreDataToastShow = true
            }
            swipeRefreshLayout.isRefreshing = false
            return
        }

        iLog("loadMore maxId ${maxId}")
        dataSource.sourceOld()?.enqueue(StatuesCallback(this, false, callback))
        swipeRefreshLayout.isRefreshing = true
    }

    class OnStatusTableScrollListener(stl: StatusTimeline): RecyclerView.OnScrollListener() {
        private val ref = SoftReference<StatusTimeline>(stl)
        private var lastVisibleItem: Int? = 0
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

    class StatuesCallback(timeline: StatusTimeline, insertMode: Boolean, private val callback: IStatusDataSource?):
        Callback<List<Status>> {
        private val ref = SoftReference<StatusTimeline>(timeline)
        private val insertMode = insertMode

        override fun onFailure(call: Call<List<Status>>, t: Throwable) {
            eLog("fail ${t.message}")
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
            if(response.body() == null) {
                callback?.loaded(ArrayList())
                return
            }

            val res = response.body()

            // handle sinceId & maxId
            res?.forEachIndexed { idx, it ->
                if(it.id > act.sinceId) act.sinceId = it.id
                if(act.maxId == 0 || it.id < act.maxId) act.maxId = it.id

                // Bind Address if possible
                LocationUtil.instance.bindGeoAddress(it)

                // TODO: Test
                it.text = it.text.dicaRenderData()

                // any more sourceOld status ?
                if(!insertMode && res?.count()!! <= 1 && act.statuses.contains(it)){
                    act.allLoaded = true
                    act.table.adapter.notifyItemChanged(idx)
                    return
                }
            }

            // handle data by itself
            if(callback != null){
                callback.loaded(res!!)
                return
            }

            // no any data
            if(act.statuses.size == 0 && res!!.isEmpty()){
                act.table.adapter.notifyDataSetChanged()
                return
            }

            if(insertMode) {
                res?.forEachIndexed continuing@ { _, it ->
                    if(act.statuses.contains(it)) { return@continuing }
                    act.statuses.add(0, it)
                }
                act.table.adapter.notifyDataSetChanged()
            } else {
                res?.forEachIndexed continuing@ { _, it ->
                    if(act.statuses.contains(it)) { return@continuing }
                    act.statuses.add(it)
                    act.table.adapter.notifyItemInserted(act.table.adapter.itemCount)
                }
            }
        }
    }
}