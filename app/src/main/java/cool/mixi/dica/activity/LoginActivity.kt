package cool.mixi.dica.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.bean.Profile
import cool.mixi.dica.util.ApiService
import cool.mixi.dica.util.PrefUtil
import cool.mixi.dica.util.eLog
import kotlinx.android.synthetic.main.activity_login.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference
import javax.net.ssl.HttpsURLConnection


class LoginActivity: BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        et_api.hint = Consts.API_HOST
        bt_login.setOnClickListener {
            hideKeyboard(this)
            PrefUtil.setUsername(et_username.text.toString())
            PrefUtil.setPassword(et_password.text.toString())
            PrefUtil.setApiUrl(et_api.text.toString())
            login()
        }

        setMiXiLink()
        setFriendicaLink()
    }

    fun login() {
        loading(getString(R.string.loading))
        try {
            ApiService.create().friendicaProfileShow(null).enqueue(
                MyCallback(
                    this
                )
            )
        }catch (e: Exception) {
            eLog("======> ${e.message}")
            App.instance.toast(e.message!!)
            loaded()
        }
    }

    fun loginSuccess() {
        loaded()
        var intent = Intent(App.instance.applicationContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

    fun setMiXiLink(){
        tv_app_name.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW)
            browserIntent.data = Uri.parse(Consts.MIXI_URL)
            startActivity(browserIntent)
        }
    }

    fun setFriendicaLink(){
        tv_app_description.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW)
            browserIntent.data = Uri.parse(Consts.FRIENDICA_WEB)
            startActivity(browserIntent)
        }
    }

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm!!.hideSoftInputFromWindow(view!!.windowToken, 0)
    }
    class MyCallback(activity: LoginActivity): Callback<Profile> {

        private val ref = WeakReference<LoginActivity>(activity)
        private fun failStr(message:String): String? {
            if(ref.get() == null) return null
            var act = ref.get()
            return act?.getString(R.string.common_error)?.format(message)
        }

        override fun onFailure(call: Call<Profile>, t: Throwable) {
            App.instance.toast(this!!.failStr(t.message!!)!!)
            if(ref.get() == null) return
            ref?.get()?.loaded()
        }

        override fun onResponse(call: Call<Profile>, response: Response<Profile>) {
            if(ref.get() == null) return

            val code = response.code()
            if(code == HttpsURLConnection.HTTP_UNAUTHORIZED){
                App.instance.toast(this!!.failStr(response.code().toString()!!)!!)
                ref?.get()?.loaded()
                return
            }
            ref?.get()?.loginSuccess()
            App.instance.myself = response.body()
        }

    }

}