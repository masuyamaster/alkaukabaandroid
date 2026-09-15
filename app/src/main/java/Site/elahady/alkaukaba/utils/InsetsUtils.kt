package site.elahady.alkaukaba.utils

import android.app.Activity
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

// Aplikasi ini pakai WindowCompat.setDecorFitsSystemWindows(window, false) di semua activity
// (edge-to-edge), tapi tidak ada satupun yang men-consume system bar insets - jadi konten
// paling bawah/atas ketutupan status bar / gesture nav bar. Dua helper ini menambahkan inset
// system bar di atas padding/margin dasar yang sudah ada di XML, bukan menggantikannya.

// systemBars() saja kadang tidak cukup di device dengan punch-hole camera - gabung dengan
// displayCutout() supaya inset atas selalu menutupi cutout, bukan cuma status bar standar.
private val topInsetTypes = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()

fun View.applySystemBarInsetsPadding(applyTop: Boolean = false, applyBottom: Boolean = false) {
    val basePaddingTop = paddingTop
    val basePaddingBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(topInsetTypes)
        view.setPadding(
            view.paddingLeft,
            if (applyTop) basePaddingTop + bars.top else view.paddingTop,
            view.paddingRight,
            if (applyBottom) basePaddingBottom + bars.bottom else view.paddingBottom
        )
        insets
    }
    // setOnApplyWindowInsetsListener cuma bereaksi ke dispatch BERIKUTNYA - kalau dispatch
    // pertama sudah lewat sebelum listener ini terpasang (mis. activity yang sempat memicu
    // dialog izin lokasi di onCreate), listener tidak akan pernah terpanggil tanpa ini.
    ViewCompat.requestApplyInsets(this)
}

fun View.applyTopSystemBarInsetAsMargin() {
    val baseTopMargin = (layoutParams as ViewGroup.MarginLayoutParams).topMargin
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(topInsetTypes)
        val lp = view.layoutParams as ViewGroup.MarginLayoutParams
        lp.topMargin = baseTopMargin + bars.top
        view.layoutParams = lp
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

// Status bar transparan (lihat komentar di atas) butuh warna ikon yang kontras secara eksplisit -
// tanpa ini ikon status bar jatuh ke default platform yang tidak ikut bereaksi ke Tema Gelap.
// Dipakai di activity yang latar atasnya ikut berubah terang/gelap sesuai tema (mayoritas
// activity di app ini). Activity yang latar atasnya SELALU gelap terlepas dari tema (mis. Login,
// Splashscreen) sengaja tidak pakai helper ini - mereka set isAppearanceLightStatusBars = false
// secara manual di kode masing-masing.
fun Window.applyStatusBarIconsForTheme() {
    val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    WindowCompat.getInsetsController(this, decorView).isAppearanceLightStatusBars = !isNightMode
}

fun Activity.applyStatusBarIconsForTheme() = window.applyStatusBarIconsForTheme()
