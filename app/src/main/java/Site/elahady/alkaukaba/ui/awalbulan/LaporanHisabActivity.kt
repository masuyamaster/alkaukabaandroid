package site.elahady.alkaukaba.ui.awalbulan

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.databinding.ActivityLaporanHisabBinding
import site.elahady.alkaukaba.databinding.ItemLaporanTableRowBinding
import site.elahady.alkaukaba.model.HilalResult
import site.elahady.alkaukaba.utils.HilalPdfService
import site.elahady.alkaukaba.utils.SessionManager
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.utils.prayerbreakdown.PrayerBreakdownSection
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import java.util.Locale

/**
 * Halaman "view" laporan hisab -- pengganti tombol "Download PDF" yang lama.
 * Sekarang tombol PDF di AwalBulanActivity cuma membuka halaman ini; PDF baru
 * dibuat kalau user menekan tombol "Unduh PDF" di sini (lihat [HilalPdfService]).
 */
class LaporanHisabActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLaporanHisabBinding
    private lateinit var result: HilalResult

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLaporanHisabBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        val extraResult = intent.getSerializableExtra(EXTRA_RESULT) as? HilalResult
        if (extraResult == null) {
            finish()
            return
        }
        result = extraResult

        binding.includeToolbar.tvToolbarTitle.text = "Laporan Hasil Hisab"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        renderReport(result)

        binding.btnDownloadPdf.setOnClickListener { downloadPdf() }
    }

    // Di Android 9 (API 28) ke bawah, tulis file ke folder Download publik masih butuh
    // izin WRITE_EXTERNAL_STORAGE eksplisit; API 29+ pakai MediaStore jadi tidak perlu.
    private fun downloadPdf() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_REQUEST_CODE)
            return
        }
        savePdf()
    }

    private fun savePdf() {
        val fileName = "Laporan_Hisab_${System.currentTimeMillis()}.pdf"
        val savedUri = HilalPdfService.exportViewAsPdf(this, binding.reportContent, fileName)
        if (savedUri != null) {
            Toast.makeText(this, "PDF disimpan di folder Download: $fileName", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Gagal menyimpan PDF", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                savePdf()
            } else {
                Toast.makeText(this, "Izin penyimpanan ditolak, PDF tidak bisa disimpan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderReport(result: HilalResult) {
        val sessionManager = SessionManager(this)
        binding.tvHasibName.text = sessionManager.getUserName()?.takeIf { it.isNotBlank() } ?: "Hasib Al-Kaukaba"

        val metode = result.breakdownSections
            .firstOrNull { it.prayerLabel == "Markaz" }
            ?.rows
            ?.firstOrNull { it.label == "Metode" }
            ?.value
            ?: "-"
        binding.tvReportTitle.text = "PERHITUNGAN AWAL BULAN QOMARIYAH\nMETODE ${metode.uppercase(Locale("id", "ID"))}"
        binding.tvReportSubtitle.text = "${result.bulanHijriyahLabel}\nGhurub: ${result.tanggalGhurubLabel}"

        binding.layoutReportRows.removeAllViews()
        var nomor = 1
        buildTableRows(result.breakdownSections).forEach { row ->
            val rowBinding = ItemLaporanTableRowBinding.inflate(layoutInflater, binding.layoutReportRows, false)
            rowBinding.tvNo.text = (nomor++).toString()
            rowBinding.tvLabel.text = row.label
            rowBinding.tvValue.text = row.value
            if (row.isSubRow) {
                val mutedColor = ContextCompat.getColor(this, R.color.waktu_sholat_pill_text)
                rowBinding.tvLabel.setTextColor(mutedColor)
                rowBinding.tvValue.setTextColor(mutedColor)
                rowBinding.tvLabel.textSize = 11f
                rowBinding.tvValue.textSize = 11f
            }
            binding.layoutReportRows.addView(rowBinding.root)
        }

        catatanPenutup(result).forEach { catatan ->
            val rowBinding = ItemLaporanTableRowBinding.inflate(layoutInflater, binding.layoutReportRows, false)
            rowBinding.tvNo.text = (nomor++).toString()
            rowBinding.tvLabel.text = "Catatan"
            rowBinding.tvValue.text = catatan
            binding.layoutReportRows.addView(rowBinding.root)
        }
    }

    private data class TableRow(val label: String, val value: String, val isSubRow: Boolean = false)

    /**
     * Ratakan [PrayerBreakdownSection] jadi baris-baris tabel bernomor, mirror
     * format laporan resmi: seksi tanpa resultTime (mis. Markaz) digabung jadi
     * satu baris multi-baris; seksi dengan resultTime + rows (mis. Data Matahari)
     * tampil sebagai baris ringkasan lalu baris rincian di bawahnya.
     */
    private fun buildTableRows(sections: List<PrayerBreakdownSection>): List<TableRow> {
        val rows = mutableListOf<TableRow>()
        sections.forEach { section ->
            when {
                section.rows.isEmpty() -> {
                    rows += TableRow(section.prayerLabel, section.resultTime)
                }
                section.resultTime.isBlank() -> {
                    val combined = section.rows.joinToString("\n") { "${it.label}: ${it.value}" }
                    rows += TableRow(section.prayerLabel, combined)
                }
                else -> {
                    rows += TableRow(section.prayerLabel, section.resultTime)
                    section.rows.forEach { row ->
                        rows += TableRow(row.label, row.value, isSubRow = true)
                    }
                }
            }
        }
        return rows
    }

    private fun catatanPenutup(result: HilalResult): List<String> {
        val catatan = mutableListOf<String>()
        if (!result.hilalMemenuhiKriteria) {
            catatan += "Karena hilal belum memenuhi kriteria (tinggi ≥ 3°, elongasi ≥ 6,4°), hilal kemungkinan tidak dapat dilihat di markaz ini."
        }
        catatan += "Penetapan awal bulan menunggu hasil Sidang Isbat Kementerian Agama RI."
        return catatan
    }

    companion object {
        const val EXTRA_RESULT = "extra_hilal_result"
        private const val STORAGE_PERMISSION_REQUEST_CODE = 200
    }
}
