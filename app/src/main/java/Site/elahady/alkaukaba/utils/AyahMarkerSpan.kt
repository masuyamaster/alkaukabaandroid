package site.elahady.alkaukaba.utils

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan

/** Marker bulat kecil bergaya "akhir ayat" mushaf, digambar manual di [Canvas] - dicoba dulu
 * pakai tanda kurung hias Unicode U+FD3E/FD3F (konvensi teks Mushaf digital, mis. Tanzil), tapi
 * ternyata banyak font Arab bawaan Android (termasuk yang dipakai app ini, tidak ada font
 * kaligrafi Utsmani yang di-bundle) tidak punya glyph untuk karakter itu dan tampil sebagai
 * kotak/tofu. Lingkaran gambar sendiri ini bebas dari masalah dukungan font, dan sekalian
 * dibuat konsisten dengan lingkaran nomor ayat gold di mode Terjemahan ([AyatAdapter]). */
class AyahMarkerSpan(
    private val label: String,
    private val circleColor: Int,
    private val textColor: Int
) : ReplacementSpan() {

    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        val diameter = paint.textSize * 1.15f
        return (diameter + paint.textSize * 0.7f).toInt()
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
        val diameter = paint.textSize * 1.15f
        val radius = diameter / 2f
        val centerX = x + paint.textSize * 0.35f + radius
        val centerY = y - (paint.textSize * 0.32f)

        val circlePaint = Paint(paint).apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = circleColor
        }
        canvas.drawCircle(centerX, centerY, radius, circlePaint)

        val textPaint = Paint(paint).apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            color = textColor
            textSize = paint.textSize * 0.48f
        }
        val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, centerX, textY, textPaint)
    }
}
