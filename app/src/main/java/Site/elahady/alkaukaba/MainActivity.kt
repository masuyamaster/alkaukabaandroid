package Site.elahady.alkaukaba

import Site.elahady.alkaukaba.arahkiblat.ArahKiblatActivity
import Site.elahady.alkaukaba.databinding.ActivityMainBinding
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Format Tanggal
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initHeaderData()
        navigation()
        checkLocationPermission()

        // Memulai simulasi update data sholat
        updatePrayerCardInfo()
    }

    private fun initHeaderData() {
        // 1. Set Tanggal Hari Ini di Header Card
        val today = Date()
        // Asumsi TextView tanggal ada di dalam cardPrayer (sesuai XML Anda, TextView kedua di constraint layout)
        // Karena tidak ada ID di XML untuk tanggal, Anda sebaiknya menambahkan ID: android:id="@+id/tvDateToday" di XML.
        // Namun, jika menggunakan binding child, bisa diakses via struktur view jika ID belum ada,
        // tapi disini saya asumsikan Anda menambahkan ID 'tvDateToday' pada TextView "02 Jan 2024".

        // binding.cardPrayer.tvDateToday.text = dateFormat.format(today)
        // *Catatan: Tambahkan ID di XML agar baris di atas bekerja.
    }

    private fun checkLocationPermission() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                when {
                    permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                        getLocation()
                    }
                    else -> {
                        Toast.makeText(this, "Izin lokasi diperlukan untuk jadwal sholat", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        } else {
            getLocation()
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Menggunakan Geocoder untuk mendapatkan nama lokasi
                try {
                    val geocoder = Geocoder(this, Locale("id", "ID"))
                    // Versi Android baru memerlukan listener, ini cara simpel untuk kompatibilitas umum
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val city = address.subAdminArea ?: address.locality
                        val country = address.countryName
                        // Update UI Lokasi (TextView sebelah iconLoc)
                        // Karena di XML TextView lokasi tidak punya ID, tambahkan ID: android:id="@+id/tvUserLocation"
                        // binding.tvUserLocation.text = "$city - $country"

                        // Alternatif akses via hierarchy jika malas tambah ID (kurang disarankan):
                        // (binding.cardPrayer.getChildAt(0) as ConstraintLayout)...
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun updatePrayerCardInfo() {
        // 2. Logika Jadwal Sholat & Waktu Terlewat
        // *PENTING: Di aplikasi real, Anda harus menggunakan Library perhitungan sholat (seperti Adhan Java)
        // Di sini kita buat simulasi jadwal statis untuk demo logika UI.

        val calendar = Calendar.getInstance()
        val now = calendar.time

        // Simulasi Jadwal Sholat Hari Ini (Ganti ini dengan kalkulasi real nanti)
        val jadwalSholat = mapOf(
            "Subuh" to getTimeToday(4, 30),
            "Dzuhur" to getTimeToday(11, 45),
            "Ashar" to getTimeToday(15, 0),
            "Maghrib" to getTimeToday(17, 45),
            "Isya" to getTimeToday(19, 0)
        )

        // Cari waktu sholat yang paling dekat (yang baru saja lewat atau yang akan datang)
        var nearestPrayerName = ""
        var nearestPrayerTime: Date? = null
        var isPassed = false

        // Logika sederhana: Cari sholat terakhir yang sudah lewat
        val sortedPrayers = jadwalSholat.toList().sortedBy { it.second }

        for (prayer in sortedPrayers) {
            if (now.after(prayer.second)) {
                nearestPrayerName = prayer.first
                nearestPrayerTime = prayer.second
                isPassed = true
            }
        }

        // Jika belum ada yang lewat (misal jam 2 pagi, ambil Isya kemarin atau Subuh nanti),
        // untuk simpelnya kita ambil Subuh hari ini sebagai "Upcoming"
        if (nearestPrayerTime == null) {
            nearestPrayerName = "Subuh"
            nearestPrayerTime = jadwalSholat["Subuh"]
            isPassed = false
        }

        // Update UI
        binding.tvPrayerName.text = "Waktu $nearestPrayerName"

        if (nearestPrayerTime != null) {
            val diff = abs(now.time - nearestPrayerTime.time)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)

            if (isPassed) {
                binding.tvLabelUpcoming.text = "Sholat Terakhir" // Label di atas
                val timeString = if (hours > 0) "$hours jam $minutes menit" else "$minutes menit"
                binding.tvStatus.text = "Sudah lewat +$timeString yang lalu"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                }
            } else {
                binding.tvLabelUpcoming.text = "Sholat Berikutnya"
                val timeString = if (hours > 0) "$hours jam $minutes menit" else "$minutes menit"
                binding.tvStatus.text = "Akan datang dalam $timeString"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                }
            }
        }

        // 4. Kalender & Hari Besar (Logika Sederhana)
        checkHolidays(now)
    }

    private fun checkHolidays(date: Date) {
        // Format tanggal untuk pencocokan: "dd-MM"
        val dayMonth = SimpleDateFormat("dd-MM", Locale.getDefault()).format(date)

        // Database hari libur sederhana
        val holidays = mapOf(
            "17-08" to "Hari Kemerdekaan RI",
            "01-01" to "Tahun Baru Masehi",
            "25-12" to "Hari Raya Natal",
            // Tambahkan konversi Hijriyah ke Masehi dinamis untuk Idul Fitri/Adha
        )

        val holidayName = holidays[dayMonth]
        if (holidayName != null) {
            // Tampilkan peringatan hari besar
            // Anda bisa menggunakan Toast, Snackbar, atau mengubah text di widget kalender
            Toast.makeText(this, "Hari ini: $holidayName", Toast.LENGTH_LONG).show()

            // Contoh mengubah warna teks bulan jika libur (opsional)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                binding.tvMonth.setTextColor(getColor(android.R.color.holo_red_light))
            }
        }
    }

    // Helper untuk membuat objek Date hari ini dengan jam tertentu
    private fun getTimeToday(hour: Int, minute: Int): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        return cal.time
    }

    private fun navigation(){
        binding.btKiblat.setOnClickListener {
            val intent = Intent(this, ArahKiblatActivity::class.java)
            startActivity(intent)
        }

        // Tambahkan navigasi tombol lain disini jika sudah ada activity-nya
        binding.btSholat.setOnClickListener {
            Toast.makeText(this, "Menu Waktu Sholat", Toast.LENGTH_SHORT).show()
        }
        // ... dst
    }
}