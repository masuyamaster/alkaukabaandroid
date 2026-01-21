package Site.elahady.alkaukaba

import PrayerRepository
import Site.elahady.alkaukaba.adapter.CalendarAdapter
import Site.elahady.alkaukaba.adapter.HolidayAdapter
import Site.elahady.alkaukaba.api.RetrofitClient
import Site.elahady.alkaukaba.ui.arahkiblat.KiblatActivity
import Site.elahady.alkaukaba.databinding.ActivityMainBinding
import Site.elahady.alkaukaba.ui.calendar.CalendarActivity
import Site.elahady.alkaukaba.ui.waktusholat.WaktuSholatActivity
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
import androidx.core.view.WindowCompat
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

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
            ) {
                getUserLocation()
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        setupViewModel()
        setupHeaderDate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkLocationPermission() // Aman dipanggil di sini
        setupObservers()
        setupNavigation()
        setupHolidayPreview()
        setupMonthlyCalendar()
        setupCalendarNavigation()

        // Setup Swipe Refresh
        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
        }
    }

    private fun refreshData() {
        setupHeaderDate()
        checkLocationPermission() // Sekarang AMAN dipanggil berulang kali
    }

    private fun setupViewModel() {
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
                    binding.swipeRefresh.isRefreshing = false // Stop loading
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
                    binding.swipeRefresh.isRefreshing = false // Stop loading
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
            }
        }
    }

    private fun setupHeaderDate() {
        val dateFormatFull = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        binding.tvDateNow.text = dateFormatFull.format(Date())
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        } else {
            // Jika sudah diizinkan, langsung ambil lokasi
            getUserLocation()
        }
    }

    private fun useDefaultLocation() {
        Toast.makeText(this, "Izin lokasi ditolak, menggunakan default Jakarta", Toast.LENGTH_SHORT).show()
        fetchDataByCoordinate(-6.2088, 106.8456)
        binding.swipeRefresh.isRefreshing = false
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                fetchDataByCoordinate(location.latitude, location.longitude)

                val geocoder = Geocoder(this, Locale("id", "ID"))
                viewModel.fetchAddressName(geocoder, location.latitude, location.longitude)
            } else {
                Toast.makeText(this, "Lokasi tidak ditemukan, coba refresh", Toast.LENGTH_SHORT).show()
                binding.swipeRefresh.isRefreshing = false
            }
        }.addOnFailureListener {
            binding.swipeRefresh.isRefreshing = false
            Toast.makeText(this, "Gagal mendapatkan lokasi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchDataByCoordinate(lat: Double, lon: Double) {
        latitude = lat
        longitude = lon
        viewModel.fetchPrayerData(lat, lon)
        viewModel.fetchUpcomingIslamicHolidays(lat, lon)
        viewModel.initCalendar(lat, lon)
    }

    private fun setupNavigation() {
        binding.btSholat.setOnClickListener {
            val intentWaktuSholat = Intent(this@MainActivity, WaktuSholatActivity::class.java)
            startActivity(intentWaktuSholat)
        }
        binding.btKiblat.setOnClickListener {
            val intentKiblat = Intent(this@MainActivity, KiblatActivity::class.java)
            startActivity(intentKiblat)
        }
        binding.btSholat.setOnClickListener {
            val intentSholat = Intent(this@MainActivity, WaktuSholatActivity::class.java)
            startActivity(intentSholat)
        }
        binding.tvLabelCalendar.setOnClickListener { openCalendarPage() }
        binding.tvLabelDetailCalendar.setOnClickListener { openCalendarPage() }
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
                    resource.data?.let { items -> holidayAdapter.setData(items) }
                }
                is Resource.Error -> println("error :: " + resource.message)
                is Resource.Loading -> {}
            }
        }

        binding.btnSeeAllHolidays.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupMonthlyCalendar() {
        calendarAdapter = CalendarAdapter()
        binding.rvWeeklyCalendar.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 7)
            adapter = calendarAdapter
            isNestedScrollingEnabled = false
        }

        viewModel.hijriTitle.observe(this) { hijriText ->
            binding.tvHijriMonthYear.text = hijriText
        }

        viewModel.calendarData.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
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
                }
            }
        }

        viewModel.monthYearTitle.observe(this) { title ->
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
        binding.btnPrevMonth.setOnClickListener { viewModel.changeMonth(-1) }
        binding.btnNextMonth.setOnClickListener { viewModel.changeMonth(1) }
    }
}