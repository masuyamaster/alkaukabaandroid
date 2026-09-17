package site.elahady.alkaukaba.ui.gerhana

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.adapter.LunarEclipseAdapter
import site.elahady.alkaukaba.adapter.SolarEclipseAdapter
import site.elahady.alkaukaba.databinding.ActivityGerhanaBinding
import site.elahady.alkaukaba.utils.SessionManager
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme
import site.elahady.alkaukaba.viewmodel.gerhana.GerhanaViewModel
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class GerhanaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGerhanaBinding
    private lateinit var viewModel: GerhanaViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sessionManager: SessionManager
    private lateinit var lunarAdapter: LunarEclipseAdapter
    private lateinit var solarAdapter: SolarEclipseAdapter

    // SharedPreferences terpisah dari SessionManager (pengaturan lokasi global) -> override
    // lokasi di sini murni lokal untuk layar Gerhana, tidak pernah menulis/membaca
    // SessionManager punya konfigurasi lokasi.
    private val pagePrefs by lazy { getSharedPreferences(PAGE_PREFS_NAME, Context.MODE_PRIVATE) }
    private var etPageManualLatRef: EditText? = null
    private var etPageManualLngRef: EditText? = null

    // Default Jakarta (fallback kalau GPS/manual tidak tersedia)
    private var currentLat = -6.2088
    private var currentLng = 106.8456

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGerhanaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        applyStatusBarIconsForTheme()
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        viewModel = ViewModelProvider(this)[GerhanaViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sessionManager = SessionManager(this)

        setupUI()
        setupTabs()
        setupRecyclerViews()
        setupObservers()
        resolveLocationAndCalculate()
    }

    private fun setupUI() {
        binding.includeToolbar.tvToolbarTitle.text = "Gerhana"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnRefreshLoc.setOnClickListener { showLocationSheet() }
    }

    private fun setupTabs() {
        updateTabState(isLunar = true)
        binding.btnTabBulan.setOnClickListener { updateTabState(isLunar = true) }
        binding.btnTabMatahari.setOnClickListener { updateTabState(isLunar = false) }
    }

    private fun updateTabState(isLunar: Boolean) {
        val colorActive = ContextCompat.getColor(this, R.color.text_label_gold)
        val colorInactive = ContextCompat.getColor(this, R.color.waktu_sholat_icon_muted)

        val activeTab = if (isLunar) binding.btnTabBulan else binding.btnTabMatahari
        val inactiveTab = if (isLunar) binding.btnTabMatahari else binding.btnTabBulan

        activeTab.setBackgroundResource(R.drawable.bg_tab_underline_active_navy)
        activeTab.setTextColor(colorActive)
        activeTab.setTypeface(null, android.graphics.Typeface.BOLD)

        inactiveTab.setBackgroundResource(R.drawable.bg_tab_underline_inactive)
        inactiveTab.setTextColor(colorInactive)
        inactiveTab.setTypeface(null, android.graphics.Typeface.NORMAL)

        binding.rvLunarEclipse.visibility = if (isLunar) View.VISIBLE else View.GONE
        binding.rvSolarEclipse.visibility = if (isLunar) View.GONE else View.VISIBLE
    }

    private fun setupRecyclerViews() {
        lunarAdapter = LunarEclipseAdapter()
        binding.rvLunarEclipse.apply {
            layoutManager = LinearLayoutManager(this@GerhanaActivity)
            adapter = lunarAdapter
        }

        solarAdapter = SolarEclipseAdapter()
        binding.rvSolarEclipse.apply {
            layoutManager = LinearLayoutManager(this@GerhanaActivity)
            adapter = solarAdapter
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.result.observe(this) { result ->
            lunarAdapter.setData(result.lunarEclipses)
            solarAdapter.setData(result.solarEclipses)
        }
    }

    private fun resolveLocationAndCalculate() {
        if (hasPageLocationOverride()) {
            currentLat = pagePrefs.getFloat(KEY_PAGE_LAT, 0f).toDouble()
            currentLng = pagePrefs.getFloat(KEY_PAGE_LNG, 0f).toDouble()
            updateCoordinateDisplay()
            runCalculation()
            return
        }
        if (sessionManager.isManualLocationMode()) {
            currentLat = sessionManager.getManualLat()
            currentLng = sessionManager.getManualLng()
            updateCoordinateDisplay()
            runCalculation()
            return
        }
        getGpsLocation()
    }

    private fun hasPageLocationOverride(): Boolean =
        pagePrefs.contains(KEY_PAGE_LAT) && pagePrefs.contains(KEY_PAGE_LNG)

    /**
     * Dialog "Ubah Lokasi" khusus layar Gerhana: pilihan "Ikuti pengaturan global" (default,
     * sama seperti perilaku lama) atau "Manual khusus halaman ini". Pilihan manual disimpan ke
     * [pagePrefs] (SharedPreferences terpisah dari [SessionManager]) sehingga tidak mengubah
     * pengaturan lokasi global yang dipakai fitur lain.
     */
    private fun showLocationSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_lokasi_halaman, null)
        bottomSheetDialog.setContentView(view)

        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupPageLocationMode)
        val radioGlobal = view.findViewById<RadioButton>(R.id.radioPageLocationGlobal)
        val radioManual = view.findViewById<RadioButton>(R.id.radioPageLocationManual)
        val layoutManual = view.findViewById<View>(R.id.layoutPageManualLocation)
        val etLat = view.findViewById<EditText>(R.id.etPageManualLat)
        val etLng = view.findViewById<EditText>(R.id.etPageManualLng)
        val btnUseGps = view.findViewById<AppCompatButton>(R.id.btnPageUseCurrentGps)
        val btnSave = view.findViewById<AppCompatButton>(R.id.btnSavePageLocation)

        val isOverride = hasPageLocationOverride()
        radioManual.isChecked = isOverride
        radioGlobal.isChecked = !isOverride
        layoutManual.visibility = if (isOverride) View.VISIBLE else View.GONE
        if (isOverride) {
            etLat.setText(pagePrefs.getFloat(KEY_PAGE_LAT, 0f).toString())
            etLng.setText(pagePrefs.getFloat(KEY_PAGE_LNG, 0f).toString())
        }

        etPageManualLatRef = etLat
        etPageManualLngRef = etLng

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            layoutManual.visibility = if (checkedId == R.id.radioPageLocationManual) View.VISIBLE else View.GONE
        }

        btnUseGps.setOnClickListener { fetchGpsIntoPageManualFields() }

        btnSave.setOnClickListener {
            if (radioGroup.checkedRadioButtonId == R.id.radioPageLocationManual) {
                val lat = etLat.text.toString().toDoubleOrNull()
                val lng = etLng.text.toString().toDoubleOrNull()
                if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
                    Toast.makeText(this, "Isi lintang/bujur dengan angka yang valid", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                pagePrefs.edit()
                    .putFloat(KEY_PAGE_LAT, lat.toFloat())
                    .putFloat(KEY_PAGE_LNG, lng.toFloat())
                    .apply()
            } else {
                pagePrefs.edit().remove(KEY_PAGE_LAT).remove(KEY_PAGE_LNG).apply()
            }
            Toast.makeText(this, "Lokasi disimpan", Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss()
            resolveLocationAndCalculate()
        }

        bottomSheetDialog.setOnDismissListener {
            etPageManualLatRef = null
            etPageManualLngRef = null
        }

        bottomSheetDialog.show()
    }

    private fun fetchGpsIntoPageManualFields() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PAGE_LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                etPageManualLatRef?.setText(location.latitude.toString())
                etPageManualLngRef?.setText(location.longitude.toString())
            } else {
                Toast.makeText(this, "Lokasi GPS tidak ditemukan, coba lagi", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal mengambil lokasi GPS", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getGpsLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLat = location.latitude
                currentLng = location.longitude
            } else {
                Toast.makeText(this, "GPS tidak ditemukan, menggunakan default Jakarta", Toast.LENGTH_SHORT).show()
            }
            updateCoordinateDisplay()
            runCalculation()
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal mengambil lokasi GPS", Toast.LENGTH_SHORT).show()
            updateCoordinateDisplay()
            runCalculation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        when (requestCode) {
            PAGE_LOCATION_PERMISSION_REQUEST_CODE -> {
                // Diminta dari dalam dialog "Ubah Lokasi" (tombol pakai GPS) -> isi field dialog,
                // bukan langsung hitung ulang seperti alur permission utama.
                if (granted) {
                    fetchGpsIntoPageManualFields()
                } else {
                    Toast.makeText(this, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
                }
            }
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (granted) {
                    getGpsLocation()
                } else {
                    Toast.makeText(this, "Izin lokasi ditolak, menggunakan default Jakarta", Toast.LENGTH_SHORT).show()
                    updateCoordinateDisplay()
                    runCalculation()
                }
            }
        }
    }

    private fun updateCoordinateDisplay() {
        resolveLocationName(currentLat, currentLng)
    }

    // Ubah koordinat mentah jadi nama lokasi (kabupaten/kota, provinsi) via reverse geocoding.
    private fun resolveLocationName(lat: Double, lng: Double) {
        binding.tvLocationName.text = "Mendeteksi lokasi..."
        lifecycleScope.launch(Dispatchers.IO) {
            val placeName = try {
                @Suppress("DEPRECATION")
                val results = Geocoder(this@GerhanaActivity, Locale("in", "ID")).getFromLocation(lat, lng, 1)
                val address = results?.firstOrNull()
                listOfNotNull(address?.subAdminArea ?: address?.adminArea, address?.countryName)
                    .joinToString(", ")
                    .ifBlank { null }
            } catch (e: Exception) {
                null
            }
            withContext(Dispatchers.Main) {
                binding.tvLocationName.text = placeName
                    ?: "%.4f, %.4f".format(Locale.US, lat, lng)
            }
        }
    }

    private fun runCalculation() {
        viewModel.calculateEclipses(currentLat, currentLng, 0.0)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
        private const val PAGE_LOCATION_PERMISSION_REQUEST_CODE = 101
        private const val PAGE_PREFS_NAME = "GerhanaPagePrefs"
        private const val KEY_PAGE_LAT = "PAGE_MANUAL_LAT"
        private const val KEY_PAGE_LNG = "PAGE_MANUAL_LNG"
    }
}
