package Site.elahady.alkaukaba

import Site.elahady.alkaukaba.api.RetrofitClient
import Site.elahady.alkaukaba.repo.PrayerRepository
import Site.elahady.alkaukaba.databinding.ActivityMainBinding
import Site.elahady.alkaukaba.viewmodel.MainViewModel
import Site.elahady.alkaukaba.viewmodel.MainViewModelFactory
import Site.elahady.alkaukaba.utils.Resource
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupHeaderDate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkLocationPermission()
        setupObservers()
        setupNavigation() // Pindahkan setup tombol ke fungsi terpisah
    }

    private fun setupViewModel() {
        // Inisialisasi Repository & ViewModel
        val apiService = RetrofitClient.instance
        val repository = PrayerRepository(apiService)
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
    }

    private fun setupObservers() {
        // 1. Observe Data Sholat
        viewModel.prayerState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.tvStatus.text = "Memuat data..."
                }
                is Resource.Success -> {
                    val data = resource.data
                    if (data != null) {
                        binding.tvPrayerName.text = data.prayerName
                        binding.tvLabelUpcoming.text = data.topLabel
                        binding.tvStatus.text = data.statusText

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val color = if (data.isPassed) getColor(android.R.color.holo_red_dark)
                            else getColor(android.R.color.holo_green_dark)
                            binding.tvStatus.setTextColor(color)
                        }
                    }
                }
                is Resource.Error -> {
                    binding.tvStatus.text = "Error: ${resource.message}"
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 2. Observe Lokasi
        viewModel.locationName.observe(this) { locationText ->
            binding.textLoc.text = locationText
        }

        // 3. Observe Hari Besar
        viewModel.holidayAlert.observe(this) { holidayName ->
            if (holidayName != null) {
                Toast.makeText(this, "Hari Besar: $holidayName", Toast.LENGTH_LONG).show()
                // Opsional: binding.tvDateNow.setTextColor(...)
            }
        }
    }

    private fun setupHeaderDate() {
        val dateFormatFull = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        binding.tvDateNow.text = dateFormatFull.format(Date())
    }

    // --- Location Logic (View Layer karena butuh Permission Activity) ---
    @RequiresApi(Build.VERSION_CODES.N)
    private fun checkLocationPermission() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
            ) {
                getUserLocation()
            } else {
                Toast.makeText(this, "Izin lokasi ditolak, menggunakan default Jakarta", Toast.LENGTH_SHORT).show()
                // Fallback Logic: Panggil ViewModel dengan koordinat Jakarta
                viewModel.fetchPrayerData(-6.2088, 106.8456)
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            getUserLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Beri tahu ViewModel ada koordinat baru
                viewModel.fetchPrayerData(location.latitude, location.longitude)

                // Beri tahu ViewModel untuk cari nama jalan (Geocoder)
                val geocoder = Geocoder(this, Locale("id", "ID"))
                viewModel.fetchAddressName(geocoder, location.latitude, location.longitude)
            }
        }
    }

    private fun setupNavigation() {
        binding.btKiblat.setOnClickListener {
            Toast.makeText(this, "Fitur Kiblat", Toast.LENGTH_SHORT).show()
        }
        // Tombol lainnya...
    }
}