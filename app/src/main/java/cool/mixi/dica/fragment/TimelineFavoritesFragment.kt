package cool.mixi.dica.fragment

import cool.mixi.dica.bean.Status
import cool.mixi.dica.util.ApiService
import kotlinx.android.synthetic.main.fg_timeline.*
import retrofit2.Call

class TimelineFavoritesFragment: TimelineFragment() {
    override fun onResume() {
        super.onResume()
        try {
            statuses_list.adapter.notifyDataSetChanged()
        }catch(e: Exception){}
    }
    override fun sourceOld(): Call<List<Status>> {
        return ApiService.create().favoritesTimeline("", "${stl?.maxId}")
    }

    override fun sourceNew(): Call<List<Status>> {
        reloadNotification()
        return ApiService.create().favoritesTimeline("${stl?.sinceId}", "")
    }
}