package cool.mixi.dica.adapter

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.activity.StatusActivity
import cool.mixi.dica.activity.UserActivity
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.fragment.NotificationDialog
import cool.mixi.dica.util.FriendicaUtil
import kotlinx.android.synthetic.main.notification_item.view.*

class NotificationAdapter(private val fragment: NotificationDialog)
    : androidx.recyclerview.widget.RecyclerView.Adapter<NotifyViewHolder>() {

    private val unReadColor = fragment.resources.getColor(R.color.notification_unread)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifyViewHolder {
        var view = LayoutInflater.from(fragment.context).inflate(R.layout.notification_item, parent, false)

        view.setOnClickListener { goToStatusPage(it) }

        return NotifyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return App.instance.notifications.size
    }

    override fun onBindViewHolder(holder: NotifyViewHolder, position: Int) {
        var notification = App.instance.notifications?.get(position)
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
        val notification = App.instance.notifications[pos]
        when {
            notification.otype == Consts.OTYPE_ITEM -> {
                val intent = Intent(fragment.context, StatusActivity::class.java)
                intent.putExtra(Consts.ID_STATUS, notification.parent)
                fragment.startActivity(intent)
            }
            notification.otype == Consts.OTYPE_INTRO -> {
                val intent = Intent(fragment.context, UserActivity::class.java)
                intent.putExtra(Consts.EXTRA_USER_NAME, notification.name)
                fragment.startActivity(intent)
            }
            else -> App.instance.toast(fragment.getString(R.string.not_implement_yet))
        }

        FriendicaUtil.seen(notification.id, null)
        notification.seen = 1
        notifyItemChanged(pos)
        App.instance.checkIfRequireClearAllNotification()
    }
}

open class NotifyViewHolder(view: View): androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
    var msg = view.tv_message
    var date = view.tv_datetime
    var avatar = view.avatar
}