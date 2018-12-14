package cool.mixi.dica.fragment

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.activity.IndexActivity
import cool.mixi.dica.adapter.NotificationAdapter
import cool.mixi.dica.util.FriendicaUtil
import cool.mixi.dica.util.PrefUtil
import kotlinx.android.synthetic.main.dlg_notifications.*
import kotlinx.android.synthetic.main.dlg_notifications.view.*


class NotificationDialog: BaseDialogFragment(), androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener {
    private var rootView: View? = null
    var drawableNotifyOff:Drawable? = null
    var drawableNotifyOn:Drawable? = null
    var strNotifyOff:String? = null
    var strNotifyOn:String? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        drawableNotifyOff = ContextCompat.getDrawable(App.instance.applicationContext, R.drawable.notify_off)
        drawableNotifyOn = ContextCompat.getDrawable(App.instance.applicationContext, R.drawable.notify_on)
        strNotifyOff = getString(R.string.notify_off)
        strNotifyOn = getString(R.string.notify_on)

        rootView = inflater?.inflate(R.layout.dlg_notifications, container)
        rootView?.table?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        rootView?.table?.adapter = NotificationAdapter(this)
        rootView?.swipeRefreshLayout?.setOnRefreshListener(this)
        rootView?.all_seen?.setOnClickListener { markAllAsRead() }
        var decoration = androidx.recyclerview.widget.DividerItemDecoration(
            App.instance.applicationContext,
            androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
        )
        rootView?.table?.addItemDecoration(decoration)

        rootView?.tv_notify?.setOnClickListener {
            PrefUtil.setPollNotification(!PrefUtil.isPollNotification())
            updateNotifyStatus()
            (activity as IndexActivity).setPollNotification()
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshDataSource()
        updateNotifyStatus()
    }

    override fun onDestroyView() {
        (activity as IndexActivity).updateUnreadUI()
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        dialog.window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun updateNotifyStatus(){
        val isEnableNotify = PrefUtil.isPollNotification()
        if(isEnableNotify){
            tv_notify.setCompoundDrawablesWithIntrinsicBounds(drawableNotifyOn, null, null, null)
            tv_notify.text = strNotifyOn
        } else {
            tv_notify.setCompoundDrawablesWithIntrinsicBounds(drawableNotifyOff, null, null, null)
            tv_notify.text = strNotifyOff
        }
    }

    private fun markAllAsRead() {
        var idx = 0
        loop@ App.instance.notifications.forEach {
            if(it.seen == 1) {
                idx++
                return@forEach
            }

            FriendicaUtil.seen(it.id, null)
            it.seen = 1
            rootView?.table?.adapter?.notifyItemChanged(idx)
            idx++
        }
        App.instance.checkIfRequireClearAllNotification()
    }

    fun refreshDataSource() {
        rootView?.swipeRefreshLayout?.isRefreshing = false
        rootView?.table?.adapter?.notifyDataSetChanged()
    }

    override fun onRefresh() {
        rootView?.swipeRefreshLayout?.isRefreshing = true
        (activity as IndexActivity).getNotifications()
    }
}