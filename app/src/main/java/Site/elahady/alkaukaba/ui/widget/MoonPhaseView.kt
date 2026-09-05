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
 * Ilustrasi 2D fase bulan saat ini: piringan gelap solid digambar dulu
 * ([nightBasePaint], selalu, walau bulan baru sekalipun) — Bulan tetap benda
 * padat yang menghalangi apa pun di belakangnya, termasuk bintang di
 * [StarfieldView] pada kartu Fase Bulan. Di atasnya digambar tekstur penuh
 * (seolah sepenuhnya tersinari), lalu sisi malam tekstur itu "dihapus"
 * ([nightErasePaint], `PorterDuff.Mode.CLEAR`) supaya warna dasar gelap tadi
 * tersingkap lagi, dengan tepi di-[BlurMaskFilter] supaya batas terminator
 * melembut alami alih-alih garis tajam. (Catatan: erase+blur dipakai alih-alih
 * fill+blur langsung karena BlurMaskFilter di atas `Paint.Style.FILL` opaque
 * biasa ternyata merender bentuk jadi poligon patah — keterbatasan/bug Skia
 * untuk shape besar.) Perilaku blur ini bisa dimatikan per-instance lewat
 * [setSoftNightSide] untuk kembali ke gaya lama (piringan solid + sabit tepi
 * tajam, tanpa blur) — dipakai layar hilal Awal Bulan yang butuh kejelasan
 * bentuk untuk keperluan rukyah, bukan realisme.
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
    private var useSoftNightSide = true

    private val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flatBrightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // Warna dasar sisi malam - sama seperti shadowPaint versi lama, digambar
    // PENUH (piringan solid, tanpa blur) SEBELUM tekstur, supaya selalu ada
    // sesuatu yang opak di balik tekstur - kalau tekstur sisi malamnya nanti
    // "dihapus" (lihat nightErasePaint), yang kelihatan warna ini, bukan
    // background di belakang View (Bulan tetap benda padat, tidak boleh
    // tembus ke bintang di StarfieldView kartu Fase Bulan).
    private val nightBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#10192A")
        style = Paint.Style.FILL
    }

    // Paint "penghapus" sisi malam dari LAPISAN TEKSTUR (bukan dari canvas
    // asli) - CLEAR di sini menyingkap nightBasePaint yang sudah digambar
    // duluan, bukan transparansi View. Sengaja tetap pakai Xfermode.CLEAR
    // (bukan fill warna solid biasa) karena BlurMaskFilter dikombinasikan
    // dengan fill opaque normal ternyata merender bentuk jadi poligon patah
    // (bug/keterbatasan Skia untuk shape besar) - CLEAR+blur terbukti mulus.
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
     * Aktifkan/nonaktifkan sisi malam yang lembut (transisi terminator blur,
     * lihat KDoc kelas ini). Kalau dinonaktifkan, tampilan kembali ke gaya
     * lama: piringan solid + sabit dengan tepi tajam, tanpa blur - dipakai
     * layar hilal Awal Bulan (lihat AwalBulanActivity) karena orang yang
     * rukyah butuh bentuk yang jelas & tidak ambigu, bukan realisme. Kartu
     * Fase Bulan & home tetap pakai versi lembut (default true).
     */
    fun setSoftNightSide(enabled: Boolean) {
        useSoftNightSide = enabled
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
     *
     * @param nightColorOverride Warna sisi malam khusus untuk render ini saja
     *   (dikembalikan ke warna asli [nightBasePaint] setelah selesai) - dipakai
     *   modal zoom yang background-nya hitam solid, bukan gradient navy
     *   seperti kartu Fase Bulan, supaya sisi malam tetap menyatu dengan
     *   background-nya (opak, tapi warnanya sama - lihat KDoc kelas ini).
     */
    fun renderToBitmap(sizePx: Int, nightColorOverride: Int? = null): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val originalColor = nightBasePaint.color
        if (nightColorOverride != null) nightBasePaint.color = nightColorOverride
        canvas.save()
        canvas.rotate(rotation, sizePx / 2f, sizePx / 2f)
        drawMoonDisc(canvas, sizePx.toFloat(), sizePx.toFloat())
        canvas.restore()
        if (nightColorOverride != null) nightBasePaint.color = originalColor
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

        // Dasar piringan: selalu digambar solid dulu (Bulan tetap benda padat,
        // harus menghalangi apa pun di belakangnya - termasuk bintang di
        // StarfieldView pada kartu Fase Bulan). Bulan baru (k=0) berhenti di
        // sini saja: piringan gelap polos, tanpa bagian terang sama sekali.
        canvas.drawCircle(cx, cy, r, nightBasePaint)
        if (k <= 0.0) return

        val litPaint = if (useRealisticTexture) {
            updateTextureShader(cx, cy, r)
            texturePaint
        } else {
            flatBrightPaint
        }

        if (k >= 0.999) {
            // Purnama: tidak ada sisi malam sama sekali, tidak perlu layer terpisah.
            canvas.drawCircle(cx, cy, r, litPaint)
            return
        }

        // Tekstur + "penghapusan" sisi malam digambar di layer TERPISAH
        // (saveLayer), BUKAN langsung ke canvas utama yang sudah punya
        // nightBasePaint - PorterDuff.Mode.CLEAR menghapus tuntas ke
        // transparan apa pun yang ada di buffer saat itu, tanpa peduli
        // "lapisan" gambar sebelumnya (nightBasePaint ikut kehapus juga kalau
        // digambar ke canvas yang sama). Dengan layer sendiri, yang kehapus
        // cuma isi layer ini (teksturnya) - begitu layer di-restore, sisa
        // teksturnya (lit path) dikomposit normal (SRC_OVER) di atas
        // nightBasePaint yang sudah aman duluan di canvas utama.
        val pad = r * 0.5f
        val layerBounds = RectF(cx - r - pad, cy - r - pad, cx + r + pad, cy + r + pad)
        val layer = canvas.saveLayer(layerBounds, null)

        // Gambar seluruh piringan seolah sepenuhnya tersinari (drawCircle
        // sendiri sudah otomatis berhenti tajam di radius r, tanpa perlu clip).
        canvas.drawCircle(cx, cy, r, litPaint)

        // ...lalu "hapus" sisi malam dari LAPISAN TEKSTUR INI SAJA - tepinya
        // di-blur (jika [useSoftNightSide]) supaya batas terminator melembut
        // alami. Sengaja TIDAK di-clip ke lingkaran r - clip malah bikin
        // cincin tipis tidak-terhapus-tuntas di limb (interaksi clip vs
        // anti-alias blur yang sulit diprediksi persis). Tanpa clip aman:
        // area di luar r memang belum pernah digambar apa pun di layer ini,
        // jadi menghapusnya di situ tidak berefek apa pun.
        val feather = if (useSoftNightSide) r * TERMINATOR_FEATHER_RATIO else 0f
        nightErasePaint.maskFilter = if (useSoftNightSide) {
            BlurMaskFilter(feather, BlurMaskFilter.Blur.NORMAL)
        } else {
            null
        }
        // Batas luar shape ini (arc yang berimpit dengan limb) tetap didorong
        // keluar melewati lebar blur, supaya mask sempat jenuh (alpha penuh)
        // SEBELUM mencapai limb sungguhan (radius r) - kalau batasnya persis di
        // r, transisi blur baru separuh jalan tepat di limb, sisa piksel
        // texture tidak terhapus tuntas di situ. Lebar terminator (rx) tetap
        // dihitung dari r asli supaya posisi/proporsi sabitnya akurat - yang
        // didorong keluar cuma sisi luarnya, bukan seluruh shape. Kalau
        // !useSoftNightSide, expand=0 (feather=0) sehingga shape ini persis
        // sama dengan buildLitPath(1-k, !brightOnRight) - tepi tajam biasa.
        canvas.drawPath(
            buildNightPath(cx, cy, r, k, brightOnRight, expand = feather * 2.2f),
            nightErasePaint
        )

        canvas.restoreToCount(layer)
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
     * dibalik) untuk dipakai [nightErasePaint] - beda dari `buildLitPath(cx, cy,
     * r, 1.0-k, !brightOnRight)` biasa, di sini radius outer arc DAN rentang
     * vertikal sengaja pakai `r + expand` (bukan `r`) supaya batas shape yang
     * berimpit dengan limb didorong keluar, sementara lebar terminator (rx)
     * tetap dihitung dari `r` asli supaya posisinya akurat. Saat `expand=0`
     * (mis. [useSoftNightSide] mati), hasilnya identik dengan `buildLitPath`
     * versi komplemen - tepi tajam biasa. Lihat pemanggilnya di
     * [drawMoonDisc] untuk kenapa dorongan `expand` ini perlu saat blur aktif.
     */
    private fun buildNightPath(cx: Float, cy: Float, r: Float, k: Double, brightOnRight: Boolean, expand: Float): Path {
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
