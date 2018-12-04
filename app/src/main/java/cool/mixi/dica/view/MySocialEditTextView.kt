package cool.mixi.dica.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import com.hendraanggrian.appcompat.widget.SocialAutoCompleteTextView

class MySocialEditTextView(context: Context, attrs: AttributeSet): SocialAutoCompleteTextView(context, attrs){
    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode === KeyEvent.KEYCODE_BACK && isPopupShowing) {
            val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (inputManager.hideSoftInputFromWindow(
                    findFocus().windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            ) {
                return true
            }
        }

        return super.onKeyPreIme(keyCode, event)
    }
}