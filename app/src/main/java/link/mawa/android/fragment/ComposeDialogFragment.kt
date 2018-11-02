package link.mawa.android.fragment

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.dlg_compose.*
import kotlinx.android.synthetic.main.dlg_compose.view.*
import link.mawa.android.App
import link.mawa.android.R
import link.mawa.android.activity.BaseActivity
import link.mawa.android.activity.MainActivity
import link.mawa.android.bean.Status
import link.mawa.android.util.ApiService
import link.mawa.android.util.PrefUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference
import javax.net.ssl.HttpsURLConnection

class ComposeDialogFragment: DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val roomView = inflater?.inflate(R.layout.dlg_compose, container)
        dialog.setTitle("")

        roomView?.et_text?.setText(PrefUtil.getLastStatus())
        roomView?.bt_submit?.setOnClickListener {
            composeSubmit()
        }
        return roomView!!
    }

    override fun onDestroyView() {
        super.onDestroyView()
        PrefUtil.setLastStatus(et_text.text.toString())
    }

    override fun onStart() {
        super.onStart()
        dialog.window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    class StatusUpdateCallback(fragment: ComposeDialogFragment): Callback<Status> {
        private val ref = WeakReference<ComposeDialogFragment>(fragment)
        private val strMsg = ref.get()?.getString(R.string.compose_fail)
        override fun onFailure(call: Call<Status>, t: Throwable) {
            (ref.get()?.activity as BaseActivity).loaded()
            App.instance.toast(strMsg?.format(t.message)!!)
        }

        override fun onResponse(call: Call<Status>, response: Response<Status>) {
            (ref.get()?.activity as BaseActivity).loaded()
            if(response.code() != HttpsURLConnection.HTTP_OK) {
                App.instance.toast(strMsg?.format(response.message())!!)
            } else {
                ref.get()?.composeDone()
            }
        }

    }

    private fun composeSubmit() {
        (activity as BaseActivity).loading(getString(R.string.status_saving))
        ApiService.create().statusUpdate(et_text.text.toString()).enqueue(StatusUpdateCallback(this))
    }

    private fun composeDone() {
        et_text.setText("")
        (activity as MainActivity).loadNewestStatuses()
        dismiss()
    }
}