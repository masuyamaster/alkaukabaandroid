package site.elahady.alkaukaba.ui.awalbulan

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.databinding.ActivityAwalBulanBinding
import site.elahady.alkaukaba.databinding.ItemHilalBreakdownRowBinding
import site.elahady.alkaukaba.utils.MoonTilt
import site.elahady.alkaukaba.utils.SessionManager
import site.elahady.alkaukaba.utils.prayerbreakdown.PrayerBreakdownSection
import site.elahady.alkaukaba.viewmodel.hilal.HilalViewModel
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AwalBulanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAwalBulanBinding
    private lateinit var viewModel: HilalViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sessionManager: SessionManager

    // Default Jakarta (fallback kalau GPS/manual tidak tersedia)
    private var currentLat = -6.2088
    private var currentLng = 106.8456

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAwalBulanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
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
            setOnClickListener { viewModel.generatePdf(this@AwalBulanActivity) }
        }
        binding.btnRefreshLoc.setOnClickListener { resolveLocationAndCalculate() }
        binding.btnCalculate.setOnClickListener { runCalculation() }
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

    private fun resolveLocationAndCalculate() {
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
        viewModel.calculateHilal(currentLat, currentLng, height)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }
}
