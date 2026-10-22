package com.example.calc

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

class SevenSegmentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var text: String = "0."
        set(value) { field = value; invalidate() }
    var lit: Boolean = true
        set(value) { field = value; invalidate() }
    var memoryOn: Boolean = false
        set(value) { field = value; invalidate() }

    private val ink = 0xFF1B2112.toInt()
    private val onPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        setShadowLayer(2.5f, 2f, 2.5f, 0x55000000)
    }
    private val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1A1B2112 }
    private val flagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val path = Path()

    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!lit) return

        val w = width.toFloat()
        val h = height.toFloat()
        val padX = w * 0.04f
        val left = padX + w * 0.07f
        val cellW = (w - left - padX) / CELLS
        val dh = h * 0.70f
        val top = (h - dh) / 2f
        val dw = cellW * 0.62f
        val t = dw * 0.2f
        val ht = t / 2f
        val g = t * 0.14f

        val glyphs = mutableListOf<Int>()
        val dots = mutableListOf<Boolean>()
        for (c in text) {
            if (c == '.') {
                if (glyphs.isEmpty()) { glyphs.add(0); dots.add(true) }
                else dots[dots.lastIndex] = true
            } else {
                glyphs.add(MAP[c] ?: 0)
                dots.add(false)
            }
        }
        val shown = glyphs.size.coerceAtMost(CELLS)
        val first = glyphs.size - shown

        if (memoryOn) {
            flagPaint.textSize = h * 0.2f
            canvas.drawText("M", padX, h * 0.34f, flagPaint)
        }

        canvas.save()
        canvas.translate(h * 0.05f, 0f)
        canvas.skew(-0.12f, 0f)

        for (cell in 0 until CELLS) {
            val gi = cell - (CELLS - shown) + first
            val valid = gi in first until glyphs.size
            val mask = if (valid) glyphs[gi] else 0
            val dp = if (valid) dots[gi] else false

            val x0 = left + cell * cellW + cellW * 0.10f
            val l = x0 + ht
            val r = x0 + dw - ht
            val tp = top + ht
            val bt = top + dh - ht
            val mid = (tp + bt) / 2f

            seg(canvas, mask and 0x01 != 0, hex(l + ht + g, r - ht - g, tp, ht))
            seg(canvas, mask and 0x02 != 0, vert(r, tp + ht + g, mid - ht - g, ht))
            seg(canvas, mask and 0x04 != 0, vert(r, mid + ht + g, bt - ht - g, ht))
            seg(canvas, mask and 0x08 != 0, hex(l + ht + g, r - ht - g, bt, ht))
            seg(canvas, mask and 0x10 != 0, vert(l, mid + ht + g, bt - ht - g, ht))
            seg(canvas, mask and 0x20 != 0, vert(l, tp + ht + g, mid - ht - g, ht))
            seg(canvas, mask and 0x40 != 0, hex(l + ht + g, r - ht - g, mid, ht))

            canvas.drawCircle(x0 + dw + t * 0.55f, bt + ht * 0.2f, t * 0.55f,
                if (dp) onPaint else ghostPaint)
        }
        canvas.restore()
    }

    private fun seg(canvas: Canvas, on: Boolean, p: Path) =
        canvas.drawPath(p, if (on) onPaint else ghostPaint)

    private fun hex(x1: Float, x2: Float, y: Float, ht: Float): Path = path.apply {
        reset()
        moveTo(x1 - ht, y); lineTo(x1, y - ht); lineTo(x2, y - ht)
        lineTo(x2 + ht, y); lineTo(x2, y + ht); lineTo(x1, y + ht); close()
    }

    private fun vert(x: Float, y1: Float, y2: Float, ht: Float): Path = path.apply {
        reset()
        moveTo(x, y1 - ht); lineTo(x + ht, y1); lineTo(x + ht, y2)
        lineTo(x, y2 + ht); lineTo(x - ht, y2); lineTo(x - ht, y1); close()
    }

    companion object {
        private const val CELLS = 9
        private val MAP = mapOf(
            '0' to 0x3F, '1' to 0x06, '2' to 0x5B, '3' to 0x4F, '4' to 0x66,
            '5' to 0x6D, '6' to 0x7D, '7' to 0x07, '8' to 0x7F, '9' to 0x6F,
            '-' to 0x40, 'E' to 0x79, ' ' to 0x00
        )
    }
}