package site.elahady.alkaukaba.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// Aplikasi ini pakai WindowCompat.setDecorFitsSystemWindows(window, false) di semua activity
// (edge-to-edge), tapi tidak ada satupun yang men-consume system bar insets - jadi konten
// paling bawah/atas ketutupan status bar / gesture nav bar. Dua helper ini menambahkan inset
// system bar di atas padding/margin dasar yang sudah ada di XML, bukan menggantikannya.

fun View.applySystemBarInsetsPadding(applyTop: Boolean = false, applyBottom: Boolean = false) {
    val basePaddingTop = paddingTop
    val basePaddingBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(
            view.paddingLeft,
            if (applyTop) basePaddingTop + bars.top else view.paddingTop,
            view.paddingRight,
            if (applyBottom) basePaddingBottom + bars.bottom else view.paddingBottom
        )
        insets
    }
}

fun View.applyTopSystemBarInsetAsMargin() {
    val baseTopMargin = (layoutParams as ViewGroup.MarginLayoutParams).topMargin
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val lp = view.layoutParams as ViewGroup.MarginLayoutParams
        lp.topMargin = baseTopMargin + bars.top
        view.layoutParams = lp
        insets
    }
}
