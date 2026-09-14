package site.elahady.alkaukaba.ui.quran

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.utils.QuranDisplayPrefs
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog

/** Bottom sheet pengaturan ukuran huruf & spasi kartu ayat - sengaja cuma dipanggil dari
 * toolbar layar Detail Surah/Detail Juz (lihat [DetailSurahActivity]/[DetailJuzActivity]),
 * BUKAN dari layar Konfigurasi global, karena preferensinya ([QuranDisplayPrefs]) cuma
 * relevan buat layar baca ayat. Perubahan langsung diterapkan tiap tap pill (tidak ada tombol
 * "Simpan" terpisah) - [onChanged] dipanggil supaya Activity pemanggil bisa refresh adapter-nya. */
object QuranDisplaySettingsSheet {

    fun show(activity: AppCompatActivity, onChanged: () -> Unit) {
        val dialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(R.layout.dialog_quran_display, null)
        dialog.setContentView(view)

        val textSizePills = mapOf(
            QuranDisplayPrefs.TextSizeLevel.KECIL to view.findViewById<TextView>(R.id.tvTextSizeKecil),
            QuranDisplayPrefs.TextSizeLevel.SEDANG to view.findViewById<TextView>(R.id.tvTextSizeSedang),
            QuranDisplayPrefs.TextSizeLevel.BESAR to view.findViewById<TextView>(R.id.tvTextSizeBesar),
        )
        val spacingPills = mapOf(
            QuranDisplayPrefs.SpacingLevel.RAPAT to view.findViewById<TextView>(R.id.tvSpacingRapat),
            QuranDisplayPrefs.SpacingLevel.SEDANG to view.findViewById<TextView>(R.id.tvSpacingSedang),
            QuranDisplayPrefs.SpacingLevel.LAPANG to view.findViewById<TextView>(R.id.tvSpacingLapang),
        )

        fun refreshTextSizePills() {
            val active = QuranDisplayPrefs.getTextSizeLevel(activity)
            textSizePills.forEach { (level, pill) -> setPillActive(activity, pill, level == active) }
        }

        fun refreshSpacingPills() {
            val active = QuranDisplayPrefs.getSpacingLevel(activity)
            spacingPills.forEach { (level, pill) -> setPillActive(activity, pill, level == active) }
        }

        textSizePills.forEach { (level, pill) ->
            pill.setOnClickListener {
                QuranDisplayPrefs.setTextSizeLevel(activity, level)
                refreshTextSizePills()
                onChanged()
            }
        }
        spacingPills.forEach { (level, pill) ->
            pill.setOnClickListener {
                QuranDisplayPrefs.setSpacingLevel(activity, level)
                refreshSpacingPills()
                onChanged()
            }
        }

        refreshTextSizePills()
        refreshSpacingPills()

        view.findViewById<View>(R.id.btnTutupQuranDisplay).setOnClickListener { dialog.dismiss() }
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
