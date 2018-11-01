package link.mawa.android.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import link.mawa.android.R
import link.mawa.android.adapter.StatusesAdapter
import link.mawa.android.bean.Status
import link.mawa.android.util.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {

    val tag = this.javaClass.simpleName!!
    var statuses = ArrayList<Status>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val api = ApiService.create()
        val callback = MyCallback(this)
        api.statusPublicTimeline("0", "0").enqueue(callback)

        rv_statuses_list.layoutManager = LinearLayoutManager(this)
        rv_statuses_list.adapter = StatusesAdapter(statuses, this)
    }


    class MyCallback(activity: MainActivity): Callback<List<Status>> {

        private val ref = WeakReference<MainActivity>(activity)
        override fun onFailure(call: Call<List<Status>>, t: Throwable) {
            if(ref.get() == null){
                return
            }
        }

        override fun onResponse(call: Call<List<Status>>, response: Response<List<Status>>) {
            if(ref.get() == null){
                return
            }

            val act = ref.get()!!
            if( response.code() == HttpsURLConnection.HTTP_UNAUTHORIZED ){
                Log.i(act.tag, "========>"+response.code())
                val intent = Intent(act, LoginActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                act.startActivity(intent)
                return
            }

            val res = response.body()
            res?.forEach {
                act.statuses.add(it)
                Log.i(act.tag, it.toString())
            }
            act.rv_statuses_list.adapter.notifyDataSetChanged()
        }
    }
}
