package link.mawa.android.adapter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.status_list_item.view.*
import link.mawa.android.R
import link.mawa.android.activity.BaseActivity
import link.mawa.android.activity.StatusActivity
import link.mawa.android.bean.Consts
import link.mawa.android.bean.Status
import link.mawa.android.fragment.ComposeDialogFragment
import link.mawa.android.util.dLog
import java.util.*

class StatusesAdapter(val data:ArrayList<Status>, private val context: Context): RecyclerView.Adapter<BasicViewHolder>() {

    enum class VIEW_TYPE {
        STATUS,
        REPLY
    }

    private val requestOptions = RequestOptions()
        .fitCenter()
        .transforms(RoundedCorners(18))!!

    override fun onBindViewHolder(holder: BasicViewHolder, position: Int) {
        val st = data[position]
        holder.userName.text = st.user.screen_name
        holder.content.text = st.text
        holder.content.tag = position
        holder.actGroup.tag = position
        var date = Date(st.created_at)
        holder.datetime.text = DateUtils.getRelativeTimeSpanString(date.time)
        Glide.with(context.applicationContext)
            .load(st.user.profile_image_url_large)
            .apply(RequestOptions().circleCrop())
            .into(holder.avatar)

        if(st.attachments != null && st.attachments.size > 0){
            holder.media.setTag(R.id.media_image, position)
            holder.media.visibility = View.VISIBLE
            val attachment = st.attachments[0]
            Glide.with(context.applicationContext)
                .load(attachment.url)
                .apply(requestOptions)
                .into(holder.media)
        } else {
            holder.media.visibility = View.GONE
        }
    }

    override fun getItemViewType(position: Int): Int {
        val st = data[position]
        return if(st.in_reply_to_status_id != 0){ VIEW_TYPE.REPLY.ordinal } else { VIEW_TYPE.STATUS.ordinal }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasicViewHolder {
        var holder: BasicViewHolder? = null
        if(viewType == VIEW_TYPE.REPLY.ordinal) {
            holder = ReplyViewHolder(
                LayoutInflater.from(context).inflate(R.layout.reply_list_item, parent, false)
            )
        } else {
            holder = StatusViewHolder(
                LayoutInflater.from(context).inflate(R.layout.status_list_item, parent, false)
            )
            holder.content.setOnClickListener { gotoStatusPage(it) }
            holder.media.setOnClickListener { gotoStatusPage(it) }
        }

        // Bottom Action
        holder.comment.setOnClickListener { actComment(it) }
        holder.share.setOnClickListener { actShare(it) }
        holder.like.setOnClickListener { actLike(it) }

        return holder!!
    }

    override fun getItemCount(): Int {
        return data.size
    }

    private fun actLike(view: View){
        val position = (view.parent as ViewGroup).tag as Int
        val st = data[position]
    }

    private fun actComment(view: View) {
        val position = (view.parent as ViewGroup).tag as Int
        val st = data[position]
        var bundle = Bundle()
        bundle.putString(Consts.EXTRA_IN_REPLY_USERNAME, st.friendica_owner.screen_name)
        bundle.putInt(Consts.EXTRA_IN_REPLY_STATUS_ID, st.id)
        val dlg = ComposeDialogFragment()
        dlg.arguments = bundle
        dlg.myShow((context as BaseActivity).supportFragmentManager, Consts.FG_COMPOSE)
    }

    private fun actShare(view: View){
        val position = (view.parent as ViewGroup).tag as Int
        val st = data[position]
    }

    private fun gotoStatusPage(view: View){
        var position: Int = if(view is ImageView){
            view.getTag(R.id.media_image) as Int
        } else {
            view.tag as Int
        }

        val st = data[position]
        val i = Intent(context, StatusActivity::class.java)
        i.putExtra(Consts.ID_STATUS, st.id)
        context.startActivity(i)
        dLog(st.toString())
    }
}

open class BasicViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val userName = view.tv_status_user_name!!
    val avatar = view.avatar!!
    var content = view.tv_content!!
    var datetime = view.tv_datetime!!
    var media = view.media!!

    // Action
    var actGroup = view.actGroup
    var share = view.tv_share
    var comment = view.tv_comment
    var like = view.tv_like
}

class StatusViewHolder(view: View): BasicViewHolder(view)
class ReplyViewHolder(view: View): BasicViewHolder(view)