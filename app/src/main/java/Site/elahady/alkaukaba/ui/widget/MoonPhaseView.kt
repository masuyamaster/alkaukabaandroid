package site.elahady.alkaukaba.ui.widget

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import site.elahady.alkaukaba.R
import kotlin.math.abs
import kotlin.math.min

/**
 * Ilustrasi 2D fase bulan saat ini: piringan gelap dengan area terang yang
 * dibentuk dari setengah lingkaran (sisi limb terang) dipotong/ditambah oleh
 * elips terminator, sesuai fraksi iluminasi dan arah waxing/waning. Area
 * terang dirender pakai tekstur foto bulan asli (bukan warna flat) supaya
 * terlihat realistis, di-clip ke bentuk yang sama.
 */
class MoonPhaseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var illuminatedFraction = 0.5
    private var brightOnRight = true
    private var useRealisticTexture = true

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#10192A")
        style = Paint.Style.FILL
    }
    private val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flatBrightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8BA5C")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        alpha = 140
    }

    // Foto bulan purnama asli (NASA, domain publik - lihat res/drawable-nodpi/moon_texture.jpg),
    // dipotong ke bentuk sabit/cembung yang sama dengan ilustrasi vektor sebelumnya, jadi bagian
    // terang terlihat seperti foto asli alih-alih warna flat. Didekode malas (lazy) supaya
    // pemakai yang men-disable tekstur (lihat setRealisticTexture) tidak ikut decode bitmap ini.
    private val moonBitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.moon_texture) }

    fun setShadowColor(color: Int) {
        shadowPaint.color = color
        invalidate()
    }

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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - outlinePaint.strokeWidth
        if (r <= 0f) return

        canvas.drawCircle(cx, cy, r, shadowPaint)

        // Hilal muda bisa <0.1% tersinari — beri lantai tampilan minimum
        // supaya bentuk sabitnya tetap terlihat (angka persen asli tetap
        // ditampilkan terpisah sebagai teks, ilustrasi ini cuma bantu bentuk).
        val k = if (illuminatedFraction <= 0.0) 0.0 else illuminatedFraction.coerceAtLeast(0.05)
        if (k > 0.0) {
            val litPaint = if (useRealisticTexture) {
                updateTextureShader(cx, cy, r)
                texturePaint
            } else {
                flatBrightPaint
            }
            when {
                k >= 0.999 -> canvas.drawCircle(cx, cy, r, litPaint)
                else -> canvas.drawPath(buildLitPath(cx, cy, r, k), litPaint)
            }
        }

        canvas.drawCircle(cx, cy, r, outlinePaint)
    }

    /**
     * Skala+posisikan tekstur foto bulan (persegi) supaya pas menutupi bounding
     * box lingkaran piringan (cx,cy,r). Matrix dihitung ulang tiap draw karena
     * murah (bukan decode bitmap), sekaligus otomatis menangani perubahan ukuran view.
     */
    private fun updateTextureShader(cx: Float, cy: Float, r: Float) {
        val shader = texturePaint.shader as? BitmapShader
            ?: BitmapShader(moonBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                .also { texturePaint.shader = it }
        val scale = (2f * r) / moonBitmap.width.toFloat()
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(cx - r, cy - r)
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
    private fun buildLitPath(cx: Float, cy: Float, r: Float, k: Double): Path {
        // Setengah bulan persis: elips terminator melebar nol (garis lurus).
        if (abs(k - 0.5) < 0.003) {
            return buildHalfDisc(cx, cy, r)
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

    private fun buildHalfDisc(cx: Float, cy: Float, r: Float): Path {
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)
        return Path().apply {
            if (brightOnRight) addArc(rect, -90f, 180f) else addArc(rect, -90f, -180f)
            lineTo(cx, cy - r)
            close()
        }
    }
}
