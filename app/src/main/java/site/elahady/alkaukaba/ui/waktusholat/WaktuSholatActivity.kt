package site.elahady.alkaukaba.ui.waktusholat

import site.elahady.alkaukaba.repo.PrayerRepository
import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.api.RetrofitClient
import site.elahady.alkaukaba.databinding.ActivityWaktuSholatBinding
import site.elahady.alkaukaba.viewmodel.waktusholat.PrayerKind
import site.elahady.alkaukaba.viewmodel.waktusholat.PrayerScheduleUiState
import site.elahady.alkaukaba.viewmodel.waktusholat.PrayerTimesViewModel
import site.elahady.alkaukaba.viewmodel.waktusholat.PrayerViewModelFactory
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class WaktuSholatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWaktuSholatBinding
    private lateinit var viewModel: PrayerTimesViewModel

    private lateinit var fusedLocationClient: FusedLocationProviderClient
        private val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (permissions.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false)
                ) {
                    getLocation()
                } else {
                    useDefaultLocation()
                }
            } else {
                // Fallback simpel untuk Android < N
                useDefaultLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup ViewBinding
        binding = ActivityWaktuSholatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupUI()
        setupViewModel()
        observeViewModel()
        checkLocationPermission()
        updateDateDisplay()
    }

    private fun setupViewModel() {
        val apiService = RetrofitClient.instance
        val repository = PrayerRepository(apiService, applicationContext)
        val factory = PrayerViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[PrayerTimesViewModel::class.java]
    }

    private fun setupUI() {
        // Set kondisi awal: Tab 'Waktu Aktual' aktif
        updateTabState(isActual = true)

        // Listener Klik Tab Kiri
        binding.btnTabActual.setOnClickListener {
            updateTabState(isActual = true)
        }

        // Listener Klik Tab Kanan
        binding.btnTabDetail.setOnClickListener {
            updateTabState(isActual = false)
        }
        binding.btnTabDetail.visibility = View.GONE
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun updateTabState(isActual: Boolean) {
        if (isActual) {
            // --- KONDISI: WAKTU AKTUAL AKTIF ---

            // 1. Ubah Style Tombol Kiri (Aktif)
            binding.btnTabActual.setBackgroundResource(R.drawable.bg_tab_active)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                binding.btnTabActual.setTextColor(getColor(android.R.color.white))
            }

            // 2. Ubah Style Tombol Kanan (Non-Aktif)
            binding.btnTabDetail.setBackgroundResource(R.drawable.bg_tab_inactive)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                binding.btnTabDetail.setTextColor(getColor(R.color.black))
            }

            // 3. Tampilkan Layout yang sesuai
            binding.layoutWaktuSholat.visibility = View.VISIBLE
            binding.layoutDetailKiblat.visibility = View.GONE

        } else {
            // --- KONDISI: DETAIL PERHITUNGAN AKTIF ---

            // 1. Ubah Style Tombol Kiri (Non-Aktif)
            binding.btnTabActual.setBackgroundResource(R.drawable.bg_tab_inactive)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                binding.btnTabActual.setTextColor(getColor(R.color.black))
            }

            // 2. Ubah Style Tombol Kanan (Aktif)
            binding.btnTabDetail.setBackgroundResource(R.drawable.bg_tab_active)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                binding.btnTabDetail.setTextColor(getColor(android.R.color.white))
            }

            // 3. Tampilkan Layout yang sesuai
            binding.layoutWaktuSholat.visibility = View.GONE
            binding.layoutDetailKiblat.visibility = View.VISIBLE
        }
    }

    // 1. Menampilkan Tanggal Masehi & Hijriyah
    @SuppressLint("NewApi") // HijrahDate butuh min API 26 (Android 8.0)
    private fun updateDateDisplay() {
        // A. Tanggal Masehi (Gregorian)
        val masehiFormat = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID"))
        val dateNow = java.util.Date()
        val masehiString = masehiFormat.format(dateNow)

        // B. Tanggal Hijriyah
        // Opsi 1: Menggunakan java.time.chrono.HijrahDate (Android 8.0+)
        val hijriString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val hijrahDate = java.time.chrono.HijrahDate.now()
                val hijriFormatter = java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale("id", "ID"))
                "${hijrahDate.format(hijriFormatter)}H"
            } catch (e: Exception) {
                "Hijriyah Unavail" // Fallback jika device tidak support
            }
        } else {
            // Untuk Android di bawah 8.0, idealnya ambil dari response API Aladhan (meta.date)
            ""
        }

        // Set ke TextView (Format: 11 Rajab 1446H | 11 Januari 2025)
        binding.tvDate.text = if (hijriString.isNotEmpty()) "$hijriString | $masehiString" else masehiString
    }

    // 2. Bind 8 baris waktu (Tsulutsul Lail Akhir s/d Isya) & tandai periode yang sedang aktif.
    // "Sholat mana yang aktif" sudah dihitung di PrayerTimesViewModel — di sini murni binding ke View.
    @SuppressLint("SetTextI18n")
    private fun updateNextPrayerUI(state: PrayerScheduleUiState) {
        val rows = listOf(
            binding.rowTsulutsulLail, binding.rowImsak, binding.rowSubuh, binding.rowDhuha,
            binding.rowDzuhur, binding.rowAshar, binding.rowMaghrib, binding.rowIsya
        )

        val colorActiveBg = ContextCompat.getColor(this, R.color.waktu_sholat_row_active_bg)
        val colorDarkBg = ContextCompat.getColor(this, R.color.waktu_sholat_dark_bg)
        val colorIconInactiveBg = ContextCompat.getColor(this, R.color.waktu_sholat_icon_bg_inactive)
        val colorIconMuted = ContextCompat.getColor(this, R.color.waktu_sholat_icon_muted)
        val colorWhite = ContextCompat.getColor(this, android.R.color.white)
        val colorTransparent = ContextCompat.getColor(this, R.color.transparent)
        val colorNameInactive = android.graphics.Color.parseColor("#374151")

        state.items.forEachIndexed { index, entry ->
            val row = rows[index]
            row.tvPrayerName.text = entry.label
            row.tvTime.text = entry.time
            row.ivIcon.setImageResource(iconFor(entry.kind))

            val isActive = index == state.activeIndex
            row.rowRoot.backgroundTintList = android.content.res.ColorStateList.valueOf(if (isActive) colorActiveBg else colorTransparent)
            row.iconContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(if (isActive) colorDarkBg else colorIconInactiveBg)
            row.ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(if (isActive) colorWhite else colorIconMuted)
            row.tvPrayerName.setTypeface(null, if (isActive) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            row.tvTime.setTypeface(null, if (isActive) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            row.tvPrayerName.setTextColor(if (isActive) colorDarkBg else colorNameInactive)
        }

        binding.tvNextPrayer.text = state.nextPrayerLabel
        binding.tvNextPrayerTime.text = "${state.nextPrayerTime} WIB"
    }

    private fun iconFor(kind: PrayerKind): Int = when (kind) {
        PrayerKind.TSULUTSUL_LAIL -> R.drawable.ic_prayer_tsulutsul_lail
        PrayerKind.IMSAK -> R.drawable.ic_prayer_imsak
        PrayerKind.SUBUH -> R.drawable.ic_prayer_subuh
        PrayerKind.DHUHA -> R.drawable.ic_prayer_dhuha
        PrayerKind.DZUHUR -> R.drawable.ic_prayer_dzuhur
        PrayerKind.ASHAR -> R.drawable.ic_prayer_ashar
        PrayerKind.MAGHRIB -> R.drawable.ic_prayer_maghrib
        PrayerKind.ISYA -> R.drawable.ic_prayer_isya
    }

    private fun observeViewModel() {
        // 1. Observe Jadwal Sholat
        viewModel.prayerSchedule.observe(this) { state ->
            state?.let { updateNextPrayerUI(it) }
        }

        // 2. Observe Detail Rumus Kiblat
        viewModel.qiblaDetailText.observe(this) { detailText ->
            binding.tvCalculationResult.text = detailText
        }

        // 3. Observe Hasil Sudut (Kotak)
        viewModel.qiblaDegreeUI.observe(this) { degreeText ->
            binding.tvResultDegree.text = degreeText
        }

        viewModel.prayerCalcDetailText.observe(this) { detailText ->
            binding.tvPrayerCalculationDetail.text = detailText
        }

        // 4. Observe Loading/Error
        viewModel.errorMessage.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }


    }

    private fun checkLocationPermission() {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                locationPermissionRequest.launch(arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            } else {
                // Jika sudah diizinkan, langsung ambil lokasi
                getLocation()
            }
        }

    @SuppressLint("SetTextI18n")
    private fun getLocation() {
        binding.tvLocationName.text = "Sedang mencari lokasi..."

        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val lat = location?.latitude ?: -6.2088
            val long = location?.longitude ?: 106.8456

            // Update UI Lokasi
            binding.tvLocationName.text = "Lat: $lat, Long: $long"
            binding.etCoordinates.setText("$lat, $long")

            // PENTING: Panggil ViewModel untuk memproses data
            viewModel.loadData(lat, long)
        }
    }

    private fun useDefaultLocation() {
        Toast.makeText(this, "Izin lokasi ditolak, menggunakan default Jakarta", Toast.LENGTH_SHORT).show()
    }
}