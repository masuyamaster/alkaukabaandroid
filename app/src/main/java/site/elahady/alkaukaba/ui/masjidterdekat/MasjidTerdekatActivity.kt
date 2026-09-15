package site.elahady.alkaukaba.ui.masjidterdekat

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.adapter.NearbyMosqueAdapter
import site.elahady.alkaukaba.databinding.ActivityMasjidTerdekatBinding
import site.elahady.alkaukaba.model.NearbyMosque
import site.elahady.alkaukaba.utils.Resource
import site.elahady.alkaukaba.utils.SessionManager
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.viewmodel.masjidterdekat.MasjidTerdekatViewModel
import java.util.Locale

/** Cari masjid terdekat dari lokasi user, sumber data Overpass API (OpenStreetMap) - dipilih
 * dibanding Google Places karena gratis total tanpa API key/billing GCP (lihat
 * docs/features/masjid-terdekat.md). Pola permission/GPS/lokasi manual di bawah ini sama persis
 * dengan [site.elahady.alkaukaba.ui.arahkiblat.KiblatActivity]. */
class MasjidTerdekatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMasjidTerdekatBinding
    private lateinit var viewModel: MasjidTerdekatViewModel
    private lateinit var adapter: NearbyMosqueAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sessionManager: SessionManager

    private var lastLat: Double? = null
    private var lastLon: Double? = null
    private var radiusMeters = DEFAULT_RADIUS_METERS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMasjidTerdekatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        applyStatusBarIconsForTheme()
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        binding.includeToolbar.tvToolbarTitle.text = getString(R.string.titleMasjidTerdekat)
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        sessionManager = SessionManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        viewModel = ViewModelProvider(this)[MasjidTerdekatViewModel::class.java]

        adapter = NearbyMosqueAdapter { mosque -> openInMaps(mosque) }
        binding.rvMasjid.layoutManager = LinearLayoutManager(this)
        binding.rvMasjid.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { checkLocationPermission() }
        binding.btnPerluasRadius.setOnClickListener { widenRadiusAndSearch() }

        setupObserver()
        checkLocationPermission()
    }

    private fun setupObserver() {
        viewModel.result.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.swipeRefresh.isRefreshing = false
                    val hasData = !resource.data.isNullOrEmpty()
                    binding.progressBar.visibility = if (hasData) View.GONE else View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    val data = resource.data ?: emptyList()
                    adapter.setData(data)
                    if (data.isEmpty()) {
                        showEmptyState("Belum ditemukan masjid dalam radius ${formatRadius()}. Coba perluas radius pencarian.")
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    val data = resource.data
                    if (data.isNullOrEmpty()) {
                        adapter.setData(emptyList())
                        showEmptyState(resource.message ?: "Gagal memuat data masjid terdekat.")
                    } else {
                        adapter.setData(data)
                        binding.layoutEmptyState.visibility = View.GONE
                        Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showEmptyState(message: String) {
        binding.tvEmptyState.text = message
        binding.layoutEmptyState.visibility = View.VISIBLE
    }

    private fun widenRadiusAndSearch() {
        radiusMeters = (radiusMeters * 2).coerceAtMost(MAX_RADIUS_METERS)
        val lat = lastLat
        val lon = lastLon
        if (lat != null && lon != null) {
            search(lat, lon)
        } else {
            checkLocationPermission()
        }
    }

    private fun search(lat: Double, lon: Double) {
        lastLat = lat
        lastLon = lon
        binding.tvInfoPencarian.text = "Radius ${formatRadius()} dari lokasi Anda saat ini"
        viewModel.search(lat, lon, radiusMeters)
    }

    private fun formatRadius(): String {
        return if (radiusMeters < 1000) {
            "$radiusMeters m"
        } else {
            String.format(Locale("id", "ID"), "%.0f km", radiusMeters / 1000.0)
        }
    }

    private fun openInMaps(mosque: NearbyMosque) {
        val uri = Uri.parse("geo:0,0?q=${mosque.latitude},${mosque.longitude}(${Uri.encode(mosque.name)})")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Tidak ada aplikasi peta yang terpasang", Toast.LENGTH_SHORT).show()
        }
    }

    // ---- Lokasi: permission/GPS/mode manual - pola sama dengan KiblatActivity, lihat
    // docs/features/arah-kiblat.md ----

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkGpsEnabled() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            getLastLocation()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            checkGpsEnabled()
        } else {
            binding.progressBar.visibility = View.GONE
            showEmptyState("Izin lokasi diperlukan untuk mencari masjid terdekat.")
            Toast.makeText(this, "Izin lokasi diperlukan untuk mencari masjid terdekat", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkLocationPermission() {
        if (sessionManager.isManualLocationMode()) {
            // Setting lokasi global (lihat KonfigurasiActivity) - lewati GPS/permission sama sekali.
            search(sessionManager.getManualLat(), sessionManager.getManualLng())
            return
        }

        if (hasLocationPermission()) {
            checkGpsEnabled()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    search(location.latitude, location.longitude)
                } else {
                    requestNewLocation()
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                showEmptyState("Gagal mendapatkan lokasi. Coba lagi.")
            }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocation() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdates(1)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation
            if (location != null) {
                search(location.latitude, location.longitude)
            }
        }
    }

    companion object {
        private const val DEFAULT_RADIUS_METERS = 5000
        private const val MAX_RADIUS_METERS = 20000
    }
}
