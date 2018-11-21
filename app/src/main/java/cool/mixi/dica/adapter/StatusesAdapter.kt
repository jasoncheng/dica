package cool.mixi.dica.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.format.DateUtils
import android.text.style.*
import android.text.util.Linkify.ALL
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.activity.BaseActivity
import cool.mixi.dica.activity.StatusActivity
import cool.mixi.dica.activity.UserActivity
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.bean.Meta
import cool.mixi.dica.bean.Status
import cool.mixi.dica.bean.User
import cool.mixi.dica.fragment.ComposeDialogFragment
import cool.mixi.dica.fragment.PhotoViewerFragment
import cool.mixi.dica.fragment.UsersDialog
import cool.mixi.dica.util.*
import cool.mixi.dica.view.MyQuoteSpan
import kotlinx.android.synthetic.main.box_status_action.view.*
import kotlinx.android.synthetic.main.box_status_user.view.*
import kotlinx.android.synthetic.main.box_status_website.view.*
import kotlinx.android.synthetic.main.empty_view.view.*
import kotlinx.android.synthetic.main.rv_user_item_header.view.tv_description
import kotlinx.android.synthetic.main.rv_user_item_header.view.tv_sitename
import kotlinx.android.synthetic.main.status_list_item.view.*
import org.jetbrains.anko.forEachChild
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.SoftReference
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.net.ssl.HttpsURLConnection
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class StatusesAdapter(val data:ArrayList<Status>, val context: Context): RecyclerView.Adapter<BasicStatusViewHolder>() {

    var ownerInfo: User? = null
    var isFavoritesFragment: Boolean = false
    var initLoaded: Boolean = false

    // if this is internal / external Network
    var isOffSiteSN = false

    private val quoteSpanColor: Int = context.getColor(R.color.quote_span_border)
    private val likeDrawable = context.getDrawable(R.drawable.thumb_up_sel)
    private val unlikeDrawable = context.getDrawable(R.drawable.thumb_up)
    private val privateMessage = context.getDrawable(R.drawable.lock)
    private val favoritesDrawable = context.getDrawable(R.drawable.favorites_sel)
    private val unFavoritesDrawable = context.getDrawable(R.drawable.favorites)
    private val statusSourceColor = context.getColor(R.color.txt_status_source)
    private val tagTextColor = context.getColor(R.color.txt_tag)
    private val emailTextColor = context.getColor(R.color.txt_email)
    private val recyclingBG = context.getDrawable(R.drawable.recycling_status_bg)
    private val strSuccessRetweet: String = context.getString(R.string.retweet_success)
    private val strRetweetFail = context.getString(R.string.retweet_fail)
    private val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm")

    enum class ViewType {
        USER_PROFILE,
        STATUS,
        STATUS_SIMPLE,
        REPLY,
        EMPTY
    }

    private val requestOptions = RequestOptions()
        .fitCenter()
        .format(DecodeFormat.PREFER_ARGB_8888)
        .transforms(RoundedCorners(16))!!

    private val requestOptionsGif = RequestOptions()
        .fitCenter()
        .skipMemoryCache(true)
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

    private val compilerQuote: Pattern =  Pattern.compile("^> ([^\n]*)",
        Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL)

    private val compilerTag: Pattern = Pattern.compile("#([^ |#|:|\n]*)",
        Pattern.CASE_INSENSITIVE  or Pattern.MULTILINE or Pattern.DOTALL )

    private val compilerLargeText: Pattern = Pattern.compile("\\*([^\\*]+)\\*",
        Pattern.CASE_INSENSITIVE  or Pattern.MULTILINE or Pattern.DOTALL )

    private var refAdapter: SoftReference<StatusesAdapter> = SoftReference(this)

    override fun onBindViewHolder(holder: BasicStatusViewHolder, position: Int) {
        if(holder is EmptyHolder){
            if(initLoaded){
                holder.emptyDescription?.visibility = View.VISIBLE
            } else {
                holder.emptyDescription?.visibility = View.GONE
            }
            return
        }

        var pos = position
        if(context is UserActivity){
            if(position == 0){
                doLayoutHeaderUserProfile(holder)
                return
            }
            pos = position - 1
        }

        data.getOrNull(pos).let { it ->
            if(it == null) return


            var lockContainer = holder.datetime
            try {
                var date = Date(it.created_at)
                if(context !is UserActivity){
                    doLayoutUserBox(holder, it, pos)
                    holder.datetime?.text = DateUtils.getRelativeTimeSpanString(date.time).toString()
                    lockContainer = holder.userName
                } else {
                    holder.datetime?.let {that->
                        that.text = sdf.format(date)
                        doAppendSourceLayout(that, that.text.toString(), it)
                    }
                }
            }catch (e: java.lang.Exception) {}


            // private message icon
            if(it.friendica_private) {
                lockContainer?.setCompoundDrawablesWithIntrinsicBounds(null, null, privateMessage, null)
            } else {
                lockContainer?.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            }

            doLayoutContent(holder, it, pos)
            holder.geoAddress?.let { doLayoutGeoAddress(it, pos) }
            doLayoutLikeRelated(holder.like!!, pos)
            doLayoutFavorites(holder.favorites!!, pos)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if(context is UserActivity){
            if (position == 0){
                return ViewType.USER_PROFILE.ordinal
            } else if(data.size == 0){
                return ViewType.EMPTY.ordinal
            }
            return ViewType.STATUS_SIMPLE.ordinal
        } else if(data.size == 0){
            return ViewType.EMPTY.ordinal
        }

        val st = data[position]
        return if(st.in_reply_to_status_id != 0){ ViewType.REPLY.ordinal } else { ViewType.STATUS.ordinal }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasicStatusViewHolder {
        val inflater = LayoutInflater.from(context)

        // EmptyView & UserPage HeaderView
        if(viewType == ViewType.USER_PROFILE.ordinal){
            return UserProfileViewHolder(
                inflater.inflate(R.layout.rv_user_item_header, parent, false)
            )
        } else if(viewType == ViewType.EMPTY.ordinal) {
            return if(context is UserActivity) {
                EmptyHolder(inflater.inflate(R.layout.empty_view_user_page, parent, false))
            } else {
                EmptyUserPageHolder(inflater.inflate(R.layout.empty_view, parent, false))
            }
        }

        var holder = when (viewType) {
            ViewType.REPLY.ordinal -> StatusReplyViewHolder(
                inflater.inflate(R.layout.reply_list_item, parent, false)
            )
            ViewType.STATUS_SIMPLE.ordinal -> StatusNoUserInfoViewHolder(
                inflater.inflate(R.layout.rv_user_item, parent, false)
            )
            else -> StatusViewHolder(
                inflater.inflate(R.layout.status_list_item, parent, false)
            )
        }

        // Control click on content
        if(context is StatusActivity){
        } else {
            holder.contentBox?.setOnClickListener { gotoStatusPage(it) }
        }

        // Bottom Action & visibility
        holder.retweet?.setOnClickListener { actRetweet(it) }
        if(isOffSiteSN){
            holder.comment?.visibility = View.GONE
            holder.like?.visibility = View.GONE
            holder.tvLikeDetails?.visibility = View.GONE
            holder.favorites?.visibility = View.GONE
        } else {
            holder.comment?.setOnClickListener { actComment(it) }
            holder.like?.setOnClickListener { actLike(it) }
            holder.tvLikeDetails?.setOnClickListener { gotoUserLikesPage(it) }
            holder.favorites?.setOnClickListener { actFavorites(it) }
        }
        holder.statusMenu?.setOnClickListener { actStatusMenu(it) }
        return holder
    }

    override fun getItemCount(): Int {
        var extraSize = 0

        if(context is UserActivity){
            extraSize = if(data.size > 0){
                1
            } else {
                2
            }
        }

        // for empty view
        if(extraSize == 0 && data.size == 0){
            return 1
        }

        return data.size + extraSize
    }

    private fun amILike(status: Status): Boolean {
        return status.friendica_activities.like.contains(App.instance.myself?.friendica_owner)
    }

    private fun doLayoutLikeRelated(view: TextView, pos: Int){
        var status = data[pos]
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
        }
    }

    private fun doLayoutFavorites(view: TextView, pos: Int){
        var status = data[pos]
        if(status.favorited){
            view.setCompoundDrawablesWithIntrinsicBounds(favoritesDrawable, null, null, null)
        } else {
            view.setCompoundDrawablesWithIntrinsicBounds(unFavoritesDrawable, null, null, null)
        }
    }

    private fun doLayoutUserBox(holder: BasicStatusViewHolder, st: Status, pos: Int){
        holder.userName?.text = st.user.screen_name
        holder.userName?.tag = pos
        holder.userName?.setOnClickListener { gotoUserPage(it) }

        st.source?.let { doAppendSourceLayout(holder.userName!!, st.user.screen_name, st) }

        holder.avatar?.setTag(R.id.avatar_tag, pos)
        holder.avatar?.setOnClickListener { gotoUserPage(it) }
        Glide.with(context.applicationContext)
            .load(st.user.profile_image_url_large)
            .apply(RequestOptions().circleCrop())
            .into(holder.avatar!!)
    }

    private fun doAppendSourceLayout(view:TextView, orgStr: String, st: Status) {
        val it = st.source!!
        if(it.isEmpty() || it.isBlank()) return

        // for Friendica legacy version API
        val source = it.replace(" \\(\\)".toRegex(), "").trim()

        val str = context.getString(R.string.status_source).format(source)
        val start = orgStr.length
        val end = start + str.length
        var sp = SpannableString("$orgStr$str")
        var color = ForegroundColorSpan(statusSourceColor)
        try {
            sp.setSpan(color, start, end, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
            sp.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
            sp.setSpan(RelativeSizeSpan(0.8f), start, end, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
            view?.text = sp
        }catch (e: Exception){}
    }

    private fun doLayoutGeoAddress(view: TextView, pos: Int){
        var status = data[pos]
        if(status.geo?.address != null){
            var address = status.geo?.address
            view.visibility = View.VISIBLE
            view.text = "${address?.getAddressLine(0)} "
            view.setOnClickListener {
                context.startActivity(LocationUtil.mapIntent(address!!))
            }
        } else {
            view.visibility = View.GONE
            view.text = ""
            view.setOnClickListener { null }
        }
    }

    private fun doLayoutContent(holder: BasicStatusViewHolder, st: Status, pos: Int){
        holder.actGroup?.tag = pos
        holder.contentBox?.tag = pos
        holder.itemView.tag = pos
        holder.contentBox?.isClickable = true
        renderContent(holder.contentBox, st)
    }

    private fun doLayoutHeaderUserProfile(holder: BasicStatusViewHolder){
        holder.userName?.text = ownerInfo?.screen_name
        if(ownerInfo?.description != null && ownerInfo?.description?.isEmpty() == false){
            holder.userDescription?.text = ownerInfo?.description
            holder.userDescription?.visibility = View.VISIBLE
        } else {
            holder.userDescription?.visibility = View.GONE
        }

        if(isOffSiteSN){
            holder.siteName?.visibility = View.VISIBLE
            holder.siteName?.text = ownerInfo?.getDomain()
            holder.siteName?.setOnClickListener {
                ownerInfo?.url.let {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it!!)))
                }
            }
        } else {
            holder.siteName?.visibility = View.GONE
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
        data.getOrNull(position).let {
            if(it == null) return
            val st = it!!
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
    }

    private fun menuDoDelete(st: Status){
        AlertDialog.Builder(context)
            .setMessage(R.string.confirm_delete_status)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val activity = context as BaseActivity
                activity.loading(context.getString(R.string.processing))
                ApiService.create().statusDestroy(st.id).enqueue(StatusDestroyCallback(refAdapter, st))
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _->
                dialog.dismiss()
            }.show()
    }

    private fun menuDoOpenLink(url: String){
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }catch (e: Exception){}
    }

    private fun actStatusMenu(view: View){
        val me = App.instance.myself?.friendica_owner!!
        var position = (view.parent.parent as ViewGroup).tag as? Int
        if(position == null){
            position = (view.parent.parent.parent.parent as ViewGroup).tag as? Int
        }

        data.getOrNull(position!!).let {
            if(it == null) return

            var pop = PopupMenu(context, view)
            var inflater = pop.menuInflater
            inflater.inflate(R.menu.status_menu, pop.menu)
            if(it.user != me) {
                pop.menu.removeItem(R.id.menu_delete)
            }
            pop.setOnMenuItemClickListener { that ->
                when(that.itemId) {
                    R.id.menu_delete -> menuDoDelete(it)
                    R.id.menu_open_link -> menuDoOpenLink(it.external_url)
                }
                true
            }
            pop.show()
        }
    }

    private fun actFavorites(view: View){
        val me = App.instance.myself?.friendica_owner!!
        val position = (view.parent.parent as ViewGroup).tag as Int
        data.getOrNull(position).let {
            if(it == null) return

            val st = it!!
            st.favorited = !st.favorited
            doLayoutFavorites(view as TextView, position)
            FriendicaUtil.favorites(st.favorited, st.id)
            if(!st.favorited && isFavoritesFragment){
                data.removeAt(position)
                notifyItemRemoved(position)
            }
        }
    }

    private fun actComment(view: View) {
        val position = (view.parent.parent as ViewGroup).tag as Int
        data.getOrNull(position).let {
            if(it == null) return

            val st = it!!
            var bundle = Bundle()
            bundle.putString(Consts.EXTRA_IN_REPLY_USERNAME, st.friendica_owner.screen_name)
            bundle.putInt(Consts.EXTRA_IN_REPLY_STATUS_ID, st.id)
            val dlg = ComposeDialogFragment()
            dlg.arguments = bundle
            dlg.myShow((context as BaseActivity).supportFragmentManager, Consts.FG_COMPOSE)
        }
    }

    private fun actRetweet(view: View){
        val position = (view.parent.parent as ViewGroup).tag as Int
        var dlg : AlertDialog?
        data.getOrNull(position).let {
            if(it == null) return
            var view =  LayoutInflater.from(context).inflate(R.layout.loading_dialog, null)
            view.findViewById<TextView>(R.id.tv_loading).text = context.getString(R.string.retweeting)
            val builder = AlertDialog.Builder(context)
            builder.setCancelable(true).setView(view)
            dlg = builder.show()


            if(isOffSiteSN){
                dLog("offSite retweet")
                val text = it.toFriendicaShareText()
                dLog("${it.toFriendicaShareText()}")
                ApiService.create()
                    .statusUpdate(context.getString(R.string.app_name), text, 0, "", "", ArrayList())
                    .enqueue(StatusUpdateCallback(dlg, refAdapter))
                return
            }

            FriendicaUtil.retweet(it.id, object: IRetweet {
                override fun done() {
                    dlg?.dismiss()
                    App.instance.toast(strSuccessRetweet)
                }

                override fun fail(reason: String) {
                    dlg?.dismiss()
                    App.instance.toast(strRetweetFail.format(reason))
                }
            })
        }
    }

    private class StatusUpdateCallback(val dialog: AlertDialog?, val adapter: SoftReference<StatusesAdapter>): Callback<String> {
        override fun onFailure(call: Call<String>, t: Throwable) {
            dialog?.dismiss()
            adapter.get()?.let {
                App.instance.toast(it.strRetweetFail!!.format("${t.message}"))
            }
        }

        override fun onResponse(call: Call<String>, response: Response<String>) {
            dialog?.dismiss()
            if(response.code() != HttpsURLConnection.HTTP_OK) {
                adapter.get()?.let {
                    App.instance.toast(it.strRetweetFail.format("${response.code()}"))
                }
                return
            }

            adapter.get()?.let {
                App.instance.toast(it.strSuccessRetweet)
            }
        }
    }

    private fun gotoUserLikesPage(view: View){
        val position = (view.parent as ViewGroup).tag as Int
        data.getOrNull(position).let {
            if(it == null) return

            val st = it!!
            val dlg = UsersDialog()
            dlg.users = st.friendica_activities.like
            dlg.myShow((context as BaseActivity).supportFragmentManager, Consts.FG_USERS)
        }
    }

    private fun gotoUserPage(view: View) {
        var position: Int = if(view is ImageView){
            view.getTag(R.id.avatar_tag) as Int
        } else {
            view.tag as Int
        }

        data.getOrNull(position).let {
            if(it == null) return
            val i = Intent(context, UserActivity::class.java)
            i.putExtra(Consts.EXTRA_USER, it!!.user)
            context.startActivity(i)
        }
    }

    fun gotoUserPage(email: String) {
        val i = Intent(context, UserActivity::class.java)
        i.putExtra(Consts.EXTRA_USER_EMAIL, email)
        context.startActivity(i)
    }

    private fun gotoStatusPage(view: View){
        var position: Int = if(view is ImageView){
            view.getTag(R.id.media_image) as Int
        } else {
            view.tag as Int
        }

        data.getOrNull(position).let {
            if(it == null) return
            val i = Intent(context, StatusActivity::class.java)
            i.putExtra(Consts.ID_STATUS, it!!.id)
            context.startActivity(i)
        }
    }

    private fun renderContent(parent: ViewGroup?, status: Status){
        if(parent == null || status.text == null) return
        parent.removeAllViews()

        // encode proxy photo if exists
        var tmpPartialPhoto = HashMap<String, String>()
        var displayedUrl = ArrayList<String>()
        status.attachments?.forEach {
            var partialUrl = FriendicaUtil.getProxyUrlPartial(it.url)
            tmpPartialPhoto[partialUrl] = it.url
        }

        // Style: Background
        if(status.text.startsWith("♲")){
            parent.background = recyclingBG
        } else {
            parent.background = null
        }

        // Style: Quote / TAG / Photo / Website
        val lines = status.text.trim().lines()
        var isPureText = false
        var txtAr = ArrayList<String>()

        lines.forEachIndexed { index, it ->
            if(it.startsWith("http", true)){
                if(isPureText){
                    renderText(parent, txtAr)
                    txtAr.clear()
                }
                isPureText = false
                displayedUrl.add(it)
                renderUrl(parent, status, it)
            } else {
                isPureText = true
                txtAr.add(it)
            }
        }
        renderText(parent, txtAr)

        // Style: Extra photos (show extra attachment photo, if not wrap into status.text)
        tmpPartialPhoto.keys.forEach {
            var photoUrl = tmpPartialPhoto[it]
            var photoDisplayed = false
            for(that in displayedUrl) {
                if(that.contains(it)){
                    photoDisplayed = true
                    break
                }
            }

            if(!photoDisplayed && !displayedUrl.contains(photoUrl)){
                renderUrl(parent, status, photoUrl!!)
            }
        }
    }

    private fun mediaPhotoClk(view: View) {
        if(context !is UserActivity && context !is StatusActivity) {
            ((view.parent as ViewGroup).callOnClick())
            return
        }

        if(context is UserActivity && !context.isOffSiteSN){
            return
        }

        val targetUrl = view.getTag(R.id.media_image)
        var targetIndex = 0
        var index = 0
        val arr = ArrayList<String>()
        (view.parent as ViewGroup).forEachChild {
            if(it is ImageView){
                val thisUrl = it.getTag(R.id.media_image) as String
                arr.add(thisUrl)
                if(thisUrl == targetUrl) { targetIndex = index }
                index+=1
            }
        }

        var b = Bundle()
        b.putSerializable(Consts.EXTRA_PHOTOS, arr)
        b.putInt(Consts.EXTRA_PHOTO_INDEX, targetIndex)
        val fg = PhotoViewerFragment()
        fg.arguments = b
        fg.myShow((context as BaseActivity).supportFragmentManager, Consts.FG_PHOTO_VIEWER)
    }

    // Image or Website
    private fun renderUrl(parent:ViewGroup, status: Status, url: String){
        val urlLower = url.toLowerCase()
        if(urlLower.contains("\\.(jpg|gif|jpeg|png)".toRegex()) ||
            // unsplash.com
            urlLower.contains("\\/photo(.*)utm_medium".toRegex()) ||
            // friendica proxy photo
            urlLower.contains("\\/proxy\\/".toRegex()) ) {
            var img = getImageView()
            img.setOnClickListener { mediaPhotoClk(it) }
            img.setTag(R.id.media_image, url)
            parent.addView(img)

            if(urlLower.contains("\\.gif".toRegex())){
                Glide.with(context.applicationContext).load(url.glideUrl()).apply(requestOptionsGif).into(img)
            } else {
                Glide.with(context.applicationContext).load(url.glideUrl()).apply(requestOptions).into(img)
            }
        } else {
            val meta = HtmlCrawler.getInstance().get(url)
            if(meta != null){
                renderWebUrl(parent, meta)
            } else {
                renderText(parent, url)
                HtmlCrawler.getInstance().run(url, MyHtmlCrawler(status, refAdapter))
            }
        }
    }

    // Pure Text (QUOTE / Recycling / TAG)
    private fun renderText(parent: ViewGroup, txt: String){
        txt.trim().isNullOrEmpty().let {
            if(it) return
        }

        val ar = ArrayList<String>()
        ar.add(txt)
        renderText(parent, ar)
    }

    private fun renderText(parent:ViewGroup, txtArr: ArrayList<String>){
        if(txtArr.size == 0) return
        if(txtArr.size == 1){
            txtArr[0].trim().isNullOrEmpty().let {
                if(it) return
                if(txtArr[0] == "\n") return
            }
        }
        var str = txtArr.joinToString("\n")
        var txt = getTextView()
        txt.text = getTextSpan(str)
        parent.addView(txt)
    }

    private fun getTextSpan(str: String): SpannableString {
        var s = str
        var sp:SpannableString

        // Style: QUOTE
        val m = compilerQuote.matcher(s)
        var indexAr = ArrayList<IntArray>()
        while(m.find()){
            val str = m.group()
            var strNew = str.replaceFirst("> ".toRegex(), "").trim()
            val start = s.indexOf(str)
            val end = start+strNew.length
            if(start > 0 && end <= s.length) {
                indexAr.add(arrayOf(start, end).toIntArray())
                s = s.replace(str, strNew, true)
            }
        }
        sp = SpannableString(s)
        indexAr.forEach {
            sp.setSpan(
                MyQuoteSpan(quoteSpanColor, 20, 30), it[0], it[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // Style: TAG
        val mTag = compilerTag.matcher(s)
        while(mTag.find()){
            sp.setSpan(
                ForegroundColorSpan(tagTextColor),
                mTag.start()+1,
                mTag.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            sp.setSpan(
                TagClickSpan(tagTextColor),
                mTag.start()+1,
                mTag.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Style: Mentions
        val mMentions = s.emails()
        for(it in mMentions){
            var start = s.indexOf(it, 0, true)
            var end = start+it.length
            sp.setSpan(RelativeSizeSpan(1.2f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp.setSpan(OffSiteUserClickSpan(emailTextColor, refAdapter), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            sp.setSpan(NoUnderLinSpan(it), start, end, 0)
        }

        // Style: Bold & Large *.....*
        val mLarge = compilerLargeText.matcher(s)
        var indexAr2 = ArrayList<IntArray>()
        while(mLarge.find()){
            val str = mLarge.group()
            val start = s.indexOf(str)
            val end = start+str.length
            indexAr2.add(arrayOf(start, end).toIntArray())
        }
        indexAr2.forEach {
            sp.setSpan(
                RelativeSizeSpan(1.2f),
                it[0],
                it[1],
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            sp.setSpan(
                AbsoluteSizeSpan(0),
                it[0],
                it[0]+1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            sp.setSpan(
                AbsoluteSizeSpan(0),
                it[1]-1,
                it[1],
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return sp
    }

    private fun renderWebUrl(parent:ViewGroup, meta: Meta) {
        if(meta.icon.isNullOrEmpty() || meta.title.isNullOrEmpty()){
            return renderText(parent, meta.url)
        }

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.box_status_website, null)
        val childParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        childParams.setMargins(0, 20, 0, 20)
        view.layoutParams = childParams

        Glide.with(context.applicationContext).load(meta.icon).into(view.site_avatar)
        if(meta.description.isNullOrEmpty()){
            view.site_desc.text = meta.url
        } else {
            view.site_desc.text = meta.description
        }
        view.site_title.text = meta.title
        view.tag = meta.url
        view.setOnClickListener {
            it?.tag.let {url ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url as String)))
            }
        }
        parent.addView(view)
    }

    private fun getImageView(): ImageView {
        var img = ImageView(context)
        val childParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        img.layoutParams = childParams
        img.scaleType = ImageView.ScaleType.FIT_XY
        img.adjustViewBounds = true
        childParams.setMargins(0, 20, 0, 20)
        return img
    }

    private fun getTextView(): TextView {
        var txt = TextView(context)
        val childParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        txt.layoutParams = childParams
        txt.autoLinkMask = ALL
        txt.setPadding(10, 10, 10, 10)
        txt.setTextIsSelectable(true)
        txt.paintFlags = 0
        txt.includeFontPadding = false
        txt.setOnClickListener {
            (it.parent as ViewGroup).callOnClick()
        }
        return txt
    }
}

class MyHtmlCrawler(private val st: Status, val adapter: SoftReference<StatusesAdapter>?): IHtmlCrawler {
    override fun done(meta: Meta) {
        adapter?.get()?.let {
            it.data.forEachIndexed { index, status ->
                if(st != status) { return@forEachIndexed }
                it.notifyItemChanged(index)
            }
        }
    }
}

class StatusDestroyCallback(val adapter: SoftReference<StatusesAdapter>?, private val st: Status): Callback<String> {
    private var errorMsg:String? = "${adapter?.get()?.context?.getString(R.string.common_error)}"
    override fun onFailure(call: Call<String>, t: Throwable) {
        App.instance.toast(errorMsg!!.format(t.message))
    }

    override fun onResponse(call: Call<String>, response: Response<String>) {
        if(response.code() != HttpsURLConnection.HTTP_OK){
            App.instance.toast(errorMsg!!.format(response.code().toString()))
            return
        }

        var activity = adapter?.get()?.context as? BaseActivity
        activity?.loaded()
        adapter?.get()?.let {
            if(it == null) return
            var targetStatus:Status? = null
            var targetIndex = -1
            adapter?.get()?.data?.forEachIndexed { index, status ->
                if(status == st){
                    targetIndex = index
                    targetStatus = status
                }
            }

            //TODO: for user page have profile at index 0
            if(activity is UserActivity){
                targetIndex+=1
            }
            targetStatus?.let { that ->
                it.data.remove(that)
                it.notifyItemRemoved(targetIndex)
            }
        }
    }
}

class TagClickSpan(private val linkColor: Int): ClickableSpan() {
    override fun onClick(widget: View?) {
        val sp = ((widget as TextView).text as Spanned)
        val start = sp.getSpanStart(this)
        val end = sp.getSpanEnd(this)
        App.instance.toast(widget.context.getString(R.string.no_api_support)+" #${sp.subSequence(start, end)}")
    }

    override fun updateDrawState(ds: TextPaint?) {
        super.updateDrawState(ds)
        ds?.color = linkColor
        ds?.linkColor = linkColor
        ds?.isUnderlineText = false
    }
}

class NoUnderLinSpan(val url: String): URLSpan(url) {
    override fun updateDrawState(ds: TextPaint?) {
        super.updateDrawState(ds)
        ds?.isUnderlineText = false
    }
}

class OffSiteUserClickSpan(private val linkColor: Int, val adapter: SoftReference<StatusesAdapter>?): ClickableSpan() {
    override fun onClick(widget: View?) {
        val sp = ((widget as TextView).text as Spanned)
        val start = sp.getSpanStart(this)
        val end = sp.getSpanEnd(this)
        var email = sp.subSequence(start, end).toString()
        adapter?.get()?.gotoUserPage(email)
    }

    override fun updateDrawState(ds: TextPaint?) {
        super.updateDrawState(ds)
        ds?.color = linkColor
        ds?.linkColor = linkColor
        ds?.isUnderlineText = false
    }
}

open class BasicStatusViewHolder(view: View):  RecyclerView.ViewHolder(view) {
    open var userName:TextView? = view.tv_status_user_name
    open var avatar:ImageView? = view.avatar
    var statusMenu: ImageView? = view.more_options
    var contentBox: ViewGroup? = view.content_box
    var emptyDescription: TextView? = view.tv_empty
    var siteName: TextView? = view.tv_sitename
    var datetime:TextView? = view.tv_datetime
    val actGroup:View? = view.actGroup
    var comment: TextView? = actGroup?.tv_comment
    var like: TextView? = actGroup?.tv_like
    var retweet: TextView? = actGroup?.tv_retweet
    var favorites: TextView? = actGroup?.tv_favorites
    var tvLikeDetails: TextView? = actGroup?.tv_like_details
    var userDescription:TextView? = view.tv_description
    var geoAddress: TextView? = view.tv_geo_address
}

class UserProfileViewHolder(view: View): BasicStatusViewHolder(view)
class StatusNoUserInfoViewHolder(view: View): BasicStatusViewHolder(view)
class StatusViewHolder(view: View): BasicStatusViewHolder(view)
class StatusReplyViewHolder(view: View): BasicStatusViewHolder(view)

open class EmptyHolder(view: View): BasicStatusViewHolder(view)
class EmptyUserPageHolder(view: View): EmptyHolder(view)