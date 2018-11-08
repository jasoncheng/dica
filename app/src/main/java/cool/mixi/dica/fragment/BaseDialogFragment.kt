package cool.mixi.dica.fragment

import android.os.Build
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import cool.mixi.dica.util.eLog

open class BaseDialogFragment: DialogFragment() {
    fun myShow(manager: FragmentManager?, tag: String) {
        if (manager == null) {
            return
        }

        if (Integer.valueOf(android.os.Build.VERSION.SDK) > Build.VERSION_CODES.JELLY_BEAN) {
            if (manager.isDestroyed) {
                return
            }
        }

        try {
            val ft = manager.beginTransaction()
            ft.add(this, tag)
            ft.commitAllowingStateLoss()
        } catch (e: Exception) {
            eLog(e.message!!)
        }

    }
}