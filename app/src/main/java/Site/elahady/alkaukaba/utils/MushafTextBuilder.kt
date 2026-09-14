package site.elahady.alkaukaba.utils

import site.elahady.alkaukaba.model.Ayat
import android.text.Spannable
import android.text.SpannableStringBuilder

/** Susun teks Arab satu surah jadi satu paragraf mengalir seperti mushaf fisik - beda dari
 * mode "Terjemahan" yang pecah per-ayat jadi kartu terpisah dengan transliterasi & terjemahan.
 * Batas antar-ayat ditandai [AyahMarkerSpan] (lingkaran kecil gold + nomor Arab-Indic), bukan
 * karakter Unicode - lihat catatan di [AyahMarkerSpan] kenapa. */
object MushafTextBuilder {

    private val arabicIndicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    private fun toArabicIndicNumber(n: Int): String =
        n.toString().map { arabicIndicDigits[it - '0'] }.joinToString("")

    fun build(ayatList: List<Ayat>, circleColor: Int, numberColor: Int): CharSequence {
        val builder = SpannableStringBuilder()
        ayatList.forEach { ayat ->
            builder.append(ayat.teksArab.trim())

            // Satu karakter placeholder (ditimpa gambar AyahMarkerSpan) langsung menempel di
            // antara dua huruf Arab kuat (akhir ayat ini & awal ayat berikutnya) - sengaja
            // TIDAK diberi spasi tambahan di kanan-kirinya, karena beberapa spasi netral
            // berurutan di dekat karakter pengganti terbukti bikin algoritma bidi salah
            // menempatkan urutan visualnya (marker "meloncat" ke tengah kata ayat berikutnya).
            val markerStart = builder.length
            builder.append(" ")
            builder.setSpan(
                AyahMarkerSpan(toArabicIndicNumber(ayat.nomorAyat), circleColor, numberColor),
                markerStart,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return builder
    }
}
