package cool.mixi.dica.fragment

import cool.mixi.dica.App
import cool.mixi.dica.bean.Status
import cool.mixi.dica.util.ApiService
import retrofit2.Call

class TimelineMyFragment: TimelineFragment() {

    companion object {
        var myId: Int? = App.instance.myself?.friendica_owner?.id
    }

    override fun sourceOld(): Call<List<Status>>? {
        if(myId == null){ return null }
        return ApiService.create().statusUserTimeline(myId!!, "", "${stl?.maxId}")
    }

    override fun sourceNew(): Call<List<Status>>? {
        if(myId == null){ return null }
        reloadNotification()
        return ApiService.create().statusUserTimeline(myId!!, "${stl?.sinceId}", "")
    }
}