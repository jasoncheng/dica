package cool.mixi.dica.view

import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.widget.TextView
import com.bumptech.glide.Glide
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class WebImageSpan {
    companion object {
        fun getDrawable(url: String, textView: TextView): BitmapDrawable {
            var bitmapDrawable = BitmapDrawable()
            doAsync {
                val featureRequest= Glide.with(textView).load(url).submit(500, 500)
                featureRequest.get().setBounds(0, 0, 500, 500)
                val drawable = (featureRequest.get() as BitmapDrawable)
                val canvas = Canvas(drawable.bitmap)
                uiThread {
                    bitmapDrawable.setBounds(0, 0, 500, 500)
                    bitmapDrawable?.draw(canvas)
                    bitmapDrawable?.invalidateSelf()
                    textView.invalidate()
//                    Glide.with(textView).clear(featureRequest)
                }
            }
            return bitmapDrawable
        }
    }
}