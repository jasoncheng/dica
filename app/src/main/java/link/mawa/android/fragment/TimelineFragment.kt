package link.mawa.android.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fg_timeline.*
import link.mawa.android.App
import link.mawa.android.R
import link.mawa.android.bean.Status
import link.mawa.android.util.IStatusDataSouce
import link.mawa.android.util.StatusTimeline
import link.mawa.android.util.dLog
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

    abstract override fun sourceOld(): Call<List<Status>>
    abstract override fun sourceNew(): Call<List<Status>>
}