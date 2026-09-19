package site.elahady.alkaukaba.ui.konfigurasi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import site.elahady.alkaukaba.databinding.ActivityPilihLokasiPetaBinding
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import java.io.File
import java.util.Locale

/** Pilih koordinat lokasi manual dengan pin di tengah peta OpenStreetMap: user menggeser/zoom
 * peta sampai pin tepat di titik yang dimau, lalu konfirmasi. Dipakai dari sheet Lokasi di
 * [KonfigurasiActivity] - hasilnya (lat/lon) dikembalikan lewat result Intent, bukan disimpan
 * langsung, supaya user tetap bisa cek/edit angkanya di sheet sebelum menekan Simpan. */
class PilihLokasiPetaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPilihLokasiPetaBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            moveToLastKnownLocation(showFailureToast = true)
        } else {
            Toast.makeText(this, "Izin lokasi diperlukan untuk menuju lokasi Anda", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureOsmdroid()

        binding = ActivityPilihLokasiPetaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        applyStatusBarIconsForTheme()
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.includeToolbar.tvToolbarTitle.text = "Pilih Lokasi di Peta"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupMap(savedInstanceState)

        binding.btnMyLocation.setOnClickListener { moveToLastKnownLocation(showFailureToast = true) }
        binding.btnConfirmLocation.setOnClickListener { confirmSelection() }
    }

    private fun configureOsmdroid() {
        // Cache tile ditaruh di cacheDir (bukan storage eksternal default osmdroid) supaya tidak
        // butuh izin storage dan ikut terhapus saat user "Hapus cache". User-Agent wajib diisi -
        // server tile OSM menolak request dengan UA default library.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            val base = File(cacheDir, "osmdroid")
            osmdroidBasePath = base
            osmdroidTileCache = File(base, "tiles")
        }
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        val map = binding.mapView
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        map.minZoomLevel = MIN_ZOOM
        map.maxZoomLevel = MAX_ZOOM

        val savedLat = savedInstanceState?.getDouble(STATE_LAT, Double.NaN) ?: Double.NaN
        val savedLng = savedInstanceState?.getDouble(STATE_LNG, Double.NaN) ?: Double.NaN
        val initialLat = intent.getDoubleExtra(EXTRA_LAT, Double.NaN)
        val initialLng = intent.getDoubleExtra(EXTRA_LNG, Double.NaN)

        val startPoint: GeoPoint
        val startZoom: Double
        var followUserLocation = false
        when {
            isValidCoordinate(savedLat, savedLng) -> {
                startPoint = GeoPoint(savedLat, savedLng)
                startZoom = savedInstanceState!!.getDouble(STATE_ZOOM, ZOOM_DETAIL)
            }
            isValidCoordinate(initialLat, initialLng) -> {
                startPoint = GeoPoint(initialLat, initialLng)
                startZoom = ZOOM_DETAIL
            }
            else -> {
                startPoint = GeoPoint(DEFAULT_LAT, DEFAULT_LNG)
                startZoom = ZOOM_COUNTRY
                // Tanpa titik awal: kalau izin lokasi sudah pernah diberikan, mulai dari posisi
                // user. Sengaja tidak meminta izin di sini - izin baru diminta saat user tap
                // tombol "Ke lokasi saya".
                followUserLocation = hasLocationPermission()
            }
        }

        // Zoom + pusat peta sengaja diterapkan SETELAH layout pertama, bukan langsung di sini:
        // osmdroid menyimpan pusat sebagai posisi scroll piksel, jadi setCenter yang jatuh
        // sebelum MapView punya ukuran (terjadi pada cold start yang lambat) bergeser sebesar
        // setengah ukuran view begitu layout selesai (pin meleset ~1 km pada zoom 16).
        map.addOnFirstLayoutListener { _, _, _, _, _ ->
            map.controller.setZoom(startZoom)
            map.controller.setCenter(startPoint)
            updateCoordinatesLabel()
            if (followUserLocation) moveToLastKnownLocation(showFailureToast = false)
        }

        map.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                updateCoordinatesLabel()
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean = false
        })
    }

    private fun currentCenter(): Pair<Double, Double> {
        val center = binding.mapView.mapCenter
        // Peta osmdroid berulang horizontal, jadi bujur bisa keluar dari -180..180 kalau digeser
        // memutari bumi - normalisasi supaya lolos validasi rentang di sheet Lokasi.
        val lng = ((center.longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
        return roundCoordinate(center.latitude) to roundCoordinate(lng)
    }

    private fun updateCoordinatesLabel() {
        val (lat, lng) = currentCenter()
        binding.tvCoordinates.text = String.format(Locale.US, "%.5f, %.5f", lat, lng)
    }

    private fun confirmSelection() {
        val (lat, lng) = currentCenter()
        setResult(RESULT_OK, Intent().putExtra(EXTRA_LAT, lat).putExtra(EXTRA_LNG, lng))
        finish()
    }

    private fun hasLocationPermission() =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun moveToLastKnownLocation(showFailureToast: Boolean) {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                binding.mapView.controller.setZoom(ZOOM_DETAIL)
                binding.mapView.controller.animateTo(GeoPoint(location.latitude, location.longitude))
            } else if (showFailureToast) {
                Toast.makeText(this, "Lokasi GPS tidak ditemukan, coba lagi", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            if (showFailureToast) {
                Toast.makeText(this, "Gagal mengambil lokasi GPS", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val center = binding.mapView.mapCenter
        outState.putDouble(STATE_LAT, center.latitude)
        outState.putDouble(STATE_LNG, center.longitude)
        outState.putDouble(STATE_ZOOM, binding.mapView.zoomLevelDouble)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        binding.mapView.onDetach()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"

        private const val STATE_LAT = "state_lat"
        private const val STATE_LNG = "state_lng"
        private const val STATE_ZOOM = "state_zoom"

        // Titik tengah Indonesia + zoom tingkat negara, dipakai kalau belum ada koordinat awal.
        private const val DEFAULT_LAT = -2.5
        private const val DEFAULT_LNG = 118.0
        private const val ZOOM_COUNTRY = 5.0
        private const val ZOOM_DETAIL = 16.0
        private const val MIN_ZOOM = 3.0
        private const val MAX_ZOOM = 19.0

        /** [lat]/[lng] dipakai sebagai titik awal peta; kirim null kalau belum ada. */
        fun newIntent(context: Context, lat: Double?, lng: Double?): Intent =
            Intent(context, PilihLokasiPetaActivity::class.java).apply {
                if (lat != null && lng != null) {
                    putExtra(EXTRA_LAT, lat)
                    putExtra(EXTRA_LNG, lng)
                }
            }

        private fun isValidCoordinate(lat: Double, lng: Double) =
            !lat.isNaN() && !lng.isNaN() && lat in -90.0..90.0 && lng in -180.0..180.0

        private fun roundCoordinate(value: Double) = Math.round(value * 1_000_000.0) / 1_000_000.0
    }
}
