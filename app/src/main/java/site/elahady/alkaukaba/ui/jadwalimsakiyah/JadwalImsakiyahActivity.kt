package site.elahady.alkaukaba.ui.jadwalimsakiyah

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.api.RetrofitClient
import site.elahady.alkaukaba.databinding.ActivityJadwalImsakiyahBinding
import site.elahady.alkaukaba.databinding.ItemImsakiyahDayCellBinding
import site.elahady.alkaukaba.databinding.ItemImsakiyahRowBinding
import site.elahady.alkaukaba.repo.PrayerRepository
import site.elahady.alkaukaba.utils.SessionManager
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.viewmodel.jadwalimsakiyah.ImsakiyahRow
import site.elahady.alkaukaba.viewmodel.jadwalimsakiyah.ImsakiyahUiState
import site.elahady.alkaukaba.viewmodel.jadwalimsakiyah.JadwalImsakiyahViewModel
import site.elahady.alkaukaba.viewmodel.jadwalimsakiyah.JadwalImsakiyahViewModelFactory

// Tabel jadwal imsakiyah 1 bulan Hijriyah: hari + semua waktu sholat yang ada di menu
// Waktu Sholat. Pola lokasi (GPS/manual/fallback) & wiring ViewModel dicopy dari
// WaktuSholatActivity supaya konsisten dengan fitur sejenis di app ini.
class JadwalImsakiyahActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJadwalImsakiyahBinding
    private lateinit var viewModel: JadwalImsakiyahViewModel
    private lateinit var sessionManager: SessionManager
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
            useDefaultLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityJadwalImsakiyahBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        applyStatusBarIconsForTheme()
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sessionManager = SessionManager(this)

        setupUI()
        setupViewModel()
        observeViewModel()
        checkLocationPermission()
    }

    private fun setupUI() {
        binding.includeToolbar.tvToolbarTitle.text = "Jadwal Imsakiyah"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnPrevMonth.setOnClickListener { viewModel.prevMonth() }
        binding.btnNextMonth.setOnClickListener { viewModel.nextMonth() }
    }

    private fun setupViewModel() {
        val apiService = RetrofitClient.instance
        val repository = PrayerRepository(apiService, applicationContext)
        val factory = JadwalImsakiyahViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[JadwalImsakiyahViewModel::class.java]
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state -> renderTable(state) }
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.errorMessage.observe(this) { message ->
            if (message.isNullOrEmpty()) {
                binding.tvError.visibility = View.GONE
            } else {
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = message
            }
        }
    }

    private fun renderTable(state: ImsakiyahUiState) {
        binding.tvMonthLabel.text = state.monthLabel
        binding.layoutImsakiyahDayColumn.removeAllViews()
        binding.layoutImsakiyahTable.removeAllViews()

        binding.layoutImsakiyahDayColumn.addView(buildDayCell("Tgl", isHeader = true))
        binding.layoutImsakiyahTable.addView(buildHeaderRow(state.columnLabels))
        state.rows.forEachIndexed { index, row ->
            binding.layoutImsakiyahDayColumn.addView(
                buildDayCell("${row.hijriDay}\n${row.gregorianLabel}", isHeader = false, rowIndex = index)
            )
            binding.layoutImsakiyahTable.addView(buildDataRow(row, index))
        }
    }

    // Kolom tanggal (freeze) - di luar HorizontalScrollView, dibangun terpisah dari kolom waktu
    // supaya hanya kolom 2 s/d terakhir yang ikut geser horizontal.
    private fun buildDayCell(text: String, isHeader: Boolean, rowIndex: Int = 0): View {
        val cellBinding = ItemImsakiyahDayCellBinding.inflate(layoutInflater, binding.layoutImsakiyahDayColumn, false)
        cellBinding.root.text = text
        if (isHeader) {
            cellBinding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_amber_light))
            cellBinding.root.setTextColor(ContextCompat.getColor(this, R.color.icon_amber))
            cellBinding.root.setTypeface(null, Typeface.BOLD)
        } else {
            val colorEven = ContextCompat.getColor(this, R.color.white)
            val colorOdd = ContextCompat.getColor(this, R.color.input_inline_bg)
            cellBinding.root.setBackgroundColor(if (rowIndex % 2 == 0) colorEven else colorOdd)
        }
        return cellBinding.root
    }

    private fun buildHeaderRow(columnLabels: List<String>): View {
        val rowBinding = ItemImsakiyahRowBinding.inflate(layoutInflater, binding.layoutImsakiyahTable, false)
        val colorAmberBg = ContextCompat.getColor(this, R.color.bg_amber_light)
        val colorAmberText = ContextCompat.getColor(this, R.color.icon_amber)

        rowBinding.rowRoot.setBackgroundColor(colorAmberBg)
        val timeViews = listOf(
            rowBinding.tvTime0, rowBinding.tvTime1, rowBinding.tvTime2, rowBinding.tvTime3,
            rowBinding.tvTime4, rowBinding.tvTime5, rowBinding.tvTime6, rowBinding.tvTime7
        )
        timeViews.forEachIndexed { index, tv -> tv.text = columnLabels[index] }

        timeViews.forEach { tv ->
            tv.setTextColor(colorAmberText)
            tv.setTypeface(null, Typeface.BOLD)
        }
        return rowBinding.root
    }

    private fun buildDataRow(row: ImsakiyahRow, index: Int): View {
        val rowBinding = ItemImsakiyahRowBinding.inflate(layoutInflater, binding.layoutImsakiyahTable, false)
        val colorEven = ContextCompat.getColor(this, R.color.white)
        val colorOdd = ContextCompat.getColor(this, R.color.input_inline_bg)

        rowBinding.rowRoot.setBackgroundColor(if (index % 2 == 0) colorEven else colorOdd)
        val timeViews = listOf(
            rowBinding.tvTime0, rowBinding.tvTime1, rowBinding.tvTime2, rowBinding.tvTime3,
            rowBinding.tvTime4, rowBinding.tvTime5, rowBinding.tvTime6, rowBinding.tvTime7
        )
        timeViews.forEachIndexed { i, tv -> tv.text = row.times.getOrElse(i) { "-" } }
        return rowBinding.root
    }

    private fun checkLocationPermission() {
        if (sessionManager.isManualLocationMode()) {
            useManualLocation(sessionManager.getManualLat(), sessionManager.getManualLng())
            return
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        } else {
            getLocation()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val lat = location?.latitude ?: -6.2088
            val long = location?.longitude ?: 106.8456
            viewModel.loadMonth(lat, long, 0)
        }
    }

    private fun useDefaultLocation() {
        Toast.makeText(this, "Izin lokasi ditolak, menggunakan default Jakarta", Toast.LENGTH_SHORT).show()
        viewModel.loadMonth(-6.2088, 106.8456, 0)
    }

    private fun useManualLocation(lat: Double, lon: Double) {
        viewModel.loadMonth(lat, lon, 0)
    }
}
