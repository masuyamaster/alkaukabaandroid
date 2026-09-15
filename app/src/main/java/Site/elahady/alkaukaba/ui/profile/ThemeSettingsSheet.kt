package site.elahady.alkaukaba.ui.profile

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.utils.ThemePrefs
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog

/** Bottom sheet pilihan Tema Aplikasi (Terang/Gelap/Ikuti Sistem), dipanggil dari rowTheme di
 * [ProfileActivity]. Sama seperti [site.elahady.alkaukaba.ui.quran.QuranDisplaySettingsSheet],
 * perubahan langsung diterapkan begitu pill ditekan - [ThemePrefs.setMode] memanggil
 * AppCompatDelegate.setDefaultNightMode() yang otomatis me-recreate activity yang terbuka,
 * termasuk ProfileActivity sendiri di belakang sheet ini. */
object ThemeSettingsSheet {

    fun show(activity: AppCompatActivity) {
        val dialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(R.layout.dialog_theme_settings, null)
        dialog.setContentView(view)

        val pills = mapOf(
            ThemePrefs.ThemeMode.LIGHT to view.findViewById<TextView>(R.id.tvThemeLight),
            ThemePrefs.ThemeMode.DARK to view.findViewById<TextView>(R.id.tvThemeDark),
            ThemePrefs.ThemeMode.SYSTEM to view.findViewById<TextView>(R.id.tvThemeSystem),
        )

        fun refreshPills() {
            val active = ThemePrefs.getMode(activity)
            pills.forEach { (mode, pill) -> setPillActive(activity, pill, mode == active) }
        }

        pills.forEach { (mode, pill) ->
            pill.setOnClickListener {
                ThemePrefs.setMode(activity, mode)
                refreshPills()
            }
        }

        refreshPills()

        view.findViewById<View>(R.id.btnTutupThemeSettings).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setPillActive(context: Context, pill: TextView, active: Boolean) {
        if (active) {
            pill.setBackgroundResource(R.drawable.bg_toggle_pill_active)
            pill.setTextColor(ContextCompat.getColor(context, R.color.text_selected))
        } else {
            pill.background = null
            pill.setTextColor(ContextCompat.getColor(context, R.color.text_unselected))
        }
    }
}
