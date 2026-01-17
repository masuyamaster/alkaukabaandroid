package Site.elahady.alkaukaba.ui.arahkiblat

import Site.elahady.alkaukaba.databinding.ActivityKiblatBinding
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
import com.google.android.gms.location.*
import java.util.*

class KiblatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityKiblatBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKiblatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkLocationPermission()

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
        binding.txtQiblaValue.text = "Lat: %.6f , Lon: %.6f".format(lat, lon)
        getAddressFromLatLong(lat, lon)
        // NEXT:
        // hitung Qibla Angle
        // update compass
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
}