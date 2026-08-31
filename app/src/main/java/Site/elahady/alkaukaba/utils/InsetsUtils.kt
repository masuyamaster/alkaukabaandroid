package site.elahady.alkaukaba.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
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
