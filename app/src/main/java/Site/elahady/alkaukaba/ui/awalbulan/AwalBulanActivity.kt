package site.elahady.alkaukaba.ui.awalbulan

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.databinding.ActivityAwalBulanBinding
import site.elahady.alkaukaba.databinding.ItemHilalResultCardBinding
import site.elahady.alkaukaba.databinding.ItemPrayerBreakdownBinding
import site.elahady.alkaukaba.utils.SessionManager
import site.elahady.alkaukaba.utils.prayerbreakdown.PrayerBreakdownSection
import site.elahady.alkaukaba.viewmodel.hilal.HilalViewModel
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
            binding.tvHasil.visibility = View.VISIBLE

            binding.tvBulanHijriyah.text = result.bulanHijriyahLabel
            binding.tvTanggalGhurub.text = "Ghurub markaz: ${result.tanggalGhurubLabel}"
            binding.tvStatusBadge.text = result.statusBadge
            binding.tvStatusBadge.setBackgroundResource(
                if (result.hilalMemenuhiKriteria) android.R.color.holo_green_dark
                else android.R.color.holo_red_dark
            )

            bindMetricCard(binding.cardTinggiHilal, "Tinggi Hilal Mar'i", "%.4f°".format(Locale.US, result.tinggiHilal))
            bindMetricCard(binding.cardElongasi, "Elongasi", "%.4f°".format(Locale.US, result.elongasi))
            bindMetricCard(binding.cardMukuts, "Mukuts (Lama di Atas Ufuk)", "%.1f menit".format(Locale.US, result.mukutsMenit))

            renderBreakdown(result.breakdownSections)
        }
    }

    private fun bindMetricCard(card: ItemHilalResultCardBinding, title: String, value: String) {
        card.tvTitle.text = title
        card.tvValue.text = value
        card.tvSubtitle.visibility = View.GONE
        card.tvDetailLink.visibility = View.GONE
    }

    // Accordion rincian perhitungan - reuse komponen yang sama dipakai fitur Waktu Sholat
    // (item_prayer_breakdown.xml + item_breakdown_row.xml), lihat WaktuSholatActivity.renderPrayerBreakdown().
    private fun renderBreakdown(sections: List<PrayerBreakdownSection>) {
        binding.layoutHilalBreakdownContainer.removeAllViews()

        sections.forEach { section ->
            val itemBinding = ItemPrayerBreakdownBinding.inflate(
                layoutInflater, binding.layoutHilalBreakdownContainer, false
            )
            itemBinding.tvPrayerLabel.text = section.prayerLabel
            itemBinding.tvResultTime.text = section.resultTime

            section.rows.forEach { row ->
                val rowView = layoutInflater.inflate(R.layout.item_breakdown_row, itemBinding.layoutBody, false)
                rowView.findViewById<TextView>(R.id.tvRowLabel).text = row.label
                rowView.findViewById<TextView>(R.id.tvRowValue).text = row.value
                itemBinding.layoutBody.addView(rowView)
            }

            itemBinding.rowHeader.setOnClickListener {
                val isExpanded = itemBinding.layoutBody.visibility == View.VISIBLE
                itemBinding.layoutBody.visibility = if (isExpanded) View.GONE else View.VISIBLE
                itemBinding.tvChevron.text = if (isExpanded) "⌄" else "⌃"
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
        binding.etCoordinates.setText("$currentLat, $currentLng")
        binding.tvLatLongDetail.text = "Latitude: $currentLat Longitude: $currentLng"
    }

    private fun runCalculation() {
        val height = binding.etKetinggian.text.toString().toDoubleOrNull() ?: 0.0
        viewModel.calculateHilal(currentLat, currentLng, height)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }
}
