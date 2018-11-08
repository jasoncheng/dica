package link.mawa.android.fragment

import link.mawa.android.bean.Status
import link.mawa.android.util.ApiService
import retrofit2.Call

class FriendsTimelineFragment: TimelineFragment() {
    override fun sourceOld(): Call<List<Status>> {
        return ApiService.create().statusFriendsTimeline("", "${stl?.maxId}")
    }

    override fun sourceNew(): Call<List<Status>> {
        return ApiService.create().statusFriendsTimeline("${stl?.sinceId}", "")
    }
}