package site.elahady.alkaukaba.utils

import android.content.Context

/** Preferensi tampilan baca ayat (ukuran huruf & spasi antar ayat) - khusus fitur Al-Qur'an
 * (dipakai [site.elahady.alkaukaba.adapter.AyatAdapter] & [site.elahady.alkaukaba.adapter.JuzAyatAdapter]
 * di layar Detail Surah/Juz), disimpan di SharedPreferences terpisah dari [SessionManager].
 * Sengaja TIDAK ditaruh di layar Konfigurasi global - preferensi ini cuma relevan buat layar
 * baca ayat, jadi diatur dari tombol aksi di toolbar layar itu sendiri (lihat
 * `QuranDisplaySettingsSheet`), bukan preferensi aplikasi secara umum.
 *
 * Level default (SEDANG) sengaja sama persis dengan ukuran yang sudah dipakai sebelum fitur
 * ini ada - supaya tidak ada pengguna yang tampilannya "tiba-tiba berubah", cuma nambah opsi
 * buat yang mau menyesuaikan. */
object QuranDisplayPrefs {
    private const val PREFS_NAME = "QuranDisplayPrefs"
    private const val KEY_TEXT_SIZE_LEVEL = "text_size_level"
    private const val KEY_SPACING_LEVEL = "spacing_level"

    enum class TextSizeLevel(
        val label: String,
        val arabicSp: Float,
        val latinSp: Float,
        val translationSp: Float,
        val mushafSp: Float
    ) {
        KECIL("Kecil", 17f, 10f, 11f, 20f),
        SEDANG("Sedang", 20f, 12f, 13f, 24f),
        BESAR("Besar", 24f, 14f, 15f, 28f),
    }

    enum class SpacingLevel(
        val label: String,
        val cardPaddingDp: Float,
        val cardMarginDp: Float,
        val arabicLineSpacing: Float,
        val mushafLineSpacing: Float
    ) {
        RAPAT("Rapat", 8f, 6f, 1.25f, 1.8f),
        SEDANG("Sedang", 12f, 8f, 1.4f, 2.1f),
        LAPANG("Lapang", 18f, 14f, 1.7f, 2.4f),
    }

    fun getTextSizeLevel(context: Context): TextSizeLevel {
        val ordinal = prefs(context).getInt(KEY_TEXT_SIZE_LEVEL, TextSizeLevel.SEDANG.ordinal)
        return TextSizeLevel.values().getOrElse(ordinal) { TextSizeLevel.SEDANG }
    }

    fun setTextSizeLevel(context: Context, level: TextSizeLevel) {
        prefs(context).edit().putInt(KEY_TEXT_SIZE_LEVEL, level.ordinal).apply()
    }

    fun getSpacingLevel(context: Context): SpacingLevel {
        val ordinal = prefs(context).getInt(KEY_SPACING_LEVEL, SpacingLevel.SEDANG.ordinal)
        return SpacingLevel.values().getOrElse(ordinal) { SpacingLevel.SEDANG }
    }

    fun setSpacingLevel(context: Context, level: SpacingLevel) {
        prefs(context).edit().putInt(KEY_SPACING_LEVEL, level.ordinal).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
