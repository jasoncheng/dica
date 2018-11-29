package cool.mixi.dica.activity

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPager
import android.support.v7.widget.PopupMenu
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.adapter.IndexPageAdapter
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.bean.Meta
import cool.mixi.dica.bean.Notification
import cool.mixi.dica.bean.Profile
import cool.mixi.dica.fragment.ComposeDialogFragment
import cool.mixi.dica.fragment.NotificationDialog
import cool.mixi.dica.util.*
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import java.net.URLEncoder
import java.util.regex.Pattern
import javax.net.ssl.HttpsURLConnection

class MainActivity : BaseActivity() {

    var notifications: ArrayList<Notification> = ArrayList()
    private val mHandler = Handler()
    private var mNotificationRunnable: NotificationRunnable? = null
    private var snackBar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Notification
        mNotificationRunnable = NotificationRunnable(this)

        // if user not login yet
        if(App.instance.myself == null){
            if(!PrefUtil.didSetUserCredential()){
                logout()
                return
            }

            try {
                ApiService.create().friendicaProfileShow(null).enqueue(
                    ProfileCallback(
                        this
                    )
                )
            }catch (e: Exception){
                dLog("${e.message}")
                App.instance.toast(getString(R.string.common_error).format("${e.message}"))
                logout()
                return
            }
        }

        setContentView(R.layout.activity_main)

        // compose
        iv_compose.setOnClickListener {
            ComposeDialogFragment().show(supportFragmentManager, Consts.FG_COMPOSE)
        }

        // avatar
        home_avatar.setOnClickListener {
            val unreadCount = getUnSeenCount()
            if(unreadCount > 0){
                showNotifications()
                return@setOnClickListener
            }

            var pop = PopupMenu(this, it)
            var inflater = pop.menuInflater
            inflater.inflate(R.menu.index_avatar_menu, pop.menu)
            pop.setOnMenuItemClickListener { it ->
                when(it.itemId) {
                    R.id.menu_logout -> logout()
                    R.id.menu_notifications -> showNotifications()
                }
                true
            }
            pop.show()
        }

        setAvatar()

        if(App.instance.myself != null){
            initViewPager()
        }


        // TODO: fetch site information for update title (no API)
        val homeName = PrefUtil.getSiteName()
        if(!homeName.isNullOrEmpty() && homeName != getString(R.string.app_name)){
            home_title?.text = homeName
        } else {
            HtmlCrawler.getInstance().run(
                PrefUtil.getApiUrl(),
                MyHtmlCrawler(this)
            )
        }

        // Intent Process
        processIntent()

        // fetch session
        getSessionID()
    }

    override fun onStart() {
        super.onStart()
        getNotifications()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processIntent()
    }

    private fun processIntent(){
        if(intent == null || intent.action != Intent.ACTION_SEND){ return }
        val dlg = ComposeDialogFragment()
        if("text/plain".equals(intent.type)){
            dlg.sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        } else if(intent.type.contains("(image|video)\\/".toRegex())){
            val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            var path:String = imageUri.path.toLowerCase()
            val pattern = Pattern.compile("\\.(jpg|jpeg|gif|bmp|mp4)\$", Pattern.CASE_INSENSITIVE)
            if(! pattern.matcher(path).matches()){
                path = getRealPathFromURI(imageUri)
            }
            dlg.sharedFile = path
        }
        dlg.show(supportFragmentManager, Consts.FG_COMPOSE)
    }

    private fun getRealPathFromURI(contentUri: Uri): String {
        var cursor: Cursor? = null
        try {
            val tmp = arrayOf(MediaStore.Images.Media.DATA)
            cursor = contentResolver.query(contentUri, tmp, null, null, null)
            val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(columnIndex)
        } finally {
            cursor?.close()
        }
    }

    fun initViewPager(){
        val names = resources.getStringArray(R.array.index_tab)
        vp_index.adapter = IndexPageAdapter(this, supportFragmentManager)
        vp_index.setOnPageChangeListener(object: ViewPager.OnPageChangeListener{
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageSelected(position: Int) {
                hideSnackBar()
                tv_home_page_name.text = names[position]
            }
        })
    }

    class MyHtmlCrawler(val activity: MainActivity): IHtmlCrawler {
        private val ref = WeakReference<MainActivity>(activity)
        override fun done(meta: Meta) {
            if(!meta.title.isNullOrEmpty()){
                var title = meta.title!!.replace(" \\(home\\)".toRegex(), "")
                PrefUtil.setSiteName(title)
                ref.get()?.let { it.home_title.text = title }
            }
            if(!meta.icon.isNullOrEmpty()){
                PrefUtil.setSiteIcon(meta.icon!!)
            }
        }
    }

    private fun setAvatar(){
        Glide.with(applicationContext)
            .load(App.instance.myself?.friendica_owner?.profile_image_url_large)
            .apply(RequestOptions().circleCrop())
            .into(home_avatar)
    }

    private fun logout() {
        App.instance.myself = null
        App.instance.mygroup = null
        PrefUtil.resetAll()
        goLogin()
        finish()
    }

    private fun goLogin(){
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    class ProfileCallback(activity: MainActivity): Callback<Profile> {
        private val ref = WeakReference<MainActivity>(activity)
        override fun onFailure(call: Call<Profile>, t: Throwable) {
            if(ref.get() == null){ return}
        }

        override fun onResponse(call: Call<Profile>, response: Response<Profile>) {
            if(ref.get() == null){ return}
            val act = ref.get()!!
            if( response.code() == HttpsURLConnection.HTTP_UNAUTHORIZED ){
                act.logout()
                return
            }

            App.instance.myself = response.body()
            act.setAvatar()
            act.initViewPager()
        }

    }

    // Notifications start here

    class MyNotificationCallback(activity: MainActivity): Callback<List<Notification>> {
        private val ref = WeakReference<MainActivity>(activity)
        override fun onFailure(call: Call<List<Notification>>, t: Throwable) {}
        override fun onResponse(call: Call<List<Notification>>, response: Response<List<Notification>>) {
            if(ref.get() == null) { return }
            val activity = ref.get()
            activity?.notifications?.clear()
            response.body()?.forEach {
                activity?.notifications?.add(it)
            }
            activity?.getNotificationDialog()?.refreshDataSource()
            activity?.updateUnreadUI()
        }
    }

    fun updateUnreadUI() {
        ViewCompat.setElevation(tv_unread, 9.toFloat())
        val count = getUnSeenCount()
        if(count == 0){
            tv_unread.visibility = View.GONE
        } else {
            tv_unread.visibility = View.VISIBLE
            tv_unread.text = count.toString()
            tv_unread.setOnClickListener{ showNotifications() }
        }
    }

    private fun getNotificationDialog(): NotificationDialog? {
        return supportFragmentManager.findFragmentByTag(Consts.EXTRA_NOTIFICATIONS) as? NotificationDialog
    }

    private fun showNotifications() {
        val dlg = NotificationDialog()
        dlg.data = notifications
        dlg.myShow(supportFragmentManager, Consts.EXTRA_NOTIFICATIONS)
    }

    private fun getUnSeenCount(): Int {
        var c = 0
        notifications.forEach {
            if(it.seen == 0) c++
        }
        return c
    }


    // Top SnackBar
    fun showSnackBar(msg: String){
        snackBar = Snackbar.make(vp_index, msg, Snackbar.LENGTH_LONG)
        snackBar?.view?.setBackgroundColor(getColor(R.color.snack_bar_bg))
        snackBar?.show()
    }

    fun hideSnackBar(){
        snackBar?.dismiss()
    }

    // for Notification
    class NotificationRunnable(val activity: MainActivity): Runnable {
        private val ref = SoftReference(activity)
        override fun run() {
            ref.get()?.let {
                dLog("friendicaNotifications fetching")
                ApiService.create().friendicaNotifications().enqueue(MyNotificationCallback(it))
            }
        }
    }

    fun getNotifications(){
        mHandler.removeCallbacks(mNotificationRunnable)
        mNotificationRunnable?.let {
            mHandler.postDelayed(it, 5000)
        }
    }

    class MyWebClient: WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            return false
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            val cookieManager = CookieManager.getInstance()
            val cookieStr = cookieManager.getCookie(url)
            ApiService.setCookie(cookieStr)
        }
    }

    private fun getSessionID(){
        doAsync {
            uiThread {
                val web = WebView(this@MainActivity)
                web.webViewClient = MyWebClient()
                val url = "${PrefUtil.getApiUrl()}/login"
                val postData = "username=" + URLEncoder.encode(PrefUtil.getUsername(), "UTF-8") +
                        "&password=" + URLEncoder.encode(PrefUtil.getPassword(), "UTF-8")
                web.postUrl(url, postData.toByteArray())
            }
        }
    }
}