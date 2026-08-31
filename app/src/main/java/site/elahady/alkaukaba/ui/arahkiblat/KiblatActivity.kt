package site.elahady.alkaukaba.ui.arahkiblat

import site.elahady.alkaukaba.databinding.ActivityKiblatBinding
import site.elahady.alkaukaba.viewmodel.arahkiblat.KiblatViewModel
import site.elahady.alkaukaba.viewmodel.arahkiblat.KiblatViewModelFactory
import site.elahady.alkaukaba.utils.QiblaCalculator
import site.elahady.alkaukaba.utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
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
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import com.google.android.gms.location.*
import java.util.*
import site.elahady.alkaukaba.R

class KiblatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityKiblatBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var viewModel: KiblatViewModel
    private lateinit var sessionManager: SessionManager

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null

    private var currentAzimuth = 0f
    private var qiblaAngle = 0f

    private var smoothedAzimuth = 0f

    private val smoothingFactor = 0.15f   // 0.1 – 0.2 ideal
    private val qiblaThresshold = 3f // derajat
    private var isCalibrationVisible = false
    private var qiblaBreakdown: QiblaCalculator.QiblaBreakdownResult? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKiblatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        binding.includeToolbar.toolbarDefault.applySystemBarInsetsPadding(applyTop = true)
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sessionManager = SessionManager(this)
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
        }

        binding.includeToolbar.tvToolbarTitle.text = getString(R.string.titleArahKiblat)
        binding.includeToolbar.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.infoCard.setOnClickListener { showQiblaBreakdownSheet() }
        binding.btnQiblaDetail.setOnClickListener { showQiblaBreakdownSheet() }
    }

    private fun showQiblaBreakdownSheet() {
        val breakdown = qiblaBreakdown
        if (breakdown == null) {
            Toast.makeText(this, "Lokasi belum siap, coba lagi sebentar", Toast.LENGTH_SHORT).show()
            return
        }

        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_qibla_breakdown, null)
        bottomSheetDialog.setContentView(view)

        val subtitle = view.findViewById<android.widget.TextView>(R.id.tvQiblaBreakdownSubtitle)
        subtitle.text = if (sessionManager.getQiblaSource() == SessionManager.QIBLA_SOURCE_MANUAL) {
            "Perhitungan manual (Al Hasib - Alkaukaba Team) — jadi sumber utama sudut & kompas di layar ini (lihat Konfigurasi > Arah Kiblat)."
        } else {
            "Perhitungan manual (Al Hasib - Alkaukaba Team), untuk referensi. Sudut & kompas utama di layar ini tetap dari Aladhan API (lihat Konfigurasi > Arah Kiblat)."
        }

        val container = view.findViewById<android.widget.LinearLayout>(R.id.layoutQiblaBreakdownContainer)
        breakdown.rows.forEach { row ->
            val rowView = layoutInflater.inflate(R.layout.item_breakdown_row, container, false)
            rowView.findViewById<android.widget.TextView>(R.id.tvRowLabel).text = row.label
            rowView.findViewById<android.widget.TextView>(R.id.tvRowValue).text = row.value
            container.addView(rowView)
        }

        bottomSheetDialog.show()
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
        if (sessionManager.isManualLocationMode()) {
            // Setting lokasi global (lihat KonfigurasiActivity) - lewati GPS/permission sama sekali.
            onLocationReady(sessionManager.getManualLat(), sessionManager.getManualLng())
            return
        }

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
    @SuppressLint("SetTextI18n")
    private fun onLocationReady(lat: Double, lon: Double) {
//        binding.txtQiblaValue.text = "Lat: %.6f , Lon: %.6f".format(lat, lon)
        if (sessionManager.isManualLocationMode()) {
            // Lokasi manual: tampilkan koordinatnya langsung, jangan reverse-geocode - titik manual
            // (mis. markaz tanpa nama jalan) sering tidak resolve ke subLocality/locality (null,
            // null, ...) lewat Geocoder.
            binding.txtLocation.text = "Lokasi manual: %.4f, %.4f".format(lat, lon)
        } else {
            getAddressFromLatLong(lat, lon)
        }
        // breakdown perhitungan manual (Al Hasib) - ditampilkan lewat tombol info
        val breakdown = QiblaCalculator.calculateBreakdown(lat, lon)
        qiblaBreakdown = breakdown

        // Sumber sudut kiblat yang jadi acuan utama (lihat KonfigurasiActivity > Arah Kiblat).
        if (sessionManager.getQiblaSource() == SessionManager.QIBLA_SOURCE_MANUAL) {
            // Set langsung dari rumus manual - JANGAN panggil fetchQiblaAngle di sini, observer-nya
            // akan menimpa balik nilai ini kalau response Aladhan datang belakangan (race condition).
            qiblaAngle = breakdown.utsbDegree.toFloat()
            binding.txtQiblaValue.text = "${breakdown.utsbDegree.toInt()}°"
        } else {
            viewModel.fetchQiblaAngle(lat, lon)
        }
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

                val unwrapped = unwrapAngle(normalized, smoothedAzimuth)

                smoothedAzimuth = lowPassFilter(unwrapped, smoothedAzimuth)
                currentAzimuth = (smoothedAzimuth + 360) % 360

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

    private fun rotateCompassSmooth() {
        // Dial berputar mengikuti heading device supaya marker Utara tetap akurat.
        binding.imgCompassDial.rotation = -currentAzimuth
        // Jarum kiblat independen dari dial - selalu menunjuk arah kiblat relatif ke layar.
        binding.imgCompassNeedle.rotation = qiblaAngle - currentAzimuth

        checkQiblaAlignment()
    }

    private fun unwrapAngle(newAngle: Float, prevAngle: Float): Float {
        var delta = newAngle - prevAngle
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360
        return prevAngle + delta
    }

    private fun checkQiblaAlignment() {
        val accentColor = ContextCompat.getColor(this, R.color.accent_yellow)

        if (isAlignedToQibla()) {
            binding.txtQiblaValue.setTextColor(accentColor)
        } else {
            binding.txtQiblaValue.setTextColor(Color.WHITE)
        }
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