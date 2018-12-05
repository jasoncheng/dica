package cool.mixi.dica.view

import android.content.Context
import androidx.viewpager.widget.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

class HackViewPager(context: Context, attrs: AttributeSet): androidx.viewpager.widget.ViewPager(context, attrs) {
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return try {
            super.onInterceptTouchEvent(ev)
        }catch (e: IllegalArgumentException){
            e.printStackTrace()
            false
        }
    }
}