package site.elahady.alkaukaba.ui.okultasi

import site.elahady.alkaukaba.adapter.OccultationAdapter
import site.elahady.alkaukaba.databinding.ActivityOkultasiBinding
import site.elahady.alkaukaba.utils.SessionManager
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme
import site.elahady.alkaukaba.viewmodel.okultasi.OkultasiViewModel
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class OkultasiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOkultasiBinding
    private lateinit var viewModel: OkultasiViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sessionManager: SessionManager
    private lateinit var occultationAdapter: OccultationAdapter

    // Default Jakarta (fallback kalau GPS/manual tidak tersedia)
    private var currentLat = -6.2088
    private var currentLng = 106.8456

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOkultasiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        applyStatusBarIconsForTheme()
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        viewModel = ViewModelProvider(this)[OkultasiViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sessionManager = SessionManager(this)

        setupUI()
        setupRecyclerView()
        setupObservers()
        resolveLocationAndCalculate()
    }

    private fun setupUI() {
        binding.includeToolbar.tvToolbarTitle.text = "Okultasi Benda Langit"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnRefreshLoc.setOnClickListener { resolveLocationAndCalculate() }
    }

    private fun setupRecyclerView() {
        occultationAdapter = OccultationAdapter()
        binding.rvOccultation.apply {
            layoutManager = LinearLayoutManager(this@OkultasiActivity)
            adapter = occultationAdapter
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.result.observe(this) { result ->
            occultationAdapter.setData(result.venusOccultations)
            binding.tvEmptyState.visibility = if (result.venusOccultations.isEmpty()) View.VISIBLE else View.GONE
            binding.rvOccultation.visibility = if (result.venusOccultations.isEmpty()) View.GONE else View.VISIBLE
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
        resolveLocationName(currentLat, currentLng)
    }

    // Ubah koordinat mentah jadi nama lokasi (kabupaten/kota, provinsi) via reverse geocoding.
    private fun resolveLocationName(lat: Double, lng: Double) {
        binding.tvLocationName.text = "Mendeteksi lokasi..."
        lifecycleScope.launch(Dispatchers.IO) {
            val placeName = try {
                @Suppress("DEPRECATION")
                val results = Geocoder(this@OkultasiActivity, Locale("in", "ID")).getFromLocation(lat, lng, 1)
                val address = results?.firstOrNull()
                listOfNotNull(address?.subAdminArea ?: address?.adminArea, address?.countryName)
                    .joinToString(", ")
                    .ifBlank { null }
            } catch (e: Exception) {
                null
            }
            withContext(Dispatchers.Main) {
                binding.tvLocationName.text = placeName
                    ?: "%.4f, %.4f".format(Locale.US, lat, lng)
            }
        }
    }

    private fun runCalculation() {
        viewModel.calculateOccultations(currentLat, currentLng, 0.0)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 101
    }
}
