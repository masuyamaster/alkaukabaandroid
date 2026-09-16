package site.elahady.alkaukaba.ui.widget

import site.elahady.alkaukaba.model.VisibilityGridPoint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import org.json.JSONArray

/**
 * Peta dunia equirectangular sederhana (bentuk benua dari data garis pantai
 * sungguhan resolusi rendah, lihat [loadLandPolygons], BUKAN peta geografis
 * presisi/batas negara) + overlay grid warna hasil hisab per sel dari
 * WorldVisibilityCalculator. Tujuannya cuma memberi konteks visual "di mana"
 * tiap sel grid berada — lihat docs/features/peta-visibilitas.md soal
 * keputusan desain ini.
 */
class WorldMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        // Setengah lebar/tinggi sel grid (derajat) — harus sinkron dengan spasi
        // grid di WorldVisibilityCalculator (5° x 5°).
        private const val CELL_HALF_LAT = 2.5
        private const val CELL_HALF_LNG = 2.5

        // Asset: garis pantai dunia dari Natural Earth 110m ("ne_110m_land", domain publik),
        // diminifikasi jadi [[[lng,lat,lng,lat,...], ring2, ...], polygon2, ...] -- array polygon,
        // tiap polygon array ring (ring pertama = outer, sisanya = lubang mis. Laut Kaspia),
        // tiap ring array flat lng/lat berselang-seling, dibulatkan 2 desimal (~1km, lebih dari
        // cukup untuk peta seukuran layar HP). Menggantikan poligon tangan-gambar versi v1 yang
        // bentuknya kasar/tidak akurat (lihat known limitation lama di
        // docs/features/peta-visibilitas.md).
        private const val LAND_ASSET = "world_land_110m.json"
    }

    // Lazy (bukan di constructor) supaya baca+parse asset (~66KB, sekali saja) tidak menunda
    // inflate View kalau ternyata belum perlu digambar; aman dipanggil dari onDraw karena View
    // selalu di-invalidate ulang setelah pertama kali (lihat setData()).
    private val landPolygons: List<List<DoubleArray>> by lazy(LazyThreadSafetyMode.NONE) { loadLandPolygons() }

    /** @return list polygon, tiap polygon = list ring (flat lng/lat), atau list kosong kalau asset gagal dibaca/parse. */
    private fun loadLandPolygons(): List<List<DoubleArray>> = try {
        val json = context.assets.open(LAND_ASSET).bufferedReader().use { it.readText() }
        val polygonsJson = JSONArray(json)
        List(polygonsJson.length()) { i ->
            val ringsJson = polygonsJson.getJSONArray(i)
            List(ringsJson.length()) { j ->
                val flat = ringsJson.getJSONArray(j)
                DoubleArray(flat.length()) { k -> flat.getDouble(k) }
            }
        }
    } catch (e: Exception) {
        emptyList()
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

        for (polygon in landPolygons) {
            val path = Path().apply { fillType = Path.FillType.EVEN_ODD }
            for (ring in polygon) {
                var i = 0
                while (i < ring.size) {
                    val x = lngToX(ring[i], w)
                    val y = latToY(ring[i + 1], h)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    i += 2
                }
                path.close()
            }
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
}
