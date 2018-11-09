package cool.mixi.dica.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableString
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.activity.BaseActivity
import cool.mixi.dica.activity.StatusActivity
import cool.mixi.dica.activity.UserActivity
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.bean.Status
import cool.mixi.dica.bean.User
import cool.mixi.dica.fragment.ComposeDialogFragment
import cool.mixi.dica.util.FriendicaUtil
import cool.mixi.dica.util.ILike
import kotlinx.android.synthetic.main.rv_user_item_header.view.tv_description
import kotlinx.android.synthetic.main.status_action_box.view.*
import kotlinx.android.synthetic.main.status_list_item.view.*
import java.util.*



class StatusesAdapter(val data:ArrayList<Status>, private val context: Context): RecyclerView.Adapter<BasicStatusViewHolder>() {

    open var ownerInfo: User? = null
    private val likeDrawable = context.getDrawable(R.drawable.thumb_up_sel)
    private val unlikeDrawable = context.getDrawable(R.drawable.thumb_up)

    enum class ViewType {
        USER_PROFILE,
        STATUS,
        STATUS_SIMPLE,
        REPLY
    }

    private val requestOptions = RequestOptions()
        .fitCenter()
        .transforms(RoundedCorners(18))!!

    override fun onBindViewHolder(holder: BasicStatusViewHolder, position: Int) {
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
            holder.datetime?.text = DateUtils.getRelativeTimeSpanString(date.time)
        } else {
            holder.datetime?.text = date.toLocaleString()
        }
        doLayoutContent(holder, st, pos)
        doLayoutLikeRelated(holder.like!!, pos)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasicStatusViewHolder {
        // HeaderView
        if(viewType == ViewType.USER_PROFILE.ordinal){
            return UserProfileViewHolder(
                LayoutInflater.from(context).inflate(R.layout.rv_user_item_header, parent, false)
            )
        }

        var holder = when (viewType) {
            ViewType.REPLY.ordinal -> StatusReplyViewHolder(
                LayoutInflater.from(context).inflate(R.layout.reply_list_item, parent, false)
            )
            ViewType.STATUS_SIMPLE.ordinal -> StatusNoUserInfoViewHolder(
                LayoutInflater.from(context).inflate(R.layout.rv_user_item, parent, false)
            )
            else -> StatusViewHolder(
                LayoutInflater.from(context).inflate(R.layout.status_list_item, parent, false)
            )
        }

        // Control click on content
        if(context is StatusActivity){
        } else {
            holder.content?.setOnClickListener { gotoStatusPage(it) }
            holder.media?.setOnClickListener { gotoStatusPage(it) }
        }

        // Bottom Action
        holder.comment?.setOnClickListener { actComment(it) }
        holder.share?.setOnClickListener { actShare(it) }
        holder.like?.setOnClickListener { actLike(it) }

        return holder
    }

    override fun getItemCount(): Int {
        val extraSize = if(context is UserActivity) 1 else 0
        return data.size + extraSize
    }

    private fun amILike(status: Status): Boolean {
        return status.friendica_activities.like.contains(App.instance.myself?.friendica_owner)
    }

    private fun doLayoutLikeRelated(view: TextView, pos: Int){
        var status = data[pos]
        var me = App.instance.myself?.friendica_owner!!
        val isLike = amILike(status)
        var likes = status.friendica_activities.like
        if(!isLike){
            view.setCompoundDrawablesWithIntrinsicBounds(unlikeDrawable, null, null, null)
        } else {
            view.setCompoundDrawablesWithIntrinsicBounds(likeDrawable, null, null, null)
        }

        var tvLikeDetails = (view.parent.parent as ViewGroup).findViewById<TextView>(R.id.tv_like_details)
        if(likes.size == 0){
            tvLikeDetails.visibility = View.GONE
        } else {
            val bold = StyleSpan(Typeface.BOLD)
            var sizeStr = likes.size.toString()
            var likeTxt = context.getString(R.string.likes_counter, likes.size.toString())
            var color = ForegroundColorSpan(context.getColor(R.color.like_counter))
            var sp = SpannableString(likeTxt)
            sp.setSpan(color, 0, sizeStr.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
            sp.setSpan(bold, 0, sizeStr.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
            tvLikeDetails.visibility = View.VISIBLE
            tvLikeDetails.text = sp
            tvLikeDetails.setOnClickListener {
                gotoUserLikesPage(pos)
            }
        }
    }

    private fun doLayoutUserBox(holder: BasicStatusViewHolder, st: Status, pos: Int){
        holder.userName?.text = st.user.screen_name
        holder.userName?.tag = pos
        holder.userName?.setOnClickListener { gotoUserPage(it) }

        holder.avatar?.setTag(R.id.avatar_tag, pos)
        holder.avatar?.setOnClickListener { gotoUserPage(it) }
        Glide.with(context.applicationContext)
            .load(st.user.profile_image_url_large)
            .apply(RequestOptions().circleCrop())
            .into(holder.avatar!!)
    }

    private fun doLayoutContent(holder: BasicStatusViewHolder, st: Status, pos: Int){
        holder.actGroup?.tag = pos
        holder.content?.text = st.text
        holder.content?.tag = pos
        if(st.attachments != null && st.attachments.size > 0){
            holder.media?.setTag(R.id.media_image, pos)
            holder.media?.visibility = View.VISIBLE
            val attachment = st.attachments[0]
            Glide.with(context.applicationContext)
                .load(attachment.url)
                .apply(requestOptions)
                .into(holder.media!!)
        } else {
            holder.media?.visibility = View.GONE
        }
    }

    private fun doLayoutHeaderUserProfile(holder: BasicStatusViewHolder){
        holder.userName?.text = ownerInfo?.screen_name
        if(ownerInfo?.description != null && ownerInfo?.description?.isEmpty() == false){
            holder.userDescription?.text = ownerInfo?.description
            holder.userDescription?.visibility = View.VISIBLE
        } else {
            holder.userDescription?.visibility = View.GONE
        }

        Glide.with(context.applicationContext)
            .load(ownerInfo?.profile_image_url_large)
            .apply(RequestOptions().circleCrop())
            .into(holder.avatar!!)
    }

    private fun actLike(view: View){
        if(App.instance.myself?.friendica_owner == null){
            App.instance.toast(context.getString(R.string.common_error).format(""))
            return
        }

        val me = App.instance.myself?.friendica_owner!!
        val position = (view.parent.parent as ViewGroup).tag as Int
        val st = data[position]

        var likes = st.friendica_activities.like
        val isLike = amILike(st)
        if(isLike){
            likes.remove(me)
        } else {
            likes.add(me)
        }

        doLayoutLikeRelated(view as TextView, position)
        FriendicaUtil.like(!isLike, st.id, object : ILike {
            override fun done() {}
            override fun fail() {
                App.instance.toast(context.getString(R.string.common_error).format(""))
            }
        })
    }

    private fun actComment(view: View) {
        val position = (view.parent.parent as ViewGroup).tag as Int
        val st = data[position]
        var bundle = Bundle()
        bundle.putString(Consts.EXTRA_IN_REPLY_USERNAME, st.friendica_owner.screen_name)
        bundle.putInt(Consts.EXTRA_IN_REPLY_STATUS_ID, st.id)
        val dlg = ComposeDialogFragment()
        dlg.arguments = bundle
        dlg.myShow((context as BaseActivity).supportFragmentManager, Consts.FG_COMPOSE)
    }

    private fun actShare(view: View){
        val position = (view.parent.parent as ViewGroup).tag as Int
        val st = data[position]
        App.instance.toast(context.getString(R.string.not_implement_yet))
    }

    private fun gotoUserLikesPage(pos: Int){

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


open class BasicStatusViewHolder(view: View):  RecyclerView.ViewHolder(view) {
    open var userName:TextView? = view.tv_status_user_name
    open var avatar:ImageView? = view.avatar

    var content:TextView? = view.tv_content
    var datetime:TextView? = view.tv_datetime
    var media:ImageView? = view.media
    val actGroup:View? = view.actGroup
    var comment: TextView? = actGroup?.tv_comment
    var like: TextView? = actGroup?.tv_like
    var share: TextView? = actGroup?.tv_share
    var tv_like_details: TextView? = actGroup?.tv_like_details
    var userDescription:TextView? = view.tv_description
}

class UserProfileViewHolder(view: View): BasicStatusViewHolder(view)
class StatusNoUserInfoViewHolder(view: View): BasicStatusViewHolder(view)
class StatusViewHolder(view: View): BasicStatusViewHolder(view)
class StatusReplyViewHolder(view: View): BasicStatusViewHolder(view)