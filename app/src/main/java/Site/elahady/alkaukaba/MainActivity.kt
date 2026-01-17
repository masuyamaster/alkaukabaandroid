package Site.elahady.alkaukaba

import PrayerRepository
import Site.elahady.alkaukaba.adapter.CalendarAdapter
import Site.elahady.alkaukaba.adapter.HolidayAdapter
import Site.elahady.alkaukaba.api.RetrofitClient
import Site.elahady.alkaukaba.ui.arahkiblat.KiblatActivity
import Site.elahady.alkaukaba.databinding.ActivityMainBinding
import Site.elahady.alkaukaba.ui.calendar.CalendarActivity
import Site.elahady.alkaukaba.utils.Resource
import Site.elahady.alkaukaba.viewmodel.MainViewModel
import Site.elahady.alkaukaba.viewmodel.MainViewModelFactory
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var holidayAdapter: HolidayAdapter
    private lateinit var calendarAdapter: CalendarAdapter
    var latitude  = 0.0
    var longitude  = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupHeaderDate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkLocationPermission()
        setupObservers()
        setupNavigation()
        setupHolidayPreview()
        setupMonthlyCalendar()
        setupCalendarNavigation()

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
    private fun checkLocationPermission() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                        permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
                } else {
                    TODO("VERSION.SDK_INT < N")
                }
            ) {
                getUserLocation()
            } else {
                Toast.makeText(this, "Izin lokasi ditolak, menggunakan default Jakarta", Toast.LENGTH_SHORT).show()
                // Fallback Logic: Panggil ViewModel dengan koordinat Jakarta
                viewModel.fetchPrayerData(-6.2088, 106.8456)
                viewModel.fetchUpcomingIslamicHolidays(-6.2088, 106.8456)
                viewModel.initCalendar(-6.2088, 106.8456)
                latitude = -6.2088
                longitude = 106.8456
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
                viewModel.fetchUpcomingIslamicHolidays(location.latitude, location.longitude)
                viewModel.initCalendar(location.latitude, location.longitude)
                latitude = location.latitude
                longitude =location.longitude

                // Beri tahu ViewModel untuk cari nama jalan (Geocoder)
                val geocoder = Geocoder(this, Locale("id", "ID"))
                viewModel.fetchAddressName(geocoder, location.latitude, location.longitude)
            }
        }
    }

    private fun setupNavigation() {
        binding.btKiblat.setOnClickListener {
            val intentKiblat = Intent(this@MainActivity, KiblatActivity::class.java)
            startActivity(intentKiblat)
        }
        binding.tvLabelCalendar.setOnClickListener {
            openCalendarPage()
        }
        binding.tvLabelDetailCalendar.setOnClickListener {
            openCalendarPage()
        }
    }

    private fun openCalendarPage() {
        val intent = Intent(this, CalendarActivity::class.java)
        intent.putExtra("LATITUDE", latitude)
        intent.putExtra("LONGITUDE", longitude)
        startActivity(intent)
    }

    private fun setupHolidayPreview() {
        holidayAdapter = HolidayAdapter()
        binding.rvHolidayPreview.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = holidayAdapter
            isNestedScrollingEnabled = false
        }
        viewModel.holidayPreview.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    resource.data?.let { items ->
                        holidayAdapter.setData(items)
                    }
                }
                is Resource.Error -> {
                    println("error :: " + resource.message)
                }
                is Resource.Loading -> {
                }
            }
        }

        binding.btnSeeAllHolidays.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupMonthlyCalendar() {
        calendarAdapter = CalendarAdapter()
        binding.rvWeeklyCalendar.apply { // ID layout tetap bisa rvWeeklyCalendar atau ganti
            layoutManager = GridLayoutManager(this@MainActivity, 7) // Grid 7 Kolom
            adapter = calendarAdapter
            isNestedScrollingEnabled = false
        }

        // 1. Observe Judul Hijriah
        viewModel.hijriTitle.observe(this) { hijriText ->
            binding.tvHijriMonthYear.text = hijriText
        }

        // 2. Observe Data Kalender & Loading State
        viewModel.calendarData.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Tampilkan Loading, Sembunyikan Grid
                    binding.progressCalendar.visibility = View.VISIBLE
                    binding.rvWeeklyCalendar.visibility = View.INVISIBLE
                }
                is Resource.Success -> {
                    binding.progressCalendar.visibility = View.GONE
                    binding.rvWeeklyCalendar.visibility = View.VISIBLE
                    resource.data?.let { calendarAdapter.setData(it) }
                }
                is Resource.Error -> {
                    binding.progressCalendar.visibility = View.GONE
                    binding.rvWeeklyCalendar.visibility = View.VISIBLE
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observe Data Grid
        viewModel.calendarData.observe(this) { resource ->
            if (resource is Resource.Success) {
                resource.data?.let { calendarAdapter.setData(it) }
            }
        }

        // Observe Judul Bulan & Tahun
        viewModel.monthYearTitle.observe(this) { title ->
            // Pecah string "Januari 2026" jika view dipisah
            val parts = title.split(" ")
            if (parts.size >= 2) {
                binding.tvMonth.text = parts[0]
                binding.tvYear.text = parts[1]
            } else {
                binding.tvMonth.text = title
            }
        }
    }

    private fun setupCalendarNavigation() {
        binding.btnPrevMonth.setOnClickListener {
            viewModel.changeMonth(-1) // Mundur 1 bulan
        }
        binding.btnNextMonth.setOnClickListener {
            viewModel.changeMonth(1) // Maju 1 bulan
        }
    }
}