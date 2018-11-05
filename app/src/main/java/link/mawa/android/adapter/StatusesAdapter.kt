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
import kotlinx.android.synthetic.main.rv_user_item_header.view.*
import kotlinx.android.synthetic.main.status_list_item.view.*
import link.mawa.android.App
import link.mawa.android.R
import link.mawa.android.activity.BaseActivity
import link.mawa.android.activity.StatusActivity
import link.mawa.android.activity.UserActivity
import link.mawa.android.bean.Consts
import link.mawa.android.bean.Status
import link.mawa.android.bean.User
import link.mawa.android.fragment.ComposeDialogFragment
import java.util.*

class StatusesAdapter(val data:ArrayList<Status>, private val context: Context): RecyclerView.Adapter<BasicViewHolder>() {

    open var ownerInfo: User? = null

    enum class ViewType {
        USER_PROFILE,
        STATUS,
        STATUS_SIMPLE,
        REPLY
    }

    private val requestOptions = RequestOptions()
        .fitCenter()
        .transforms(RoundedCorners(18))!!

    override fun onBindViewHolder(holder: BasicViewHolder, position: Int) {
        var pos = position
        if(context is UserActivity){
            if(position == 0){
                doLayoutHeaderUserProfile(holder)
                return
            }
            pos = position - 1
        }

        // MainActivity + StatusActivity + Others...
        val st = data[pos]
        var date = Date(st.created_at)
        if(context !is UserActivity){
            doLayoutUserBox(holder, st, pos)
            holder.datetime.text = DateUtils.getRelativeTimeSpanString(date.time)
        } else {
            holder.datetime.text = date.toLocaleString()
        }
        doLayoutContent(holder, st, pos)
    }

    override fun getItemViewType(position: Int): Int {
        if(context is UserActivity){
            if (position == 0){
                return ViewType.USER_PROFILE.ordinal
            }
            return ViewType.STATUS_SIMPLE.ordinal
        }

        val st = data[position]
        return if(st.in_reply_to_status_id != 0){ ViewType.REPLY.ordinal } else { ViewType.STATUS.ordinal }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasicViewHolder {
        // HeaderView
        if(viewType == ViewType.USER_PROFILE.ordinal){
            return UserProfileViewHolder(
                LayoutInflater.from(context).inflate(R.layout.rv_user_item_header, parent, false)
            )
        }

        var holder = when (viewType) {
            ViewType.REPLY.ordinal -> ReplyViewHolder(
                LayoutInflater.from(context).inflate(R.layout.reply_list_item, parent, false)
            )
            ViewType.STATUS_SIMPLE.ordinal -> UserProfileViewHolder(
                LayoutInflater.from(context).inflate(R.layout.rv_user_item, parent, false)
            )
            else -> StatusViewHolder(
                LayoutInflater.from(context).inflate(R.layout.status_list_item, parent, false)
            )
        }

        // Control click on content
        if(context is StatusActivity){
        } else {
            holder.content.setOnClickListener { gotoStatusPage(it) }
            holder.media.setOnClickListener { gotoStatusPage(it) }
        }

        // Bottom Action
        holder.comment.setOnClickListener { actComment(it) }
        holder.share.setOnClickListener { actShare(it) }
        holder.like.setOnClickListener { actLike(it) }

        return holder
    }

    override fun getItemCount(): Int {
        val extraSize = if(context is UserActivity) 1 else 0
        return data.size + extraSize
    }

    private fun doLayoutUserBox(holder: BasicViewHolder, st: Status, pos: Int){
        holder.userName.text = st.user.screen_name
        holder.userName.tag = pos
        holder.userName.setOnClickListener { gotoUserPage(it) }

        holder.avatar.setTag(R.id.avatar_tag, pos)
        holder.avatar.setOnClickListener { gotoUserPage(it) }
        Glide.with(context.applicationContext)
            .load(st.user.profile_image_url_large)
            .apply(RequestOptions().circleCrop())
            .into(holder.avatar)
    }

    private fun doLayoutContent(holder: BasicViewHolder, st: Status, pos: Int){
        holder.actGroup.tag = pos
        holder.content.text = st.text
        holder.content.tag = pos
        if(st.attachments != null && st.attachments.size > 0){
            holder.media.setTag(R.id.media_image, pos)
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

    private fun doLayoutHeaderUserProfile(holder: BasicViewHolder){
        holder.userName.text = ownerInfo?.screen_name
        Glide.with(context.applicationContext)
            .load(ownerInfo?.profile_image_url_large)
            .apply(RequestOptions().circleCrop())
            .into(holder.avatar)
    }

    private fun actLike(view: View){
        App.instance.toast(context.getString(R.string.no_api_support))
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
        App.instance.toast(context.getString(R.string.not_implement_yet))
    }

    private fun gotoUserPage(view: View) {
        var position: Int = if(view is ImageView){
            view.getTag(R.id.avatar_tag) as Int
        } else {
            view.tag as Int
        }

        val i = Intent(context, UserActivity::class.java)
        i.putExtra(Consts.EXTRA_USER, data[position].user)
        context.startActivity(i)
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
    }
}

open class BasicViewHolder(view: View): RecyclerView.ViewHolder(view) {
    open var userName = view.tv_status_user_name
    open var avatar = view.avatar
    var content = view.tv_content
    var datetime = view.tv_datetime
    var media = view.media

    // Action
    var actGroup = view.actGroup
    var share = view.tv_share
    var comment = view.tv_comment
    var like = view.tv_like
}

class UserProfileViewHolder(view: View): BasicViewHolder(view) {
    override var userName = view.tv_screen_name
    override var avatar = view.user_avatar_large
}

class StatusViewHolder(view: View): BasicViewHolder(view)
class ReplyViewHolder(view: View): BasicViewHolder(view)