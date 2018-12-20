package cool.mixi.dica.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.activity.LoginActivity
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.fragment.FriendicaServerListDialog
import kotlinx.android.synthetic.main.friendica_server_list_item.view.*
import java.net.URL

class FriendicaServerListAdapter(private val fragment: FriendicaServerListDialog)
    : androidx.recyclerview.widget.RecyclerView.Adapter<ServerListHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerListHolder {
        var view = LayoutInflater.from(fragment.context).inflate(R.layout.friendica_server_list_item, parent, false)
        view.setOnClickListener { setServerLink(it) }
        return ServerListHolder(view)
    }

    override fun getItemCount(): Int {
        return App.instance.serverList.size
    }

    override fun onBindViewHolder(holder: ServerListHolder, position: Int) {
        var meta = App.instance.serverList[position]
        var host = meta.url
        try {
            host = URL(meta.url).host
        }catch (e: Exception){}
        holder.itemView.tag = position
        holder.serverUrl.text = host
        meta.description.isNullOrEmpty()?.let {
            if(it){
                holder.serverDesc.visibility = View.GONE
            } else {
                holder.serverDesc.visibility = View.VISIBLE
                holder.serverDesc.text = meta.description
            }
        }
    }

    private fun setServerLink(view: View) {
        var position = view.tag as Int
        val meta = App.instance.serverList[position]
        (fragment.activity as LoginActivity).setServerLink(meta.url)
        if(fragment.registrationMode){
            fragment.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${meta.url}${Consts.FRIENDICA_REGISTER_PATH}")))
        }
        fragment.dismissAllowingStateLoss()
    }

}

open class ServerListHolder(view: View): androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
    var serverUrl = view.tv_server_url!!
    var serverDesc = view.tv_server_description!!
}