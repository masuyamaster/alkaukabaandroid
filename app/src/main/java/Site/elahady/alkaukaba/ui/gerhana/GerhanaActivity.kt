package site.elahady.alkaukaba.ui.gerhana

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.adapter.LunarEclipseAdapter
import site.elahady.alkaukaba.adapter.SolarEclipseAdapter
import site.elahady.alkaukaba.databinding.ActivityGerhanaBinding
import site.elahady.alkaukaba.utils.SessionManager
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.viewmodel.gerhana.GerhanaViewModel
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

class GerhanaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGerhanaBinding
    private lateinit var viewModel: GerhanaViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sessionManager: SessionManager
    private lateinit var lunarAdapter: LunarEclipseAdapter
    private lateinit var solarAdapter: SolarEclipseAdapter

    // Default Jakarta (fallback kalau GPS/manual tidak tersedia)
    private var currentLat = -6.2088
    private var currentLng = 106.8456

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGerhanaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        viewModel = ViewModelProvider(this)[GerhanaViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sessionManager = SessionManager(this)

        setupUI()
        setupTabs()
        setupRecyclerViews()
        setupObservers()
        resolveLocationAndCalculate()
    }

    private fun setupUI() {
        binding.includeToolbar.tvToolbarTitle.text = "Gerhana"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnRefreshLoc.setOnClickListener { resolveLocationAndCalculate() }
    }

    private fun setupTabs() {
        updateTabState(isLunar = true)
        binding.btnTabBulan.setOnClickListener { updateTabState(isLunar = true) }
        binding.btnTabMatahari.setOnClickListener { updateTabState(isLunar = false) }
    }

    private fun updateTabState(isLunar: Boolean) {
        val colorActive = ContextCompat.getColor(this, R.color.navy_dongker)
        val colorInactive = ContextCompat.getColor(this, R.color.waktu_sholat_icon_muted)

        val activeTab = if (isLunar) binding.btnTabBulan else binding.btnTabMatahari
        val inactiveTab = if (isLunar) binding.btnTabMatahari else binding.btnTabBulan

        activeTab.setBackgroundResource(R.drawable.bg_tab_underline_active_navy)
        activeTab.setTextColor(colorActive)
        activeTab.setTypeface(null, android.graphics.Typeface.BOLD)

        inactiveTab.setBackgroundResource(R.drawable.bg_tab_underline_inactive)
        inactiveTab.setTextColor(colorInactive)
        inactiveTab.setTypeface(null, android.graphics.Typeface.NORMAL)

        binding.rvLunarEclipse.visibility = if (isLunar) View.VISIBLE else View.GONE
        binding.rvSolarEclipse.visibility = if (isLunar) View.GONE else View.VISIBLE
    }

    private fun setupRecyclerViews() {
        lunarAdapter = LunarEclipseAdapter()
        binding.rvLunarEclipse.apply {
            layoutManager = LinearLayoutManager(this@GerhanaActivity)
            adapter = lunarAdapter
        }

        solarAdapter = SolarEclipseAdapter()
        binding.rvSolarEclipse.apply {
            layoutManager = LinearLayoutManager(this@GerhanaActivity)
            adapter = solarAdapter
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.result.observe(this) { result ->
            lunarAdapter.setData(result.lunarEclipses)
            solarAdapter.setData(result.solarEclipses)
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
                val results = Geocoder(this@GerhanaActivity, Locale("in", "ID")).getFromLocation(lat, lng, 1)
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
        viewModel.calculateEclipses(currentLat, currentLng, 0.0)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }
}
