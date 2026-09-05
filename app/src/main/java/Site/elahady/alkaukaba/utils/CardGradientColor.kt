package site.elahady.alkaukaba.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import site.elahady.alkaukaba.R

/**
 * Warna latar sungguhan (`bg_card_gradient_navy` atau background lain apa
 * pun yang dipasang [cardView]) tepat di posisi [targetView] relatif ke
 * [cardView] - dipakai `MoonPhaseView.setNightBaseColor` supaya sisi malam
 * Bulan menyatu dengan card di posisi sungguhannya, bukan warna flat tunggal
 * yang cuma pas di satu titik gradient (mis. `#10192A` = `login_bg_deep`,
 * cuma akurat kalau Bulan kebetulan ada di pojok yang paling gelap).
 *
 * Pendekatan: render `cardView.background` (drawable apa pun - `Android
 * `GradientDrawable` tidak expose rumus interpolasi gradient-nya secara
 * publik, jadi menebak rumus manual selalu sedikit meleset) ke [Bitmap]
 * seukuran card, lalu baca piksel sungguhan tepat di posisi tengah
 * [targetView] - hasilnya identik dengan yang sungguhan dirender sistem,
 * bukan aproksimasi.
 */
object CardGradientColor {
    fun approximateAt(cardView: View, targetView: View): Int {
        val fallback = ContextCompat.getColor(cardView.context, R.color.login_bg_deep)
        val background = cardView.background ?: return fallback
        if (cardView.width <= 0 || cardView.height <= 0) return fallback

        val cardLoc = IntArray(2)
        val targetLoc = IntArray(2)
        cardView.getLocationOnScreen(cardLoc)
        targetView.getLocationOnScreen(targetLoc)

        val x = (targetLoc[0] - cardLoc[0] + targetView.width / 2)
            .coerceIn(0, cardView.width - 1)
        val y = (targetLoc[1] - cardLoc[1] + targetView.height / 2)
            .coerceIn(0, cardView.height - 1)

        val bitmap = Bitmap.createBitmap(cardView.width, cardView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val prevBounds = background.copyBounds()
        background.setBounds(0, 0, cardView.width, cardView.height)
        background.draw(canvas)
        background.bounds = prevBounds

        val pixel = bitmap.getPixel(x, y)
        bitmap.recycle()
        return if (Color.alpha(pixel) == 0) fallback else pixel
    }
}
