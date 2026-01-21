package Site.elahady.alkaukaba.ui.waktusholat

import PrayerRepository
import Site.elahady.alkaukaba.R
import Site.elahady.alkaukaba.api.RetrofitClient
import Site.elahady.alkaukaba.api.TimingPrayers
import Site.elahady.alkaukaba.api.Timings
import Site.elahady.alkaukaba.databinding.ActivityWaktuSholatBinding
import Site.elahady.alkaukaba.viewmodel.waktusholat.PrayerTimesViewModel
import Site.elahady.alkaukaba.viewmodel.waktusholat.PrayerViewModelFactory
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
        val repository = PrayerRepository(apiService)
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
            } // Atau warna abu gelap #333

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
            // Di sini kita kosongkan atau beri placeholder
            ""
        }

        // Set ke TextView (Format: 11 Rajab 1446H | 11 Januari 2025)
        binding.tvDate.text = if (hijriString.isNotEmpty()) "$hijriString | $masehiString" else masehiString
    }

    // 2. Logika Mencari Jadwal Sholat Berikutnya (Next Prayer)
    @SuppressLint("SetTextI18n")
    private fun updateNextPrayerUI(timings: TimingPrayers) {
        val prayerMap = mapOf(
            "Subuh" to timings.subuh,
            "Dzuhur" to timings.dzuhur,
            "Ashar" to timings.ashar,
            "Maghrib" to timings.maghrib,
            "Isya" to timings.isya
        )

        val nextPrayer = getNextPrayerTime(prayerMap)

        // Update UI
        // Format Teks: "Dzuhur 11:39 WIB"
        // Anda bisa menambahkan "WIB" secara hardcode atau ambil dari timezone
        binding.tvNextPrayer.text = "${nextPrayer.first} ${nextPrayer.second} WIB"
    }

    private fun getNextPrayerTime(prayers: Map<String, String>): Pair<String, String> {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val currentTimeString = sdf.format(java.util.Date())

        // Ubah waktu sekarang ke menit agar mudah dibandingkan (Jam * 60 + Menit)
        val currentMinutes = timeToMinutes(currentTimeString)

        var nearestPrayerName = "Subuh" // Default jika semua lewat (berarti besok Subuh)
        var nearestPrayerTime = prayers["Subuh"] ?: "00:00"
        var minDiff = Int.MAX_VALUE

        // Loop semua jadwal untuk mencari yang belum lewat
        for ((name, timeStr) in prayers) {
            // Bersihkan format jam (kadang API return "04:12 (WIB)") -> ambil 5 char pertama
            val cleanTime = timeStr.take(5)
            val prayerMinutes = timeToMinutes(cleanTime)

            if (prayerMinutes > currentMinutes) {
                val diff = prayerMinutes - currentMinutes
                // Cari selisih terkecil (waktu terdekat yang akan datang)
                if (diff < minDiff) {
                    minDiff = diff
                    nearestPrayerName = name
                    nearestPrayerTime = cleanTime
                }
            }
        }

        // Jika minDiff masih MAX_VALUE, berarti sekarang sudah malam (setelah Isya)
        // Maka waktu sholat berikutnya adalah Subuh (besok)
        if (minDiff == Int.MAX_VALUE) {
            nearestPrayerName = "Subuh"
            nearestPrayerTime = prayers["Subuh"]?.take(5) ?: "04:00"
        }

        return Pair(nearestPrayerName, nearestPrayerTime)
    }

    private fun timeToMinutes(time: String): Int {
        return try {
            val parts = time.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            (hour * 60) + minute
        } catch (e: Exception) {
            0
        }
    }

    private fun observeViewModel() {
        // 1. Observe Jadwal Sholat
        viewModel.prayerTimings.observe(this) { timings ->
            timings?.let {
                // Binding ke layout 'include' sangat mudah:
                // binding.ID_INCLUDE.ID_TEXTVIEW_DI_DALAM_INCLUDE

                binding.rowImsak.tvPrayerName.text = "Imsak"
                binding.rowImsak.tvTime.text = it.imsak

                binding.rowSubuh.tvPrayerName.text = "Subuh"
                binding.rowSubuh.tvTime.text = it.subuh

                binding.rowDzuhur.tvPrayerName.text = "Dzuhur"
                binding.rowDzuhur.tvTime.text = it.dzuhur

                binding.rowAshar.tvPrayerName.text = "Ashar"
                binding.rowAshar.tvTime.text = it.ashar

                binding.rowMaghrib.tvPrayerName.text = "Maghrib"
                binding.rowMaghrib.tvTime.text = it.maghrib

                binding.rowIsya.tvPrayerName.text = "Isya"
                binding.rowIsya.tvTime.text = it.isya
                updateNextPrayerUI(it)
            }
        }

        // 2. Observe Detail Rumus Kiblat
        viewModel.qiblaDetailText.observe(this) { detailText ->
            binding.tvCalculationResult.text = detailText
        }

        // 3. Observe Hasil Sudut (Kotak)
        viewModel.qiblaDegreeUI.observe(this) { degreeText ->
            binding.tvResultDegree.text = degreeText
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
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            // Default Jakarta jika null (misal di emulator belum set lokasi)
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