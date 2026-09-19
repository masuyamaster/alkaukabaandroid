package site.elahady.alkaukaba.ui.widget

import site.elahady.alkaukaba.model.VisibilityGridPoint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
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
        // Asset: garis pantai dunia dari Natural Earth 110m ("ne_110m_land", domain publik),
        // diminifikasi jadi [[[lng,lat,lng,lat,...], ring2, ...], polygon2, ...] -- array polygon,
        // tiap polygon array ring (ring pertama = outer, sisanya = lubang mis. Laut Kaspia),
        // tiap ring array flat lng/lat berselang-seling, dibulatkan 2 desimal (~1km, lebih dari
        // cukup untuk peta seukuran layar HP). Menggantikan poligon tangan-gambar versi v1 yang
        // bentuknya kasar/tidak akurat (lihat known limitation lama di
        // docs/features/peta-visibilitas.md).
        private const val LAND_ASSET = "world_land_110m.json"

        private const val STATE_UNKNOWN = 0
        private const val STATE_MEMENUHI = 1
        private const val STATE_BELUM = 2
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
    private val memenuhiColor = Color.parseColor("#66059669") // hijau emerald, semi-transparan
    private val belumColor = Color.parseColor("#59B91C1C") // merah, semi-transparan (35% alpha)

    // Bitmap kecil (1 piksel per titik grid + 1 kolom wrap) yang di-scale-up dengan bilinear
    // filter (lihat gridBitmapPaint) -- trik standar untuk dapat gradasi warna halus antar titik
    // grid tanpa menambah jumlah titik hisab (opsi B.3 di rencana peta-visibilitas.md 6a).
    private var gridBitmap: Bitmap? = null
    private var gridLatStep = 0.0
    private var gridLngStep = 0.0
    private val gridBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }

    fun setData(newPoints: List<VisibilityGridPoint>) {
        gridBitmap = buildGridBitmap(newPoints)
        invalidate()
    }

    /**
     * Susun titik grid (renggang, tersebar per [VisibilityGridPoint]) jadi bitmap kecil
     * (1 piksel = 1 titik) yang membentang dari lintang 90° s/d -90° dan bujur -180° s/d 180°,
     * lalu digambar ter-scale di [onDraw] dengan bilinear filter -- Android otomatis
     * menginterpolasi warna antar piksel bitmap saat di-scale, sehingga transisi memenuhi/belum
     * kriteria terlihat gradasi halus, bukan kotak-kotak tegas, tanpa perlu menghitung titik
     * grid lebih rapat.
     *
     * Sel yang tidak punya hasil (di luar rentang lintang yang dihitung, atau gagal dihitung
     * dan di-skip WorldVisibilityCalculator, mis. dekat kutub) diisi dari sel terdekat
     * ([fillUnknownFromNearest]) supaya zona memenuhi/belum menutup seluruh peta tanpa celah.
     */
    private fun buildGridBitmap(points: List<VisibilityGridPoint>): Bitmap? {
        val lats = points.map { it.latitude }.distinct().sorted()
        val lngs = points.map { it.longitude }.distinct().sorted()
        if (lats.size < 2 || lngs.size < 2) return null

        val latStep = lats[1] - lats[0]
        val lngStep = lngs[1] - lngs[0]
        val numLat = Math.round(180.0 / latStep).toInt() + 1
        val numLng = Math.round(360.0 / lngStep).toInt()

        val state = Array(numLat) { IntArray(numLng) }
        for (p in points) {
            val row = Math.round((90.0 - p.latitude) / latStep).toInt()
            val col = Math.round((p.longitude + 180.0) / lngStep).toInt()
            if (row in 0 until numLat && col in 0 until numLng) {
                state[row][col] = if (p.memenuhiKriteria) STATE_MEMENUHI else STATE_BELUM
            }
        }
        fillUnknownFromNearest(state)

        // +1 kolom di kanan = duplikat kolom pertama, merepresentasikan bujur 180° (~ -180°) --
        // grid dunia selalu lingkaran penuh, jadi ini menyambungkan interpolasi di seam
        // meridian ±180 alih-alih terpotong tegas di tepi peta.
        val bitmap = Bitmap.createBitmap(numLng + 1, numLat, Bitmap.Config.ARGB_8888)
        for (row in 0 until numLat) {
            for (col in 0..numLng) {
                val color = if (state[row][col % numLng] == STATE_MEMENUHI) memenuhiColor else belumColor
                bitmap.setPixel(col, row, color)
            }
        }

        gridLatStep = latStep
        gridLngStep = lngStep
        return bitmap
    }

    // BFS multi-sumber dari semua sel berhasil: tiap sel kosong dapat nilai sel terisi terdekat.
    // Kolom pertama & terakhir bertetangga (bujur melingkar); baris tidak melingkar (kutub).
    private fun fillUnknownFromNearest(state: Array<IntArray>) {
        val numLat = state.size
        val numLng = state[0].size
        val queue = ArrayDeque<Int>()
        for (r in 0 until numLat) {
            for (c in 0 until numLng) {
                if (state[r][c] != STATE_UNKNOWN) queue.add(r * numLng + c)
            }
        }
        val neighbors = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1))
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val r = cur / numLng
            val c = cur % numLng
            for (d in neighbors) {
                val nr = r + d[0]
                if (nr < 0 || nr >= numLat) continue
                val nc = (c + d[1] + numLng) % numLng
                if (state[nr][nc] == STATE_UNKNOWN) {
                    state[nr][nc] = state[r][c]
                    queue.add(nr * numLng + nc)
                }
            }
        }
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

        gridBitmap?.let { bitmap ->
            val src = Rect(0, 0, bitmap.width, bitmap.height)
            // Titik grid = pusat piksel, jadi bitmap melebar setengah sel melewati tepi peta
            // (±180°/±90°) dan dipotong oleh batas View -- seluruh peta tertutup zona.
            val dst = RectF(
                lngToX(-180.0 - gridLngStep / 2, w),
                latToY(90.0 + gridLatStep / 2, h),
                lngToX(180.0 + gridLngStep / 2, w),
                latToY(-90.0 - gridLatStep / 2, h)
            )
            canvas.drawBitmap(bitmap, src, dst, gridBitmapPaint)
        }
    }
}
