package cool.mixi.dica.fragment

import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.adapter.UsersAdapter
import cool.mixi.dica.bean.User
import kotlinx.android.synthetic.main.dlg_notifications.view.*

class UsersDialog: BaseDialogFragment(){
    var rootView: View? = null
    var users: ArrayList<User>? = ArrayList()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater?.inflate(R.layout.dlg_users, container)
        rootView?.table?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        rootView?.table?.adapter = UsersAdapter(users as java.util.ArrayList<User>, this)
        var decoration = androidx.recyclerview.widget.DividerItemDecoration(
            App.instance.applicationContext,
            androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
        )
        rootView?.table?.addItemDecoration(decoration)
        return rootView
    }

    override fun onStart() {
        super.onStart()
        dialog.window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}