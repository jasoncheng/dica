package cool.mixi.dica.view

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.style.LeadingMarginSpan



class MyQuoteSpan(private val borderColor:Int, val borderWidth: Int, val gapWidth: Int): LeadingMarginSpan {
    override fun drawLeadingMargin(
        c: Canvas?,
        p: Paint?,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence?,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout?
    ) {
        val style = p?.style
        val color = p?.color

        p?.style = Paint.Style.FILL
        p?.color = borderColor

        c?.drawRect(x.toFloat(), top.toFloat(), (x + dir * borderWidth).toFloat(), bottom.toFloat(), p)

        p?.style = style
        color?.let { p.color = it }
    }

    override fun getLeadingMargin(first: Boolean): Int {
        return borderWidth + gapWidth
    }


}