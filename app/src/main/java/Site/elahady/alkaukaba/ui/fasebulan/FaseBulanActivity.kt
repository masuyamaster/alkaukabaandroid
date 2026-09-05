package site.elahady.alkaukaba.ui.fasebulan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.MoonQuarterInfo
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.KM_PER_AU
import io.github.cosinekitty.astronomy.moonPhase
import io.github.cosinekitty.astronomy.nextMoonQuarter
import io.github.cosinekitty.astronomy.searchMoonQuarter
import io.github.cosinekitty.astronomy.searchRiseSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import site.elahady.alkaukaba.databinding.ActivityFaseBulanBinding
import site.elahady.alkaukaba.utils.MoonPhaseLabel
import site.elahady.alkaukaba.utils.SessionManager
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

class FaseBulanActivity : AppCompatActivity() {

    // Radius rata-rata Bulan (IAU mean radius) - konstanta fisik, tidak bergantung waktu/lokasi.
    private val moonMeanRadiusKm = 1737.4

    private lateinit var binding: ActivityFaseBulanBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaseBulanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        binding.includeToolbar.tvToolbarTitle.text = "Fase Bulan"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sessionManager = SessionManager(this)

        showCurrentPhase()
        loadUpcomingQuarters()
        checkLocationPermission()
    }

    private fun showCurrentPhase() {
        val now = Time.fromMillisecondsSince1970(System.currentTimeMillis())
        val synodicAngle = moonPhase(now)
        val illum = illumination(Body.Moon, now)
        val distanceKm = geoVector(Body.Moon, now, Aberration.Corrected).length() * KM_PER_AU
        binding.moonPhaseView.setPhase(synodicAngle, illum.phaseFraction)
        binding.tvMoonPhaseName.text = MoonPhaseLabel.forAngle(synodicAngle)
        binding.tvMoonIllumination.text = "%.0f%% permukaan tersinari".format(illum.phaseFraction * 100)
        binding.tvMoonMagnitude.text = "%.2f".format(illum.mag)
        binding.tvMoonDistance.text = "%,.0f km".format(distanceKm)
        binding.tvMoonRadius.text = "%,.1f km".format(moonMeanRadiusKm)
    }

    // --- Data yang bergantung lokasi observer: Az/Alt & waktu terbit/terbenam ---
    // RA/Dec/Magnitude/Distance di atas geosentris (sama untuk semua lokasi), tapi posisi di
    // langit (azimuth/altitude) dan jam terbit/terbenam Bulan berbeda per lokasi pengamat.

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkLocationPermission() {
        if (sessionManager.isManualLocationMode()) {
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

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            if (granted) {
                checkGpsEnabled()
            } else {
                binding.tvMoonAzAlt.text = "-"
                binding.tvMoonRiseSet.text = "-"
            }
        }

    private fun checkGpsEnabled() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            getLastLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocationReady(location.latitude, location.longitude)
            } else {
                requestNewLocation()
            }
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
            result.lastLocation?.let { onLocationReady(it.latitude, it.longitude) }
        }
    }

    private fun onLocationReady(lat: Double, lon: Double) {
        lifecycleScope.launch(Dispatchers.Default) {
            val observer = Observer(lat, lon, 0.0)
            val now = Time.fromMillisecondsSince1970(System.currentTimeMillis())
            val eq = equator(Body.Moon, now, observer, EquatorEpoch.OfDate, Aberration.Corrected)
            val hor = horizon(now, observer, eq.ra, eq.dec, Refraction.Normal)
            val moonrise = searchRiseSet(Body.Moon, observer, Direction.Rise, now, 1.2)
            val moonset = searchRiseSet(Body.Moon, observer, Direction.Set, now, 1.2)

            withContext(Dispatchers.Main) {
                if (isFinishing) return@withContext
                val timeFormat = SimpleDateFormat("HH:mm", Locale("in", "ID"))
                binding.tvMoonRaDec.text = "${formatRaHours(eq.ra)} / ${formatDegreesDms(eq.dec)}"
                binding.tvMoonAzAlt.text = "${formatDegreesDms(hor.azimuth)} / ${formatDegreesDms(hor.altitude)}"
                val riseText = moonrise?.let { timeFormat.format(Date(it.toMillisecondsSince1970())) } ?: "-"
                val setText = moonset?.let { timeFormat.format(Date(it.toMillisecondsSince1970())) } ?: "-"
                binding.tvMoonRiseSet.text = "Terbit $riseText — Terbenam $setText"
            }
        }
    }

    /** Format sudut derajat (bisa negatif, mis. deklinasi/altitude) ke DD°MM'SS.S". */
    private fun formatDegreesDms(decimalDegrees: Double): String {
        val sign = if (decimalDegrees < 0) "-" else ""
        val abs = kotlin.math.abs(decimalDegrees)
        val degrees = floor(abs).toInt()
        val minutesDecimal = (abs - degrees) * 60.0
        val minutes = floor(minutesDecimal).toInt()
        val seconds = (minutesDecimal - minutes) * 60.0
        return "%s%d°%02d'%04.1f\"".format(sign, degrees, minutes, seconds)
    }

    /** Format asensiorekta (jam sideris, 0-24) ke HHhMMmSS.Ss. */
    private fun formatRaHours(hours: Double): String {
        val normalized = ((hours % 24.0) + 24.0) % 24.0
        val h = floor(normalized).toInt()
        val minutesDecimal = (normalized - h) * 60.0
        val m = floor(minutesDecimal).toInt()
        val s = (minutesDecimal - m) * 60.0
        return "%02dh%02dm%04.1fs".format(h, m, s)
    }

    private fun loadUpcomingQuarters() {
        lifecycleScope.launch(Dispatchers.Default) {
            val now = Time.fromMillisecondsSince1970(System.currentTimeMillis())
            var mq = searchMoonQuarter(now)
            val quarters = mutableListOf(mq)
            repeat(3) {
                mq = nextMoonQuarter(mq)
                quarters.add(mq)
            }
            withContext(Dispatchers.Main) {
                bindQuarters(quarters)
            }
        }
    }

    private fun bindQuarters(quarters: List<MoonQuarterInfo>) {
        val format = SimpleDateFormat("EEEE, d MMMM yyyy HH:mm", Locale("in", "ID"))
        fun label(quarter: Int) = quarters.firstOrNull { it.quarter == quarter }
            ?.let { format.format(Date(it.time.toMillisecondsSince1970())) }
            ?: "-"

        binding.tvNextNewMoon.text = label(0)
        binding.tvNextFirstQuarter.text = label(1)
        binding.tvNextFullMoon.text = label(2)
        binding.tvNextLastQuarter.text = label(3)
    }
}
