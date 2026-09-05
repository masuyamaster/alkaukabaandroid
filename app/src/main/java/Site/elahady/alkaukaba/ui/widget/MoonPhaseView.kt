package site.elahady.alkaukaba.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.min

/**
 * Ilustrasi 2D fase bulan saat ini: piringan gelap dengan area terang yang
 * dibentuk dari setengah lingkaran (sisi limb terang) dipotong/ditambah oleh
 * elips terminator, sesuai fraksi iluminasi dan arah waxing/waning.
 */
class MoonPhaseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var illuminatedFraction = 0.5
    private var brightOnRight = true

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#10192A")
        style = Paint.Style.FILL
    }
    private val brightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F6F2E9")
        style = Paint.Style.FILL
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8BA5C")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        alpha = 140
    }

    fun setShadowColor(color: Int) {
        shadowPaint.color = color
        invalidate()
    }

    fun setBrightColor(color: Int) {
        brightPaint.color = color
        invalidate()
    }

    /**
     * @param phaseAngleDegrees sudut fase dari Astronomy Engine (0=bulan baru,
     *   90=kuartal pertama, 180=purnama, 270=kuartal akhir).
     * @param illuminatedFraction fraksi piringan yang tersinari, 0..1.
     */
    fun setPhase(phaseAngleDegrees: Double, illuminatedFraction: Double) {
        val normalizedAngle = ((phaseAngleDegrees % 360.0) + 360.0) % 360.0
        this.illuminatedFraction = illuminatedFraction.coerceIn(0.0, 1.0)
        this.brightOnRight = normalizedAngle < 180.0
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - outlinePaint.strokeWidth
        if (r <= 0f) return

        canvas.drawCircle(cx, cy, r, shadowPaint)

        val k = illuminatedFraction
        when {
            k <= 0.001 -> Unit
            k >= 0.999 -> canvas.drawCircle(cx, cy, r, brightPaint)
            else -> canvas.drawPath(buildLitPath(cx, cy, r, k), brightPaint)
        }

        canvas.drawCircle(cx, cy, r, outlinePaint)
    }

    private fun buildLitPath(cx: Float, cy: Float, r: Float, k: Double): Path {
        val circleRect = RectF(cx - r, cy - r, cx + r, cy + r)
        val brightHalf = Path().apply {
            if (brightOnRight) {
                addArc(circleRect, -90f, 180f)
            } else {
                addArc(circleRect, -90f, -180f)
            }
            lineTo(cx, cy - r)
            close()
        }

        // Setengah moon persis (k=0.5) rawan glitch numerik kalau dipaksa
        // lewat Path.op dengan elips terminator berlebar nol, jadi pakai
        // bentuk setengah lingkaran langsung.
        if (abs(k - 0.5) < 0.003) {
            return brightHalf
        }

        val brightSideRect = if (brightOnRight) {
            RectF(cx, cy - r, cx + r, cy + r)
        } else {
            RectF(cx - r, cy - r, cx, cy + r)
        }
        val darkSideRect = if (brightOnRight) {
            RectF(cx - r, cy - r, cx, cy + r)
        } else {
            RectF(cx, cy - r, cx + r, cy + r)
        }

        val rx = (r * abs(1.0 - 2.0 * k)).toFloat()
        val terminatorOval = Path().apply {
            addOval(RectF(cx - rx, cy - r, cx + rx, cy + r), Path.Direction.CW)
        }

        val litPath = Path()
        if (k < 0.5) {
            // Sabit: potong limb terang dengan bagian elips di sisi yang sama.
            val terminatorOnBrightSide = Path()
            terminatorOnBrightSide.op(terminatorOval, Path().apply { addRect(brightSideRect, Path.Direction.CW) }, Path.Op.INTERSECT)
            litPath.op(brightHalf, terminatorOnBrightSide, Path.Op.DIFFERENCE)
        } else {
            // Cembung: tambahkan bagian elips di sisi gelap ke limb terang.
            val terminatorOnDarkSide = Path()
            terminatorOnDarkSide.op(terminatorOval, Path().apply { addRect(darkSideRect, Path.Direction.CW) }, Path.Op.INTERSECT)
            litPath.op(brightHalf, terminatorOnDarkSide, Path.Op.UNION)
        }
        return litPath
    }
}
