package Site.elahady.alkaukaba.ui.awalbulan

import site.elahady.alkaukaba.databinding.ActivityAwalBulanBinding
import site.elahady.alkaukaba.viewmodel.hilal.HilalViewModel
import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.*

class AwalBulanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAwalBulanBinding
    private lateinit var viewModel: HilalViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedDate = Calendar.getInstance()

    // Default Jakarta
    private var currentLat = -6.2088
    private var currentLng = 106.8456

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
         binding = ActivityAwalBulanBinding.inflate(layoutInflater)
         setContentView(binding.root)

        viewModel = ViewModelProvider(this)[HilalViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupUI()
        setupObservers()
        getLocation() // Ambil GPS
    }

    private fun setupUI() {
        updateDateText()
         binding.tvDateInput.setOnClickListener {
             showDatePicker()
         }
         binding.btnCalculate.setOnClickListener {
             val alt = binding.etAltitude.text.toString()
             val ref = binding.etRefraksi.text.toString()
             viewModel.calculateHilal(currentLat, currentLng, alt, ref, selectedDate)
         }
         binding.btnDownloadPdf.setOnClickListener {
             viewModel.generatePdf(this)
         }
    }

    private fun setupObservers() {
        viewModel.calculationResult.observe(this) { result ->
             binding.layoutResultContainer.visibility = View.VISIBLE
             binding.tvHasil.visibility = View.VISIBLE
             binding.cardIjtima.tvTitle.text = "Ijtima' (Konjungsi) Bulan"
             binding.cardIjtima.tvValue.text = result.ijtimaTime

             binding.cardGhurub.tvTitle.text = "Saat Matahari Terbenam"
             binding.cardGhurub.tvValue.text = result.ghurubTime

             binding.cardHilalHeight.tvTitle.text = "Tinggi Hilal Haqiqi"
             binding.cardHilalHeight.tvValue.text = result.moonAltitude
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLat = location.latitude
                currentLng = location.longitude
            } else {
                Toast.makeText(this, "GPS null, menggunakan Default Jakarta", Toast.LENGTH_SHORT).show()
            }
             binding.etCoordinates.setText("$currentLat, $currentLng")
             binding.tvLatLongDetail.text = "Lat: $currentLat Long: $currentLng"
        }
    }

    private fun showDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            selectedDate.set(Calendar.YEAR, year)
            selectedDate.set(Calendar.MONTH, month)
            selectedDate.set(Calendar.DAY_OF_MONTH, day)
            updateDateText()
        }
        DatePickerDialog(this, dateSetListener,
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDateText() {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID"))
         binding.tvDateInput.text = sdf.format(selectedDate.time)
    }
}