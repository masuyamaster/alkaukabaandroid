package site.elahady.alkaukaba.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import site.elahady.alkaukaba.R
import kotlin.math.abs
import kotlin.math.min

/**
 * Ilustrasi 2D fase bulan saat ini: piringan digambar penuh dengan tekstur
 * (seolah sepenuhnya tersinari), lalu sisi malamnya "dihapus" (bukan ditimpa
 * warna solid) memakai [BlurMaskFilter] + [PorterDuff.Mode.CLEAR] supaya sisi
 * itu benar-benar transparan (menyatu dengan background di belakang View,
 * apa pun warnanya) dan batas terminator-nya melembut secara alami alih-alih
 * garis tajam — limb luar (tepi lingkaran) tetap tajam karena semua
 * penghapusan di-clip ketat ke lingkaran itu duluan.
 */
class MoonPhaseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        // Perbesar tekstur ~4% dari pas-pasan supaya margin gelap tipis di tepi
        // moon_texture.jpg (sisa proses crop) terdorong keluar area lingkaran.
        private const val TEXTURE_OVERSCAN = 1.04f

        // Lebar pelembutan terminator, sebagai fraksi radius piringan.
        private const val TERMINATOR_FEATHER_RATIO = 0.10f
    }

    init {
        // BlurMaskFilter (dipakai nightErasePaint) tidak didukung penuh di
        // hardware-accelerated canvas pada banyak versi Android - paksa software
        // layer supaya pelembutan terminator konsisten tampil di semua device.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private var illuminatedFraction = 0.5
    private var brightOnRight = true
    private var useRealisticTexture = true

    private val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flatBrightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // Paint "penghapus" sisi malam: warna tidak relevan (CLEAR cuma pakai alpha
    // mask-nya), maskFilter di-set ulang tiap draw karena radius blur mengikuti
    // ukuran piringan saat itu (beda antara kartu home 56dp & layar detail 160dp).
    private val nightErasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    // Foto bulan purnama asli (Gregory H. Revera, CC BY-SA 3.0 - lihat docs/features/fase-bulan.md
    // untuk atribusi lengkap), sudah di-crop presisi supaya piringannya pas memenuhi bujur sangkar
    // gambar (foto sumber aslinya tidak simetris, sempat menyisakan celah di tepi lingkaran yang
    // digambar). Dipotong ke bentuk sabit/cembung yang sama dengan ilustrasi vektor sebelumnya, jadi
    // bagian terang terlihat seperti foto asli alih-alih warna flat. Didekode malas (lazy) supaya
    // pemakai yang men-disable tekstur (lihat setRealisticTexture) tidak ikut decode bitmap ini.
    private val moonBitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.moon_texture) }

    /**
     * Aktifkan/nonaktifkan tekstur foto bulan asli untuk bagian terang; kalau
     * dinonaktifkan, bagian terang diisi warna putih flat. Layar hilal Awal
     * Bulan menonaktifkan ini (lihat AwalBulanActivity) - kartu Fase Bulan
     * dan home tetap pakai tekstur (default true).
     */
    fun setRealisticTexture(enabled: Boolean) {
        useRealisticTexture = enabled
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

    /**
     * Set fase untuk bulan sabit awal (waxing crescent) saja, dipakai saat
     * sudut fase sinodik tidak tersedia tapi sudah pasti waxing — mis. hilal
     * awal bulan, yang selalu terjadi tak lama setelah ijtima' (bulan baru).
     * Pakai bersama [setBrightLimbAngle] untuk orientasi sungguhan di langit.
     */
    fun setWaxingCrescent(illuminatedFraction: Double) {
        this.illuminatedFraction = illuminatedFraction.coerceIn(0.0, 1.0)
        this.brightOnRight = true
        invalidate()
    }

    /**
     * Putar ilustrasi supaya limb terang menghadap sudut sebenarnya di langit
     * pengamat (derajat, dari atas/zenith, searah jarum jam — lihat
     * [site.elahady.alkaukaba.utils.MoonTilt]). Dipakai untuk visualisasi
     * hilal saat rukyah; kartu fase bulan generik tidak perlu memanggil ini.
     * Hanya valid untuk bulan sabit awal (waxing crescent, limb terang di
     * kanan pada gambar kanonik) seperti hilal awal bulan.
     */
    fun setBrightLimbAngle(angleDegrees: Double) {
        rotation = (angleDegrees - 90.0).toFloat()
    }

    /**
     * Set fase (fraksi tersinari) SEKALIGUS kemiringan sungguhan limb terang
     * seperti tampak di langit pengamat (lihat [site.elahady.alkaukaba.utils.MoonTilt]),
     * dipakai saat lokasi observer tersedia. Beda dari [setPhase] yang cuma
     * pilih sisi kiri/kanan generik tanpa rotasi (dipakai saat lokasi belum
     * ada) — di sini shape kanonik selalu "bright-on-right" (sama seperti
     * [setWaxingCrescent]) karena orientasi sungguhan sepenuhnya ditentukan
     * lewat rotasi [setBrightLimbAngle], bukan lewat flag kiri/kanan lagi.
     */
    fun setPhaseWithTrueTilt(illuminatedFraction: Double, tiltAngleDegrees: Double) {
        this.illuminatedFraction = illuminatedFraction.coerceIn(0.0, 1.0)
        this.brightOnRight = true
        setBrightLimbAngle(tiltAngleDegrees)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawMoonDisc(canvas, width.toFloat(), height.toFloat())
    }

    /**
     * Render ilustrasi fase saat ini ke [Bitmap] persegi berdiri sendiri (bukan
     * ke View ini), dipakai layar detail Fase Bulan untuk modal zoom — ukuran
     * bitmap bebas beda dari ukuran View sebenarnya di layar. Rotasi [rotation]
     * (dari [setBrightLimbAngle]/[setPhaseWithTrueTilt]) di-terapkan manual ke
     * canvas karena di sini kita gambar langsung, bukan lewat [View.draw] yang
     * biasanya otomatis menerapkan rotasi View.
     */
    fun renderToBitmap(sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.save()
        canvas.rotate(rotation, sizePx / 2f, sizePx / 2f)
        drawMoonDisc(canvas, sizePx.toFloat(), sizePx.toFloat())
        canvas.restore()
        return bitmap
    }

    private fun drawMoonDisc(canvas: Canvas, w: Float, h: Float) {
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) / 2f
        if (r <= 0f) return

        // Hilal muda bisa <0.1% tersinari — beri lantai tampilan minimum
        // supaya bentuk sabitnya tetap terlihat (angka persen asli tetap
        // ditampilkan terpisah sebagai teks, ilustrasi ini cuma bantu bentuk).
        val k = if (illuminatedFraction <= 0.0) 0.0 else illuminatedFraction.coerceAtLeast(0.05)
        // Bulan baru (k=0): tidak digambar sama sekali - seluruhnya transparan,
        // menyatu dengan background, bukan piringan gelap solid.
        if (k <= 0.0) return

        val litPaint = if (useRealisticTexture) {
            updateTextureShader(cx, cy, r)
            texturePaint
        } else {
            flatBrightPaint
        }

        // Gambar seluruh piringan seolah sepenuhnya tersinari dulu (drawCircle
        // sendiri sudah otomatis berhenti tajam di radius r, tanpa perlu clip).
        canvas.drawCircle(cx, cy, r, litPaint)

        // ...lalu "hapus" sisi malam (bukan timpa warna solid) supaya benar-benar
        // transparan & batas terminator melembut alami. Sengaja TIDAK di-clip ke
        // lingkaran r - clip malah bikin cincin tipis tidak-terhapus-tuntas di
        // limb (interaksi clip vs anti-alias blur yang sulit diprediksi persis).
        // Tanpa clip aman: area di luar r memang belum pernah digambar apa pun
        // (transparan dari awal), jadi menghapusnya di situ tidak berefek apa pun.
        if (k < 0.999) {
            val feather = r * TERMINATOR_FEATHER_RATIO
            nightErasePaint.maskFilter = BlurMaskFilter(feather, BlurMaskFilter.Blur.NORMAL)
            // Batas luar shape ini (arc yang berimpit dengan limb) tetap didorong
            // keluar melewati lebar blur, supaya mask sempat jenuh (alpha penuh)
            // SEBELUM mencapai limb sungguhan (radius r) - kalau batasnya persis di
            // r, transisi blur baru separuh jalan tepat di limb, sisa piksel
            // texture tidak terhapus tuntas di situ. Lebar terminator (rx) tetap
            // dihitung dari r asli supaya posisi/proporsi sabitnya akurat - yang
            // didorong keluar cuma sisi luarnya, bukan seluruh shape.
            canvas.drawPath(
                buildNightErasePath(cx, cy, r, k, brightOnRight, expand = feather * 2.2f),
                nightErasePaint
            )
        }
    }

    /**
     * Skala+posisikan tekstur foto bulan (persegi) supaya pas menutupi bounding
     * box lingkaran piringan (cx,cy,r). Matrix dihitung ulang tiap draw karena
     * murah (bukan decode bitmap), sekaligus otomatis menangani perubahan ukuran view.
     *
     * Di-overscan sedikit ([TEXTURE_OVERSCAN]) karena file tekstur sengaja
     * menyisakan margin tipis piksel gelap di sekeliling piringan saat di-crop
     * (lihat docs/features/fase-bulan.md §4) - tanpa overscan, margin itu ikut
     * ter-render sebagai cincin gelap tepat di tepi lingkaran begitu outline
     * gold dihapus (sebelumnya tersamar oleh outline tsb).
     */
    private fun updateTextureShader(cx: Float, cy: Float, r: Float) {
        val shader = texturePaint.shader as? BitmapShader
            ?: BitmapShader(moonBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                .also { texturePaint.shader = it }
        val scale = (2f * r) / moonBitmap.width.toFloat() * TEXTURE_OVERSCAN
        val offset = r * (TEXTURE_OVERSCAN - 1f)
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(cx - r - offset, cy - r - offset)
        }
        shader.setLocalMatrix(matrix)
    }

    /**
     * Dibangun langsung dari dua arc (bukan Path.op boolean union/difference)
     * karena Path.op tidak stabil pada bentuk hampir-degenerate — persis kasus
     * hilal tipis (k mendekati 0, elips terminator hampir sama lebar dengan
     * lingkaran luar) yang sempat gagal dirender lewat pendekatan Path.op
     * sebelumnya. Dua arc dari titik atas ke bawah (sisi luar, radius r) lalu
     * kembali ke atas (sisi elips terminator, radius rx) selalu valid berapa
     * pun tipis/gemuknya bulan, tanpa operasi boolean.
     */
    private fun buildLitPath(cx: Float, cy: Float, r: Float, k: Double, brightOnRight: Boolean): Path {
        // Setengah bulan persis: elips terminator melebar nol (garis lurus).
        if (abs(k - 0.5) < 0.003) {
            return buildHalfDisc(cx, cy, r, brightOnRight)
        }

        val outerRect = RectF(cx - r, cy - r, cx + r, cy + r)
        val rx = (r * abs(1.0 - 2.0 * k)).toFloat()
        val innerRect = RectF(cx - rx, cy - r, cx + rx, cy + r)

        // Sabit (k<0.5): sisi elips membulat ke sisi terang yang sama (lensa
        // tipis di pinggir). Cembung (k>0.5): membulat ke sisi gelap
        // (menambah area terang melewati garis tengah).
        val innerBulgesRight = if (k < 0.5) brightOnRight else !brightOnRight

        return Path().apply {
            arcTo(outerRect, -90f, if (brightOnRight) 180f else -180f)
            arcTo(innerRect, 90f, if (innerBulgesRight) -180f else 180f)
            close()
        }
    }

    /**
     * Shape sisi malam (komplemen [buildLitPath]: fraksi 1-k, sisi terang
     * dibalik) untuk dipakai [nightErasePaint] - beda dari `buildLitPath(cx,
     * cy, r, 1.0-k, !brightOnRight)` biasa, di sini radius outer arc DAN
     * rentang vertikal sengaja pakai `r + expand` (bukan `r`) supaya batas
     * shape yang berimpit dengan limb didorong keluar, sementara lebar
     * terminator (rx) tetap dihitung dari `r` asli supaya posisinya akurat.
     * Lihat pemanggilnya di [drawMoonDisc] untuk kenapa dorongan ini perlu.
     */
    private fun buildNightErasePath(cx: Float, cy: Float, r: Float, k: Double, brightOnRight: Boolean, expand: Float): Path {
        val nightK = 1.0 - k
        val nightBrightOnRight = !brightOnRight
        val r2 = r + expand
        val outerRect = RectF(cx - r2, cy - r2, cx + r2, cy + r2)

        if (abs(nightK - 0.5) < 0.003) {
            return Path().apply {
                if (nightBrightOnRight) addArc(outerRect, -90f, 180f) else addArc(outerRect, -90f, -180f)
                lineTo(cx, cy - r2)
                close()
            }
        }

        val rx = (r * abs(1.0 - 2.0 * nightK)).toFloat()
        val innerRect = RectF(cx - rx, cy - r2, cx + rx, cy + r2)
        val innerBulgesRight = if (nightK < 0.5) nightBrightOnRight else !nightBrightOnRight

        return Path().apply {
            arcTo(outerRect, -90f, if (nightBrightOnRight) 180f else -180f)
            arcTo(innerRect, 90f, if (innerBulgesRight) -180f else 180f)
            close()
        }
    }

    private fun buildHalfDisc(cx: Float, cy: Float, r: Float, brightOnRight: Boolean): Path {
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)
        return Path().apply {
            if (brightOnRight) addArc(rect, -90f, 180f) else addArc(rect, -90f, -180f)
            lineTo(cx, cy - r)
            close()
        }
    }
}
