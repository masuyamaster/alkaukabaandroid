package site.elahady.alkaukaba.ui.widget

import site.elahady.alkaukaba.model.VisibilityGridPoint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * Peta dunia equirectangular super-sederhana (bentuk benua berupa poligon kasar
 * tangan-gambar, BUKAN data batas negara/garis pantai akurat) + overlay grid warna
 * hasil hisab per sel dari WorldVisibilityCalculator. Tujuannya cuma memberi konteks
 * visual "di mana" tiap sel grid berada, bukan peta geografis presisi — lihat
 * docs/features/peta-visibilitas.md soal keputusan desain ini.
 */
class WorldMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        // Setengah lebar/tinggi sel grid (derajat) — harus sinkron dengan spasi
        // grid di WorldVisibilityCalculator (15° x 15°).
        private const val CELL_HALF_LAT = 7.5
        private const val CELL_HALF_LNG = 7.5

        // Poligon benua sangat disederhanakan (lat, lng) per titik — dibuat manual,
        // sekadar cukup dikenali bentuknya, bukan hasil digitasi peta sungguhan.
        private val CONTINENTS: List<List<DoubleArray>> = listOf(
            // Amerika Utara
            listOf(
                doubleArrayOf(72.0, -155.0), doubleArrayOf(72.0, -75.0), doubleArrayOf(60.0, -52.0),
                doubleArrayOf(45.0, -60.0), doubleArrayOf(25.0, -80.0), doubleArrayOf(15.0, -95.0),
                doubleArrayOf(18.0, -105.0), doubleArrayOf(32.0, -117.0), doubleArrayOf(49.0, -125.0),
                doubleArrayOf(60.0, -140.0)
            ),
            // Amerika Selatan
            listOf(
                doubleArrayOf(12.0, -72.0), doubleArrayOf(10.0, -62.0), doubleArrayOf(-5.0, -35.0),
                doubleArrayOf(-20.0, -40.0), doubleArrayOf(-35.0, -58.0), doubleArrayOf(-55.0, -68.0),
                doubleArrayOf(-50.0, -73.0), doubleArrayOf(-30.0, -71.0), doubleArrayOf(-12.0, -77.0)
            ),
            // Eropa
            listOf(
                doubleArrayOf(71.0, 25.0), doubleArrayOf(60.0, 60.0), doubleArrayOf(45.0, 45.0),
                doubleArrayOf(36.0, 25.0), doubleArrayOf(36.0, -9.0), doubleArrayOf(43.0, -9.0),
                doubleArrayOf(50.0, 0.0), doubleArrayOf(60.0, 5.0)
            ),
            // Afrika
            listOf(
                doubleArrayOf(37.0, -6.0), doubleArrayOf(32.0, 32.0), doubleArrayOf(12.0, 43.0),
                doubleArrayOf(-1.0, 42.0), doubleArrayOf(-26.0, 33.0), doubleArrayOf(-35.0, 20.0),
                doubleArrayOf(-34.0, 18.0), doubleArrayOf(-20.0, 12.0), doubleArrayOf(4.0, 9.0),
                doubleArrayOf(15.0, -17.0), doubleArrayOf(28.0, -16.0)
            ),
            // Asia
            listOf(
                doubleArrayOf(77.0, 60.0), doubleArrayOf(77.0, 180.0), doubleArrayOf(60.0, 180.0),
                doubleArrayOf(50.0, 140.0), doubleArrayOf(35.0, 130.0), doubleArrayOf(20.0, 110.0),
                doubleArrayOf(8.0, 98.0), doubleArrayOf(8.0, 77.0), doubleArrayOf(20.0, 68.0),
                doubleArrayOf(30.0, 48.0), doubleArrayOf(42.0, 35.0), doubleArrayOf(45.0, 30.0),
                doubleArrayOf(55.0, 40.0)
            ),
            // Australia
            listOf(
                doubleArrayOf(-10.0, 113.0), doubleArrayOf(-12.0, 142.0), doubleArrayOf(-22.0, 150.0),
                doubleArrayOf(-38.0, 148.0), doubleArrayOf(-35.0, 116.0), doubleArrayOf(-22.0, 114.0)
            )
        )
    }

    private var points: List<VisibilityGridPoint> = emptyList()

    private val oceanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#BFE3F5") }
    private val landPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E4E0CF")
        style = Paint.Style.FILL
    }
    private val landStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9C3A8")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val memenuhiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66059669") // hijau emerald, semi-transparan
    }
    private val belumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#59B91C1C") // merah, semi-transparan (35% alpha)
    }

    fun setData(newPoints: List<VisibilityGridPoint>) {
        points = newPoints
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(300, widthMeasureSpec)
        // Rasio 2:1 khas proyeksi equirectangular (360° bujur : 180° lintang)
        val height = (width / 2)
        setMeasuredDimension(width, height)
    }

    private fun lngToX(lng: Double, width: Float): Float = ((lng + 180.0) / 360.0 * width).toFloat()
    private fun latToY(lat: Double, height: Float): Float = ((90.0 - lat) / 180.0 * height).toFloat()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, oceanPaint)

        for (continent in CONTINENTS) {
            val path = Path()
            continent.forEachIndexed { index, (lat, lng) ->
                val x = lngToX(lng, w)
                val y = latToY(lat, h)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, landPaint)
            canvas.drawPath(path, landStrokePaint)
        }

        for (point in points) {
            val left = lngToX(point.longitude - CELL_HALF_LNG, w)
            val right = lngToX(point.longitude + CELL_HALF_LNG, w)
            val top = latToY(point.latitude + CELL_HALF_LAT, h)
            val bottom = latToY(point.latitude - CELL_HALF_LAT, h)
            val paint = if (point.memenuhiKriteria) memenuhiPaint else belumPaint
            canvas.drawRect(left, top, right, bottom, paint)
        }
    }

    // Destructuring DoubleArray(lat, lng) di forEachIndexed
    private operator fun DoubleArray.component1() = this[0]
    private operator fun DoubleArray.component2() = this[1]
}
