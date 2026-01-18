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
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
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

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null

    private var currentAzimuth = 0f
    private var qiblaAngle = 0f

    private var smoothedAzimuth = 0f
    private var lastRotation = 0f

    private val smoothingFactor = 0.15f   // 0.1 – 0.2 ideal
    private val qiblaThresshold = 3f // derajat

    private var isCalibrationVisible = false
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKiblatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        viewModel = ViewModelProvider(
            this,
            KiblatViewModelFactory()
        )[KiblatViewModel::class.java]

        observeViewModel()
        checkLocationPermission()

        viewModel.qiblaAngle.observe(this) { angle ->

            qiblaAngle = angle.toFloat()   // <-- penting
            binding.txtQiblaValue.text = "${angle.toInt()}°"
            rotateCompass()
        }
    }
    @SuppressLint("SetTextI18n")
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
    @SuppressLint("SetTextI18n")
    private fun getAddressFromLatLong(lat: Double, lon: Double) {

        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val kecamatan = address.subLocality ?: address.locality
                val kota = address.subAdminArea ?: address.adminArea
//                val provinsi = address.adminArea ?: "-"
                val negara = address.countryName ?: "-"
                val lokasiTeks = "$kecamatan, $kota, $negara"
                binding.txtLocation.text = lokasiTeks
            }

        } catch (e: Exception) {
            e.printStackTrace()
            binding.txtLocation.text = getString(R.string.infoLokasi)
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

    private fun lowPassFilter(input: Float, output: Float): Float {
        return output + smoothingFactor * (input - output)
    }

    private val sensorListener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {

            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {

                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(
                    rotationMatrix,
                    event.values
                )

                val adjustedMatrix = FloatArray(9)

                // REMAP coordinate sesuai PORTRAIT
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Y,
                    adjustedMatrix
                )

                val orientation = FloatArray(3)
                SensorManager.getOrientation(adjustedMatrix, orientation)

                val azimuthRad = orientation[0]
                val azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()

                val normalized = (azimuthDeg + 360) % 360

                smoothedAzimuth = lowPassFilter(normalized, smoothedAzimuth)
                currentAzimuth = smoothedAzimuth

                rotateCompassSmooth()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            val needCalibration = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
                    accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW

            if (needCalibration && !isCalibrationVisible) {
                binding.calibrationHint.visibility = View.VISIBLE
                isCalibrationVisible = true
            }

            if (!needCalibration && isCalibrationVisible) {
                binding.calibrationHint.visibility = View.GONE
                isCalibrationVisible = false
            }

        }
    }

    private fun getShortestRotation(target: Float, current: Float): Float {
        var diff = target - current
        while (diff > 180) diff -= 360
        while (diff < -180) diff += 360
        return current + diff
    }

    private fun rotateCompassSmooth() {

//        val target = qiblaAngle - currentAzimuth
        val smoothTarget = getShortestRotation(smoothedAzimuth, lastRotation)

        lastRotation = smoothTarget
        println("qibla angle $qiblaAngle azimuth $currentAzimuth lastrotation $lastRotation")
        binding.imgCompass.rotation = -smoothTarget

        checkQiblaAlignment()
    }

    private fun checkQiblaAlignment() {

        if (isAlignedToQibla()) {
            binding.qiblaAngleContainer.background =
                ContextCompat.getDrawable(this, R.drawable.bg_qibla_match)
            binding.txtQiblaLabel.setTextColor(Color.WHITE)
            binding.txtQiblaValue.setTextColor(Color.WHITE)

        } else {
            binding.qiblaAngleContainer.background =
                ContextCompat.getDrawable(this, R.drawable.bg_qibla_angle)
            binding.txtQiblaLabel.setTextColor(Color.BLACK)
            binding.txtQiblaValue.setTextColor(Color.BLACK)
        }
    }

    private fun rotateCompass() {

        val targetRotation = qiblaAngle - currentAzimuth

        binding.imgCompass.animate()
            .rotation(targetRotation)
            .setDuration(200)
            .start()
    }

    private fun isAlignedToQibla(): Boolean {
        val diff = kotlin.math.abs(currentAzimuth - qiblaAngle)
        return diff <= qiblaThresshold || diff >= 360 - qiblaThresshold
    }

    override fun onResume() {
        super.onResume()
        rotationSensor?.also {
            sensorManager.registerListener(
                sensorListener,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorListener)
    }
}