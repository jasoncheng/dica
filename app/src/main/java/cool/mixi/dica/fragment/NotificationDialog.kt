package cool.mixi.dica.fragment

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.activity.MainActivity
import cool.mixi.dica.adapter.NotificationAdapter
import cool.mixi.dica.bean.Notification
import cool.mixi.dica.util.FriendicaUtil
import kotlinx.android.synthetic.main.dlg_notifications.view.*


class NotificationDialog: BaseDialogFragment(), SwipeRefreshLayout.OnRefreshListener {
    private var rootView: View? = null
    var data: ArrayList<Notification>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater?.inflate(R.layout.dlg_notifications, container)
        rootView?.table?.layoutManager = LinearLayoutManager(context)
        rootView?.table?.adapter = NotificationAdapter(data as java.util.ArrayList<Notification>, this)
        rootView?.swipeRefreshLayout?.setOnRefreshListener(this)
        rootView?.all_seen?.setOnClickListener { markAllAsRead() }
        var decoration = DividerItemDecoration(App.instance.applicationContext, DividerItemDecoration.VERTICAL)
        rootView?.table?.addItemDecoration(decoration)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshDataSource()
    }

    override fun onDestroyView() {
        (activity as MainActivity).updateUnreadUI()
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        dialog.window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun markAllAsRead() {
        var act = activity as MainActivity
        var idx = 0
        loop@ act.notifications.forEach {
            if(it.seen == 1) {
                idx++
                return@forEach
            }

            FriendicaUtil.seen(it.id, null)
            it.seen = 1
            rootView?.table?.adapter?.notifyItemChanged(idx)
            idx++
        }
    }

    fun refreshDataSource() {
        rootView?.swipeRefreshLayout?.isRefreshing = false
        rootView?.table?.adapter?.notifyDataSetChanged()
    }

    override fun onRefresh() {
        rootView?.swipeRefreshLayout?.isRefreshing = true
        (activity as MainActivity).getNotifications()
    }
}