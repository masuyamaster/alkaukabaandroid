package site.elahady.alkaukaba.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** Preferensi Tema Gelap (Light/Dark/Ikuti Sistem) - dibaca sekali di [site.elahady.alkaukaba.AlKaukabaApplication]
 * saat startup lewat [AppCompatDelegate.setDefaultNightMode], dan ditulis dari sheet pilihan tema
 * di [site.elahady.alkaukaba.ui.profile.ProfileActivity]. setDefaultNightMode() otomatis
 * me-recreate semua activity yang sedang terbuka begitu dipanggil, jadi tidak perlu recreate()
 * manual di sisi pemanggil. */
object ThemePrefs {
    private const val PREFS_NAME = "ThemePrefs"
    private const val KEY_THEME_MODE = "theme_mode"

    enum class ThemeMode(val label: String, val nightMode: Int) {
        LIGHT("Terang", AppCompatDelegate.MODE_NIGHT_NO),
        DARK("Gelap", AppCompatDelegate.MODE_NIGHT_YES),
        SYSTEM("Ikuti Sistem", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    }

    fun getMode(context: Context): ThemeMode {
        val ordinal = prefs(context).getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.ordinal)
        return ThemeMode.values().getOrElse(ordinal) { ThemeMode.SYSTEM }
    }

    fun setMode(context: Context, mode: ThemeMode) {
        prefs(context).edit().putInt(KEY_THEME_MODE, mode.ordinal).apply()
        AppCompatDelegate.setDefaultNightMode(mode.nightMode)
    }

    fun applySavedMode(context: Context) {
        AppCompatDelegate.setDefaultNightMode(getMode(context).nightMode)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
