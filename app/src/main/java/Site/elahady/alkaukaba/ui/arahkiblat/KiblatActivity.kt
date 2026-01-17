package Site.elahady.alkaukaba.ui.arahkiblat

import Site.elahady.alkaukaba.R
import Site.elahady.alkaukaba.databinding.ActivityKiblatBinding
import Site.elahady.alkaukaba.viewmodel.arahkiblat.KiblatViewModel
import Site.elahady.alkaukaba.viewmodel.arahkiblat.KiblatViewModelFactory
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import java.util.*

class KiblatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityKiblatBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var viewModel: KiblatViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKiblatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        viewModel = ViewModelProvider(
            this,
            KiblatViewModelFactory()
        )[KiblatViewModel::class.java]

        observeViewModel()
        checkLocationPermission()

    }
    private fun observeViewModel() {
        viewModel.qiblaAngle.observe(this) { angle ->
            binding.txtQiblaValue.text = "${angle.toInt()}°"
        }

        viewModel.error.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        }
    }
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    private fun checkGpsEnabled() {
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            getLastLocation()
        }
    }

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            if (granted) {
                checkGpsEnabled()
            } else {
                Toast.makeText(
                    this,
                    "Izin lokasi diperlukan untuk menentukan arah kiblat",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private fun checkLocationPermission() {
        if (hasLocationPermission()) {
            checkGpsEnabled()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->

                if (location != null) {
                    onLocationReady(location.latitude, location.longitude)
                } else {
                    requestNewLocation()
                }
            }
    }
    @SuppressLint("MissingPermission")
    private fun requestNewLocation() {

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2000
        )
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdates(1)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation
            if (location != null) {
                onLocationReady(location.latitude, location.longitude)
            }
        }
    }
    private fun onLocationReady(lat: Double, lon: Double) {
//        binding.txtQiblaValue.text = "Lat: %.6f , Lon: %.6f".format(lat, lon)
        getAddressFromLatLong(lat, lon)
        // hitung Qibla Angle
        viewModel.fetchQiblaAngle(lat, lon)
        // update compass
        loadQiblaCompass(lat, lon)
    }
    private fun getAddressFromLatLong(lat: Double, lon: Double) {

        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
//                val kecamatan = address.subLocality ?: "-"
                val kota = address.locality ?: "-"
//                val provinsi = address.adminArea ?: "-"
                val negara = address.countryName ?: "-"
                val lokasiTeks = "$kota, $negara"
                binding.txtLocation.text = lokasiTeks
            }

        } catch (e: Exception) {
            e.printStackTrace()
            binding.txtLocation.text = "Lokasi tidak diketahui"
        }
    }

    private fun loadQiblaCompass(lat: Double, lon: Double) {

        val url =
            "https://api.aladhan.com/v1/qibla/$lat/$lon/compass"

        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.ic_compass_placeholder)
            .error(R.drawable.ic_compass_error)
            .into(binding.imgCompass)
    }
}