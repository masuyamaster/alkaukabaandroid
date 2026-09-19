package site.elahady.alkaukaba.ui.konversitanggal

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.tabs.TabLayout
import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.databinding.ActivityKonversiHijriyahBinding
import site.elahady.alkaukaba.utils.HijriDateUtil
import site.elahady.alkaukaba.utils.HijriDateUtil.DateParts
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import java.util.Calendar

/**
 * Konversi tanggal Masehi <-> Hijriyah dua arah, 2 tab dalam satu Activity (toggle visibility,
 * pola yang sama dengan `ZakatActivity`). Hasil diperbarui langsung tiap input berubah, tanpa
 * tombol "Hitung" — hitungannya murni aritmetika ringan di [HijriDateUtil] (tabular, offline).
 *
 * Rentang dibatasi (Masehi [MIN_MASEHI_YEAR]..[MAX_MASEHI_YEAR], Hijriyah [MIN_HIJRI_YEAR]..
 * [MAX_HIJRI_YEAR]) karena sebelum reformasi Gregorian 1582 tanggal sejarah tercatat di kalender Julian —
 * hasil Gregorian proleptik di rentang itu akan menyesatkan.
 */
class KonversiHijriyahActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKonversiHijriyahBinding
    private var tanggalMasehi = todayGregorian()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKonversiHijriyahBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        applyStatusBarIconsForTheme()
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        binding.includeToolbar.tvToolbarTitle.text = "Konversi Hijriyah - Masehi"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupTabs()
        setupMasehiKeHijriyah()
        setupHijriyahKeMasehi()
    }

    private fun setupTabs() {
        binding.tabLayoutKonversi.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.layoutMasehiKeHijriyah.visibility = if (tab.position == 0) View.VISIBLE else View.GONE
                binding.layoutHijriyahKeMasehi.visibility = if (tab.position == 1) View.VISIBLE else View.GONE
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    // ===== Masehi -> Hijriyah =====

    private fun setupMasehiKeHijriyah() {
        binding.tvTanggalMasehi.setOnClickListener { showDatePicker() }
        binding.btnHariIniMasehi.setOnClickListener {
            tanggalMasehi = todayGregorian()
            renderMasehiKeHijriyah()
        }
        renderMasehiKeHijriyah()
    }

    private fun showDatePicker() {
        val dialog = DatePickerDialog(
            this,
            { _, year, monthZeroBased, day ->
                tanggalMasehi = DateParts(year, monthZeroBased + 1, day)
                renderMasehiKeHijriyah()
            },
            tanggalMasehi.year, tanggalMasehi.month - 1, tanggalMasehi.day
        )
        dialog.datePicker.minDate = millisOf(MIN_MASEHI_YEAR, 1, 1)
        dialog.datePicker.maxDate = millisOf(MAX_MASEHI_YEAR, 12, 31)
        dialog.show()
    }

    private fun renderMasehiKeHijriyah() {
        val g = tanggalMasehi
        binding.tvTanggalMasehi.text = "${g.day} ${HijriDateUtil.gregorianMonthNames[g.month - 1]} ${g.year}"
        val hijri = HijriDateUtil.gregorianToHijri(g.year, g.month, g.day)
        binding.tvHasilHijriyah.text = HijriDateUtil.hijriLabel(hijri)
        binding.tvHasilHijriyahHari.text = "Hari ${HijriDateUtil.weekdayName(g.year, g.month, g.day)}"
    }

    // ===== Hijriyah -> Masehi =====

    private fun setupHijriyahKeMasehi() {
        val monthAdapter = ArrayAdapter(this, R.layout.item_spinner_selector, HijriDateUtil.monthNames)
        monthAdapter.setDropDownViewResource(R.layout.item_spinner_selector_dropdown)
        binding.spinnerBulanKonversi.adapter = monthAdapter

        setHijriInputs(todayHijri())

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = renderHijriyahKeMasehi()
        }
        binding.etTanggalHijriyah.addTextChangedListener(watcher)
        binding.etTahunHijriyah.addTextChangedListener(watcher)
        binding.spinnerBulanKonversi.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) =
                renderHijriyahKeMasehi()

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.btnHariIniHijriyah.setOnClickListener { setHijriInputs(todayHijri()) }
    }

    private fun setHijriInputs(date: DateParts) {
        binding.etTanggalHijriyah.setText(date.day.toString())
        binding.spinnerBulanKonversi.setSelection(date.month - 1)
        binding.etTahunHijriyah.setText(date.year.toString())
        renderHijriyahKeMasehi()
    }

    private fun renderHijriyahKeMasehi() {
        val day = binding.etTanggalHijriyah.text.toString().toIntOrNull()
        val year = binding.etTahunHijriyah.text.toString().toIntOrNull()
        val month = binding.spinnerBulanKonversi.selectedItemPosition + 1

        val error = when {
            day == null || year == null -> "Isi tanggal dan tahun Hijriyah."
            year !in MIN_HIJRI_YEAR..MAX_HIJRI_YEAR -> "Tahun Hijriyah yang didukung: $MIN_HIJRI_YEAR–$MAX_HIJRI_YEAR H."
            day !in 1..HijriDateUtil.hijriMonthLength(year, month) ->
                "${HijriDateUtil.monthNames[month - 1]} $year H hanya punya ${HijriDateUtil.hijriMonthLength(year, month)} hari."
            else -> null
        }

        if (error != null || day == null || year == null) {
            binding.tvHasilMasehi.text = "—"
            binding.tvHasilMasehiKeterangan.text = error
            binding.tvHasilMasehiKeterangan.setTextColor(ContextCompat.getColor(this, R.color.icon_rose))
            binding.tvHasilMasehiKeterangan.visibility = View.VISIBLE
            return
        }

        val g = HijriDateUtil.hijriToGregorian(year, month, day)
        binding.tvHasilMasehi.text = HijriDateUtil.gregorianLabel(g)
        binding.tvHasilMasehiKeterangan.visibility = View.GONE
    }

    // ===== Helper =====

    private fun todayGregorian(): DateParts {
        val now = Calendar.getInstance()
        return DateParts(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH))
    }

    private fun todayHijri(): DateParts {
        val g = todayGregorian()
        return HijriDateUtil.gregorianToHijri(g.year, g.month, g.day)
    }

    private fun millisOf(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply { clear(); set(year, month - 1, day) }.timeInMillis

    private companion object {
        const val MIN_MASEHI_YEAR = 1600
        const val MAX_MASEHI_YEAR = 2250
        const val MIN_HIJRI_YEAR = 1000
        const val MAX_HIJRI_YEAR = 1670
    }
}
