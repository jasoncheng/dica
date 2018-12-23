package cool.mixi.dica.util

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

class DiCaUtil {

    companion object {
        fun hideKeyboard(activity: Activity) {
            val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            var view = activity.currentFocus
            if (view == null) {
                view = View(activity)
            }
            imm!!.hideSoftInputFromWindow(view!!.windowToken, 0)
        }

        fun hideKeyboard(context:Context?, view: View){
            if(context == null){
                return
            }

            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm?.hideSoftInputFromWindow(view?.windowToken, 0)
        }
    }
}