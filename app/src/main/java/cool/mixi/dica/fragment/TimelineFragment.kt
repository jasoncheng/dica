package cool.mixi.dica.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cool.mixi.dica.R
import cool.mixi.dica.activity.MainActivity
import cool.mixi.dica.adapter.StatusesAdapter
import cool.mixi.dica.bean.Status
import cool.mixi.dica.util.IStatusDataSource
import cool.mixi.dica.util.StatusTimeline
import cool.mixi.dica.util.eLog
import kotlinx.android.synthetic.main.fg_timeline.*
import retrofit2.Call

abstract class TimelineFragment: Fragment(), IStatusDataSource {

    var stl: StatusTimeline? = null
    private var isInitLoad = false
    private var ifVisible = false

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        ifVisible = isVisibleToUser
        ifVisible.let {
            if(ifVisible && !isInitLoad) {
                stl?.loadNewest(this)
                isInitLoad = true
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        isInitLoad = false
        return inflater.inflate(R.layout.fg_timeline, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stl = StatusTimeline(context!!, statuses_list, srl, this).init()
        (stl!!.table.adapter as StatusesAdapter).isFavoritesFragment =
                this.javaClass.simpleName == TimelineFavoritesFragment::class.java.simpleName

        srl.setOnRefreshListener {
            reloadNotification()
            stl?.resetQuery()
            stl?.loadNewest(this)
        }

        if(ifVisible && !isInitLoad){
            stl?.loadNewest(this)
            isInitLoad = true
        }
    }

    fun reloadNotification(){
        (activity as MainActivity).getNotifications()
    }

    override fun loaded(data: List<Status>) {
        stl?.clear()
        stl?.addAll(data)
        try {
            statuses_list.adapter.notifyDataSetChanged()
            (statuses_list.adapter as StatusesAdapter).initLoaded = true
        }catch(e: Exception){
            eLog("${e.message}")
        }
    }

    abstract override fun sourceOld(): Call<List<Status>>?
    abstract override fun sourceNew(): Call<List<Status>>?
}