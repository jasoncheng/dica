package cool.mixi.dica.util

import android.content.Context
import android.os.Handler
import androidx.recyclerview.widget.RecyclerView
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.activity.StatusActivity
import cool.mixi.dica.activity.UserActivity
import cool.mixi.dica.adapter.StatusesAdapter
import cool.mixi.dica.bean.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

interface IStatusDataSource {
    fun sourceOld(): Call<List<Status>>?
    fun sourceNew(): Call<List<Status>>?
    fun loaded(data: List<Status>)
}

class StatusTimeline(val context: Context, val table: androidx.recyclerview.widget.RecyclerView,
                     private val swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout,
                     private val dataSource: IStatusDataSource
) : androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener {

    var selfRef: SoftReference<StatusTimeline>? = null

    var statuses = ArrayList<Status>()

    // is load more toast show
    private var noMoreDataToastShow = false

    // if everything is loaded
    var allLoaded: Boolean = false

    // for pull newest status
    var sinceId = 0

    // for load sourceOld status
    var maxId = 0

    // Handler
    private var mHandler: Handler? = null
    private var moreRunnable: MoreRunnable? = null
    fun init(): StatusTimeline {
        mHandler = Handler()
        selfRef = SoftReference(this)
        table.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        table.adapter = StatusesAdapter(statuses, context)
        table.setOnScrollListener(OnStatusTableScrollListener(selfRef))
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

    fun loadNewest(callback: IStatusDataSource?){
        dataSource.sourceNew()?.enqueue(StatuesCallback(this, true, callback))
    }

    class MoreRunnable(private val ref: SoftReference<StatusTimeline>, val callback: WeakReference<IStatusDataSource>?): Runnable {
        override fun run() {
            ref.get()?.let {
                if(callback == null) {
                    it.dataSource.sourceOld()?.enqueue(StatuesCallback(it, false, null))
                } else {
                    it.dataSource.sourceOld()?.enqueue(StatuesCallback(it, false, callback.get()))
                }
            }
        }
    }

    fun loadMore(callback: IStatusDataSource?){
        if(allLoaded){
            if(!noMoreDataToastShow && context !is StatusActivity){
                App.instance.toast(context.getString(R.string.all_data_load))
                noMoreDataToastShow = true
            }
            swipeRefreshLayout.isRefreshing = false
            return
        }

        if(moreRunnable == null){
            val cb: WeakReference<IStatusDataSource>? = if(callback != null){
                WeakReference(callback)
            } else {
                null
            }
            moreRunnable = MoreRunnable(selfRef!!, cb)
        }

        swipeRefreshLayout.isRefreshing = true
        mHandler?.removeCallbacks(moreRunnable)
        mHandler?.postDelayed(moreRunnable, 3000)
    }

    class OnStatusTableScrollListener(private val ref: SoftReference<StatusTimeline>?): androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
        private var lastVisibleItem: Int? = 0
        override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            ref?.get()?.let {
                recyclerView?.removeOnScrollListener(this)
            }

            if(newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE &&
                lastVisibleItem!! + 1 == recyclerView?.adapter?.itemCount) {
                ref?.get()?.let {
                    it.loadMore(null)
                }
            }
        }

        override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView?.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
            lastVisibleItem = layoutManager.findLastVisibleItemPosition()
        }
    }

    class MyBindStatusGeoCallback(private val statusTimeline: SoftReference<StatusTimeline>?): IBindStatusGeo {
        override fun done(status: Status) {
            val adapter = statusTimeline?.get()?.table?.adapter as StatusesAdapter
            try {
                var pos = adapter.data.indexOf(status)
                if(statusTimeline.get()?.context is UserActivity){
                    pos+=1
                }
                adapter.notifyItemChanged(pos)
            }catch (e: Exception){}
        }
    }

    class StatuesCallback(timeline: StatusTimeline, insertMode: Boolean, private val callback: IStatusDataSource?):
        Callback<List<Status>> {
        private val ref = SoftReference<StatusTimeline>(timeline)
        private val insertMode = insertMode

        override fun onFailure(call: Call<List<Status>>, t: Throwable) {
            eLog("fail ${t.message}")
            ref.get()?.let {
                App.instance.toast(it.context.getString(R.string.common_error).format(t.message))
                it.swipeRefreshLayout.isRefreshing = false
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

            //TODO: for testing parse BBCode or something else
//            var tmpStr= "*Validators + Aspects: кастомизируем валидацию https://habr.com/post/432040/?utm_source=habrahabr&utm_medium=rss&utm_campaign=432040* #habrahabr https://habr.com/post/432040/?amp;utm_medium=rss&utm_campaign=432040 https://habr.com/post/432040/?utm_source=habrahabr&amp;utm_medium=rss&amp;utm_campaign=432040"
//            var tmpStr2= "*[Из песочницы] На-click-ать известность, или как взбудоражить робота и … остальных https://habr.com/post/432038/?utm_source=habrahabr&utm_medium=rss&utm_campaign=432038* https://MELD.de/proxy/b6/aHR0cHM6Ly9oYWJyYXN0b3JhZ2Uub3JnL3dlYnQvcHgvaHEvcG8vcHhocXBvdDZ4OXNxcGh4YWhoMmZ6aGs0cHo4LnBuZw==.png https://habr.com/post/432038/?utm_source=habrahabr&utm_medium=rss&utm_campaign=432038 #habrahabr https://habr.com/post/432038/?amp;utm_medium=rss&utm_campaign=432038 https://habr.com/post/432038/?utm_source=habrahabr&amp;utm_medium=rss&amp;utm_campaign=432038"
//            var tmpStr3 = "*Jeffrey Epstein, registered sex offender, settles civil lawsuit and avoids testimony from alleged victims http://feeds.foxnews.com/~r/foxnews/national/~3/XHEJzX9G1CA/jeffrey-epstein-a-registered-sex-offender-settles-civil-lawsuit-and-avoids-testimony-from-alleged-victims* https://MELD.de/proxy/05/aHR0cHM6Ly9hNTcuZm94bmV3cy5jb20vc3RhdGljLmZveG5ld3MuY29tL2ZveG5ld3MuY29tL2NvbnRlbnQvdXBsb2Fkcy8yMDE4LzExLzAvMC9qZXBzdGVpbm9mZmVuZGVyLmpwZz92ZT0xJnRsPTE=.jpg http://feeds.foxnews.com/~r/foxnews/national/~3/XHEJzX9G1CA/jeffrey-epstein-a-registered-sex-offender-settles-civil-lawsuit-and-avoids-testimony-from-alleged-victims"

//            var tmpStr4 = "- #EmilyLeon on #Instagram https://MELD.de/proxy/ff23b97f7524bfebfe4246b68a714790.jpg?url=https%3A%2F%2Fscontent-iad3-1.cdninstagram.com%2Fvp%2F347c5107150d1d592b82601e8660a53e%2F5CAB3F33%2Ft51.2885-15%2Fsh0.08%2Fe35%2Fs640x640%2F45537907_2509009682458860_346713977406848561_n.jpg https://web.stagram.com/p/Bq5N7Aahpin"
//            var att4 = Attachment()
//            att4.url = "https://scontent-iad3-1.cdninstagram.com/vp/347c5107150d1d592b82601e8660a53e/5CAB3F33/t51.2885-15/sh0.08/e35/s640x640/45537907_2509009682458860_346713977406848561_n.jpg"
//            att4.mimetype = "image/jpeg"

//            var tmpStr5 = "*Chinese man arrested for taking pictures of US Navy base in Florida, officials say http://feeds.foxnews.com/~r/foxnews/national/~3/M5PFQDd787A/chinese-man-arrested-for-taking-pictures-of-navy-base-in-florida-officials-say* https://MELD.de/proxy/09/aHR0cHM6Ly9hNTcuZm94bmV3cy5jb20vc3RhdGljLmZveG5ld3MuY29tL2ZveG5ld3MuY29tL2NvbnRlbnQvdXBsb2Fkcy8yMDE4LzEyLzAvMC9aaGFvLVFpYW5saS5qcGc_dmU9MSZ0bD0x.jpg http://feeds.foxnews.com/~r/foxnews/national/~3/M5PFQDd787A/chinese-man-arrested-for-taking-pictures-of-navy-base-in-florida-officials-say"
//            var att5 = Attachment()
//            att5.url = "http://feeds.feedburner.com/~r/foxnews/national/~4/M5PFQDd787A"
//            att5.mimetype = "image/jpeg"

//            var tmpStr6 = "This #Week in #F-Droid - #Building the #Android #SDKs as #Free #Software, and other #calls for #help F-Droid - #Free and #OpenSource #Android #App #Repository - #news #summary #ThisWeekInFdroid #TWIF https://MELD.de/proxy/4a6e66e6d7695de4607fe32888fc1357?url=https%3A%2F%2Fpod.geraspora.de%2Fcamo%2Fc6947de8327c68c440037b7a0a7b2059668350bd%2F68747470733a2f2f736e61726c2e64652f70726f78792f34622f6148523063484d364c793930636a457559324a7a61584e30595852705979356a62323076614856694c326b76636938794d4445314c7a41784c7a49344c7a5a694e6d59784e7a67784c544579596d59744e4467774f5331684f5467304c545a684d4451324e44677a4d444d354f5339795a584e70656d55764e7a6377654339685a4463315a6a4a6a4e7a426d5a6d59315a574e6d5a4745784e4455784f5455785a6a67785a6a41315a53396d5a484a766157526f5a584a764c6d70775a773d3d2e6a7067 In this edition: Building the Android SDKs as Free Software, F-Droid buildserver container, F-Droid article in c’t magazine, repomaker Flatpak and TWIF Call for Help. There are 8 new and 67 updated apps. https://f-droid.org/en/2018/11/30/twif-32-fixme-building-the-android-sdks-as-free-software-and-other-calls-for-help.html https://f-droid.org/en/2018/11/30/twif-32-fixme-building-the-android-sdks-as-free-software-and-other-calls-for-help.html"
//            var att6 = Attachment()
//            att6.url = "https://pod.geraspora.de/camo/c6947de8327c68c440037b7a0a7b2059668350bd/68747470733a2f2f736e61726c2e64652f70726f78792f34622f6148523063484d364c793930636a457559324a7a61584e30595852705979356a62323076614856694c326b76636938794d4445314c7a41784c7a49344c7a5a694e6d59784e7a67784c544579596d59744e4467774f5331684f5467304c545a684d4451324e44677a4d444d354f5339795a584e70656d55764e7a6377654339685a4463315a6a4a6a4e7a426d5a6d59315a574e6d5a4745784e4455784f5455785a6a67785a6a41315a53396d5a484a766157526f5a584a764c6d70775a773d3d2e6a7067"
//            att6.mimetype = "image/jpeg"
//            var tmpAr = ArrayList<Attachment>()
//            tmpAr.add(att5)

            // handle sinceId & maxId
            res?.forEachIndexed { idx, it ->
                if(it.id > act.sinceId) act.sinceId = it.id
                if(act.maxId == 0 || it.id < act.maxId) act.maxId = it.id

                // Bind Address if possible
                LocationUtil.instance.bindGeoAddress(it, MyBindStatusGeoCallback(act?.selfRef))

//                it.attachments = tmpAr
//                it.text = tmpStr

                it.text = it.text.dicaRenderData()

                // NotSafeForWork
                it.enableNSFW = it.text.contains("#nsfw", true)

                // any more sourceOld status ?
                if(!insertMode && res?.count()!! <= 1 && act.statuses.contains(it)){
                    act.allLoaded = true
                    act.table.adapter?.notifyItemChanged(idx)
                    return
                }
            }

            // Cache user
            res?.let { App.instance.addUserToDB(it) }

            // handle data by itself
            if(callback != null){
                callback.loaded(res!!)
                return
            }

            // no any data
            if(act.statuses.size == 0 && res!!.isEmpty()){
                act.table.adapter?.notifyDataSetChanged()
                return
            }

            if(insertMode) {
                res?.forEachIndexed continuing@ { _, it ->
                    if(act.statuses.contains(it)) { return@continuing }
                    act.statuses.add(0, it)
                }
                act.table.adapter?.notifyDataSetChanged()
            } else {
                res?.forEachIndexed continuing@ { _, it ->
                    if(act.statuses.contains(it)) { return@continuing }
                    act.statuses.add(it)
                    act.table.adapter?.notifyItemInserted(act?.table?.adapter?.itemCount ?: 0)
                }
            }
        }
    }
}