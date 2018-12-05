package cool.mixi.dica.adapter

import android.content.Intent
import android.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.activity.StatusActivity
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.bean.Notification
import cool.mixi.dica.fragment.NotificationDialog
import cool.mixi.dica.util.FriendicaUtil
import kotlinx.android.synthetic.main.notification_item.view.*
import java.util.*

class NotificationAdapter(val data: ArrayList<Notification>, private val fragment: NotificationDialog)
    : androidx.recyclerview.widget.RecyclerView.Adapter<NotifyViewHolder>() {

    private val unReadColor = fragment.resources.getColor(R.color.notification_unread)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifyViewHolder {
        var view = LayoutInflater.from(fragment.context).inflate(R.layout.notification_item, parent, false)

        view.setOnClickListener { goToStatusPage(it) }

        return NotifyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: NotifyViewHolder, position: Int) {
        var notification = fragment.data?.get(position)
        holder.itemView.tag = position
        if(notification?.seen == 1) {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        } else {
            holder.itemView.setBackgroundColor(unReadColor)
        }
        holder.msg.text = notification?.msg_plain
        holder.date.text = notification?.date_rel
        Glide.with(App.instance.applicationContext)
            .load(notification?.photo)
            .apply(RequestOptions().circleCrop())
            .into(holder.avatar!!)
    }

    private fun goToStatusPage(view: View) {
        val pos = view.tag as? Int ?: return
        val notification = data[pos]
        if(notification.otype != Consts.OTYPE_ITEM){
            App.instance.toast(fragment.getString(R.string.not_implement_yet))
            return
        }

        val intent = Intent(fragment.context, StatusActivity::class.java)
        intent.putExtra(Consts.ID_STATUS, notification.parent)
        fragment.startActivity(intent)
        FriendicaUtil.seen(notification.id, null)
        notification.seen = 1
        notifyItemChanged(pos)
    }
}

open class NotifyViewHolder(view: View): androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
    var msg = view.tv_message
    var date = view.tv_datetime
    var avatar = view.avatar
}