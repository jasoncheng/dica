package link.mawa.android.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.status_list_item.view.*
import link.mawa.android.R
import link.mawa.android.bean.Status
import java.util.*

class StatusesAdapter(val data:ArrayList<Status>, val context: Context): RecyclerView.Adapter<StatusViewHolder>() {

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        val st = data.get(position)
        holder.userName.text = st.user.screen_name
        holder.content.text = st.text
        var date = Date(st.created_at)
        holder.datetime.text = DateUtils.getRelativeTimeSpanString(date.time)
        Glide.with(context.applicationContext)
            .load(st.user.profile_image_url_large)
            .apply(RequestOptions().circleCrop())
            .into(holder.avatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        return StatusViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.status_list_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return data.size
    }

}

class StatusViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val userName = view.tv_status_user_name
    val avatar = view.avatar
    var content = view.tv_content
    var datetime = view.tv_datetime
}