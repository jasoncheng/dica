package cool.mixi.dica.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.activity.MainActivity
import cool.mixi.dica.bean.Status
import cool.mixi.dica.util.IStatusDataSouce
import cool.mixi.dica.util.StatusTimeline
import cool.mixi.dica.util.dLog
import kotlinx.android.synthetic.main.fg_timeline.*
import retrofit2.Call

abstract class TimelineFragment: Fragment(), IStatusDataSouce {

    var stl: StatusTimeline? = null
    var myId: Int = App.instance.myself?.friendica_owner?.id!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fg_timeline, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stl = StatusTimeline(context!!, statuses_list, srl, this).init()
        stl?.loadMore(null)
    }

    override fun loaded(data: List<Status>){
        dLog("")
    }

    fun reloadNotification(){
        (activity as MainActivity).getNotifications()
    }

    abstract override fun sourceOld(): Call<List<Status>>
    abstract override fun sourceNew(): Call<List<Status>>
}