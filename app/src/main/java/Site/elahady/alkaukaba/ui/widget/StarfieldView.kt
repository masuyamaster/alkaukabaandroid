package site.elahady.alkaukaba.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

/**
 * Latar bintang statis untuk kartu bertema langit malam (mis. Fase Bulan) -
 * dipakai sebagai layer paling belakang di FrameLayout supaya kartu terasa
 * seperti langit gelap penuh bintang, bukan cuma gradient polos + 1-2 titik
 * dekoratif. Posisi/ukuran/kecerahan tiap bintang di-generate sekali dengan
 * seed tetap (bukan di onDraw) supaya tidak "berkedip"/berubah posisi tiap
 * kali View di-invalidate (mis. saat fase Bulan berubah).
 */
class StarfieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val STAR_COUNT = 70
        private const val SEED = 20260905L
    }

    private class Star(val xFrac: Float, val yFrac: Float, val radiusDp: Float, val alpha: Int)

    private val stars = Random(SEED).let { rng ->
        List(STAR_COUNT) {
            Star(
                xFrac = rng.nextFloat(),
                yFrac = rng.nextFloat(),
                radiusDp = 0.4f + rng.nextFloat() * 1.1f,
                alpha = 50 + rng.nextInt(170)
            )
        }
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val density = resources.displayMetrics.density
        for (star in stars) {
            paint.alpha = star.alpha
            canvas.drawCircle(star.xFrac * width, star.yFrac * height, star.radiusDp * density, paint)
        }
    }
}
