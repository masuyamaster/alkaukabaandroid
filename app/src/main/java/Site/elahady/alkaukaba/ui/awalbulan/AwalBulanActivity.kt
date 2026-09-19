package site.elahady.alkaukaba.ui.awalbulan

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.databinding.ActivityAwalBulanBinding
import site.elahady.alkaukaba.databinding.ItemHilalBreakdownRowBinding
import site.elahady.alkaukaba.ui.konfigurasi.PageLocationOverride
import site.elahady.alkaukaba.utils.HijriDateUtil
import site.elahady.alkaukaba.utils.MoonTilt
import site.elahady.alkaukaba.utils.SessionManager
import site.elahady.alkaukaba.utils.prayerbreakdown.PrayerBreakdownSection
import site.elahady.alkaukaba.viewmodel.hilal.HilalViewModel
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class AwalBulanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAwalBulanBinding
    private lateinit var viewModel: HilalViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sessionManager: SessionManager

    // Override lokasi khusus halaman Awal Bulan (prefs terpisah dari SessionManager) - lihat
    // PageLocationOverride.
    private val pageLocation = PageLocationOverride(
        activity = this,
        prefsName = PAGE_PREFS_NAME,
        fallbackLocation = { currentLat to currentLng },
        onSaved = { resolveLocationAndCalculate() }
    )

    // Default Jakarta (fallback kalau GPS/manual tidak tersedia)
    private var currentLat = -6.2088
    private var currentLng = 106.8456

    // 0 = bulan terdekat ke depan dari sekarang (default), + = maju N bulan, - = mundur N bulan.
    // Diturunkan dari selisih pilihan spinnerBulan/TahunHijriyah terhadap baseline (lihat setupBulanSelectors()).
    private var currentMonthOffset = 0

    // Bulan (1-12) & tahun Hijriyah (tabular, HANYA utk isi default+konversi selector -- lihat
    // HijriDateUtil) untuk offset 0 (bulan terdekat ke depan dari sekarang, dihitung sekali di onCreate).
    private var baselineHijriYear = 0
    private var baselineHijriMonth = 1
    private lateinit var hijriYearRange: IntRange

    // Spinner otomatis fire onItemSelected sekali per spinner begitu adapter/selection awal dipasang
    // (bukan aksi user) -- counter ini menghitung mundur 2 callback awal itu (1 per spinner) sebelum
    // callback berikutnya dianggap sebagai pilihan user yang harus memicu hitung ulang.
    private var pendingInitialSpinnerCallbacks = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAwalBulanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        applyStatusBarIconsForTheme()
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        viewModel = ViewModelProvider(this)[HilalViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sessionManager = SessionManager(this)

        setupUI()
        setupObservers()
        resolveLocationAndCalculate()
    }

    private fun setupUI() {
        binding.includeToolbar.tvToolbarTitle.text = "Awal Bulan Hijriyah"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.includeToolbar.btnToolbarAction.apply {
            visibility = View.VISIBLE
            setImageResource(R.drawable.ic_pdf_icon)
            setOnClickListener { openLaporanHisab() }
        }
        binding.btnRefreshLoc.setOnClickListener { pageLocation.showSheet() }
        binding.btnCalculate.setOnClickListener { runCalculation() }

        setupBulanSelectors()
    }

    // Isi spinner bulan (12 nama Hijriyah) & tahun (baseline +-10 tahun) dengan default = bulan
    // terdekat ke depan dari sekarang (offset 0), lalu recalculate begitu user ganti salah satu.
    private fun setupBulanSelectors() {
        val (baseYear, baseMonth) = HijriDateUtil.nextMonthYearMonth(Calendar.getInstance())
        baselineHijriYear = baseYear
        baselineHijriMonth = baseMonth
        hijriYearRange = (baseYear - 10)..(baseYear + 10)

        val monthAdapter = ArrayAdapter(this, R.layout.item_spinner_selector, HijriDateUtil.monthNames)
        monthAdapter.setDropDownViewResource(R.layout.item_spinner_selector_dropdown)
        binding.spinnerBulanHijriyah.adapter = monthAdapter
        binding.spinnerBulanHijriyah.setSelection(baseMonth - 1)

        val yearLabels = hijriYearRange.map { "$it H" }
        val yearAdapter = ArrayAdapter(this, R.layout.item_spinner_selector, yearLabels)
        yearAdapter.setDropDownViewResource(R.layout.item_spinner_selector_dropdown)
        binding.spinnerTahunHijriyah.adapter = yearAdapter
        binding.spinnerTahunHijriyah.setSelection(hijriYearRange.indexOf(baseYear))

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (pendingInitialSpinnerCallbacks > 0) {
                    pendingInitialSpinnerCallbacks--
                    return
                }
                val selectedMonth = binding.spinnerBulanHijriyah.selectedItemPosition + 1
                val selectedYear = hijriYearRange.first + binding.spinnerTahunHijriyah.selectedItemPosition
                currentMonthOffset = (selectedYear * 12 + selectedMonth) - (baselineHijriYear * 12 + baselineHijriMonth)
                runCalculation()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.spinnerBulanHijriyah.onItemSelectedListener = listener
        binding.spinnerTahunHijriyah.onItemSelectedListener = listener
    }

    private fun setupObservers() {
        viewModel.calculationResult.observe(this) { result ->
            binding.layoutResultContainer.visibility = View.VISIBLE

            binding.tvBulanHijriyah.text = result.bulanHijriyahLabel
            binding.tvTanggalGhurub.text = "📅 Ghurub: ${result.tanggalGhurubLabel}"
            binding.tvStatusBadge.text = result.statusBadge
            if (result.hilalMemenuhiKriteria) {
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_green)
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.pill_green_text))
            } else {
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_red)
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.pill_red_text))
            }

            binding.tvTinggiHilalValue.text = "%.2f°".format(Locale.US, result.tinggiHilal)
            binding.tvElongasiValue.text = "%.2f°".format(Locale.US, result.elongasi)
            binding.tvMukutsValue.text = "%.1f menit".format(Locale.US, result.mukutsMenit)

            val tiltDegrees = MoonTilt.brightLimbAngleDegrees(
                moonAzimuthDeg = result.azimuthHilal,
                moonAltitudeDeg = result.tinggiHilal,
                sunAzimuthDeg = result.azimuthMatahari,
                sunAltitudeDeg = result.tinggiMatahari
            )
            binding.moonPhaseView.setRealisticTexture(false)
            binding.moonPhaseView.setSoftNightSide(false)
            binding.moonPhaseView.setWaxingCrescent(result.illumFraction)
            binding.moonPhaseView.setBrightLimbAngle(tiltDegrees)
            binding.tvHilalIllumination.text = "%.2f%% tersinari".format(Locale.US, result.illumFraction * 100.0)
            binding.tvHilalAzimuth.text = "Azimuth %.1f° — cari dekat titik terbenam Matahari (Az %.1f°)"
                .format(Locale.US, result.azimuthHilal, result.azimuthMatahari)

            renderBreakdown(result.breakdownSections)
        }
    }

    // Accordion rincian perhitungan - satu kartu memanjang dengan pemisah garis tipis antar seksi.
    private fun renderBreakdown(sections: List<PrayerBreakdownSection>) {
        binding.layoutHilalBreakdownContainer.removeAllViews()

        sections.forEachIndexed { index, section ->
            val itemBinding = ItemHilalBreakdownRowBinding.inflate(
                layoutInflater, binding.layoutHilalBreakdownContainer, false
            )
            itemBinding.tvSectionLabel.text = section.prayerLabel
            itemBinding.tvSectionValue.text = section.resultTime
            itemBinding.divider.visibility = if (index == sections.lastIndex) View.GONE else View.VISIBLE

            section.rows.forEach { row ->
                val rowView = layoutInflater.inflate(R.layout.item_breakdown_row, itemBinding.layoutBody, false)
                rowView.findViewById<TextView>(R.id.tvRowLabel).text = row.label
                rowView.findViewById<TextView>(R.id.tvRowValue).text = row.value
                itemBinding.layoutBody.addView(rowView)
            }

            itemBinding.rowHeader.setOnClickListener {
                val isExpanded = itemBinding.layoutBody.visibility == View.VISIBLE
                itemBinding.layoutBody.visibility = if (isExpanded) View.GONE else View.VISIBLE
                itemBinding.ivChevron.animate().rotation(if (isExpanded) 0f else 180f).setDuration(150).start()
            }

            binding.layoutHilalBreakdownContainer.addView(itemBinding.root)
        }
    }

    private fun openLaporanHisab() {
        val result = viewModel.calculationResult.value
        if (result == null) {
            Toast.makeText(this, "Hitung hasil hisab terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, LaporanHisabActivity::class.java).apply {
            putExtra(LaporanHisabActivity.EXTRA_RESULT, result)
        })
    }

    private fun resolveLocationAndCalculate() {
        if (pageLocation.hasOverride()) {
            currentLat = pageLocation.lat()
            currentLng = pageLocation.lng()
            updateCoordinateDisplay()
            runCalculation()
            return
        }
        if (sessionManager.isManualLocationMode()) {
            currentLat = sessionManager.getManualLat()
            currentLng = sessionManager.getManualLng()
            updateCoordinateDisplay()
            runCalculation()
            return
        }
        getGpsLocation()
    }

    private fun getGpsLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLat = location.latitude
                currentLng = location.longitude
            } else {
                Toast.makeText(this, "GPS tidak ditemukan, menggunakan default Jakarta", Toast.LENGTH_SHORT).show()
            }
            updateCoordinateDisplay()
            runCalculation()
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal mengambil lokasi GPS", Toast.LENGTH_SHORT).show()
            updateCoordinateDisplay()
            runCalculation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getGpsLocation()
        } else {
            Toast.makeText(this, "Izin lokasi ditolak, menggunakan default Jakarta", Toast.LENGTH_SHORT).show()
            updateCoordinateDisplay()
            runCalculation()
        }
    }

    private fun updateCoordinateDisplay() {
        binding.tvLatLongDetail.text = "Lintang: %.4f | Bujur: %.4f".format(Locale.US, currentLat, currentLng)
        resolveLocationName(currentLat, currentLng)
    }

    // Ubah koordinat mentah jadi nama lokasi (kabupaten/kota, provinsi) via reverse geocoding.
    private fun resolveLocationName(lat: Double, lng: Double) {
        binding.tvLocationName.text = "📍 Mendeteksi lokasi..."
        lifecycleScope.launch(Dispatchers.IO) {
            val placeName = try {
                @Suppress("DEPRECATION")
                val results = Geocoder(this@AwalBulanActivity, Locale("in", "ID")).getFromLocation(lat, lng, 1)
                val address = results?.firstOrNull()
                listOfNotNull(address?.subAdminArea ?: address?.adminArea, address?.countryName)
                    .joinToString(", ")
                    .ifBlank { null }
            } catch (e: Exception) {
                null
            }
            withContext(Dispatchers.Main) {
                binding.tvLocationName.text = "📍 " + (placeName
                    ?: "%.4f, %.4f".format(Locale.US, lat, lng))
            }
        }
    }

    private fun runCalculation() {
        val height = binding.etKetinggian.text.toString().toDoubleOrNull() ?: 0.0
        try {
            viewModel.calculateHilal(
                currentLat, currentLng, height,
                sessionManager.getHisabAwalBulanMethod(), currentMonthOffset
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal menghitung bulan ini (${e.message})", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
        private const val PAGE_PREFS_NAME = "AwalBulanPagePrefs"
    }
}
