package cool.mixi.dica.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.adapter.FriendicaServerListAdapter
import cool.mixi.dica.bean.Meta
import cool.mixi.dica.util.HtmlCrawler
import cool.mixi.dica.util.IHtmlCrawler
import kotlinx.android.synthetic.main.dlg_notifications.view.*
import java.lang.ref.WeakReference

class FriendicaServerListDialog: BaseDialogFragment(){

    var rootView: View? = null
    var adapter:FriendicaServerListAdapter? = null
    var registrationMode = false
    var pb: ProgressBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater?.inflate(R.layout.dlg_friendica_server_list, container)
        rootView?.table?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        adapter = FriendicaServerListAdapter(this)
        rootView?.table?.adapter = adapter
        var decoration = androidx.recyclerview.widget.DividerItemDecoration(
            App.instance.applicationContext,
            androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
        )
        rootView?.table?.addItemDecoration(decoration)
        pb = rootView?.findViewById(R.id.pb)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ref = WeakReference<FriendicaServerListDialog>(this)
        val servers = HtmlCrawler.getInstance().friendicaServerList(object: IHtmlCrawler{
            override fun done(meta: Meta) {
                ref.get()?.let {
                    it.pb?.visibility = View.GONE
                    it.adapter?.notifyDataSetChanged()
                }
            }
        })

        servers?.let {
            if(it.size > 0){
                adapter?.notifyDataSetChanged()
                ref.get()?.pb?.visibility = View.GONE
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}