package link.mawa.android.fragment

import link.mawa.android.bean.Status
import link.mawa.android.util.ApiService
import retrofit2.Call

class PublicTimelineFragment: TimelineFragment() {
    override fun sourceOld(): Call<List<Status>> {
        return ApiService.create().statusPublicTimeline("", "${stl?.maxId}")
    }

    override fun sourceNew(): Call<List<Status>> {
        return ApiService.create().statusPublicTimeline("${stl?.sinceId}", "")
    }
}