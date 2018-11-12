package cool.mixi.dica.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.ReplacementSpan



class RecyclingRoundSpan(val bgColor: Int, val redius: Int): ReplacementSpan() {
    val padding = 40

    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        return (padding + paint.measureText(text?.subSequence(start, end).toString()) + padding).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val width = paint.measureText(text?.subSequence(start, end).toString())
        val orgColor = paint.color
        val rect = RectF(x - padding, top.toFloat(), x + width + padding, bottom.toFloat())
        paint.color = bgColor
        canvas.drawRoundRect(rect, redius.toFloat(), redius.toFloat(), paint)
        paint.color = orgColor
        canvas.drawText(text, start, end, x, y.toFloat(), paint)
    }
}