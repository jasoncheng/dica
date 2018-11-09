package cool.mixi.dica.fragment

import cool.mixi.dica.bean.Status
import cool.mixi.dica.util.ApiService
import retrofit2.Call

class TimelinePublicFragment: TimelineFragment() {
    override fun sourceOld(): Call<List<Status>> {
        return ApiService.create().statusPublicTimeline("", "${stl?.maxId}")
    }

    override fun sourceNew(): Call<List<Status>> {
        reloadNotification()
        return ApiService.create().statusPublicTimeline("${stl?.sinceId}", "")
    }
}