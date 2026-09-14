package site.elahady.alkaukaba.utils

import site.elahady.alkaukaba.databinding.ItemAyatBinding
import android.content.Context
import android.view.ViewGroup

/** Terapkan [QuranDisplayPrefs] (ukuran huruf & spasi) ke satu kartu `item_ayat.xml` - dipakai
 * bareng oleh [site.elahady.alkaukaba.adapter.AyatAdapter] (layar Detail Surah) dan
 * [site.elahady.alkaukaba.adapter.JuzAyatAdapter] (layar Detail Juz) supaya logikanya tidak
 * dobel di dua tempat. Dipanggil tiap `bind()` (bukan sekali di `onCreateViewHolder`) karena
 * user bisa ganti preferensi kapan saja lewat `QuranDisplaySettingsSheet`, lalu Activity
 * pemanggil trigger `notifyDataSetChanged()` supaya kartu yang sudah ke-render ikut update. */
fun applyDisplayPrefs(binding: ItemAyatBinding, context: Context) {
    val textSize = QuranDisplayPrefs.getTextSizeLevel(context)
    val spacing = QuranDisplayPrefs.getSpacingLevel(context)
    val density = context.resources.displayMetrics.density

    binding.tvTeksArab.textSize = textSize.arabicSp
    binding.tvTeksArab.setLineSpacing(0f, spacing.arabicLineSpacing)
    binding.tvTeksLatin.textSize = textSize.latinSp
    binding.tvTeksIndonesia.textSize = textSize.translationSp

    val paddingPx = (spacing.cardPaddingDp * density).toInt()
    binding.layoutAyatContent.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

    val marginPx = (spacing.cardMarginDp * density).toInt()
    val layoutParams = binding.cardAyat.layoutParams as ViewGroup.MarginLayoutParams
    layoutParams.bottomMargin = marginPx
    binding.cardAyat.layoutParams = layoutParams
}
