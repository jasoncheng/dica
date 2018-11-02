package link.mawa.android.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_login.*
import link.mawa.android.App
import link.mawa.android.R
import link.mawa.android.bean.Consts
import link.mawa.android.bean.Profile
import link.mawa.android.util.ApiService
import link.mawa.android.util.PrefUtil
import link.mawa.android.util.eLog
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
            PrefUtil.setUsername(et_username.text.toString())
            PrefUtil.setPassword(et_password.text.toString())
            PrefUtil.setApiUrl(et_api.text.toString())
            login()
        }
    }

    fun login() {
        loading(getString(R.string.loading))
        try {
            ApiService.create().friendicaProfileShow().enqueue(MyCallback(this))
        }catch (e: Exception) {
            eLog("======> ${e.message}")
            App.instance.toast(e.message!!)
            loaded()
        }
    }

    fun login_success() {
        var intent = Intent(App.instance.applicationContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        showSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    class MyCallback(activity: LoginActivity): Callback<Profile> {

        private val ref = WeakReference<LoginActivity>(activity)
        private fun failStr(message:String): String? {
            if(ref.get() == null) return null
            var act = ref.get()
            return act?.getString(R.string.login_error)?.format(message)
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
            ref?.get()?.login_success()
            App.instance.myself = response.body()
        }

    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }
}