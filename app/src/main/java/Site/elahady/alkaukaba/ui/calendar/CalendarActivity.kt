package Site.elahady.alkaukaba.ui.calendar

import PrayerRepository
import Site.elahady.alkaukaba.adapter.HolidayAdapter
import Site.elahady.alkaukaba.api.HolidayItem
import Site.elahady.alkaukaba.api.RetrofitClient
import Site.elahady.alkaukaba.databinding.ActivityCalendarBinding
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private val adapter = HolidayAdapter()

    // Variable untuk menyimpan data asli dan state filter
    private var originalList: List<HolidayItem> = listOf()
    private var filterStartDate: Date? = null
    private var filterEndDate: Date? = null

    private val repository by lazy {
        PrayerRepository(RetrofitClient.instance)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearchAndFilter() // Setup Listener

        val lat = intent.getDoubleExtra("LATITUDE", -6.2088)
        val lng = intent.getDoubleExtra("LONGITUDE", 106.8456)

        fetchYearlyHolidays(lat, lng)

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        binding.rvHolidays.layoutManager = LinearLayoutManager(this)
        binding.rvHolidays.adapter = adapter
    }

    // --- SETUP LISTENER FILTER ---
    private fun setupSearchAndFilter() {
        // 1. Search Listener
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter() // Panggil filter setiap user mengetik
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 2. Start Date Picker
        binding.btnStartDate.setOnClickListener {
            showDatePicker { date ->
                filterStartDate = date
                binding.tvStartDateVal.text = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(date)
                binding.btnResetDate.visibility = View.VISIBLE
                applyFilter()
            }
        }

        // 3. End Date Picker
        binding.btnEndDate.setOnClickListener {
            showDatePicker { date ->
                filterEndDate = date
                binding.tvEndDateVal.text = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(date)
                binding.btnResetDate.visibility = View.VISIBLE
                applyFilter()
            }
        }

        // 4. Reset Button
        binding.btnResetDate.setOnClickListener {
            filterStartDate = null
            filterEndDate = null
            binding.tvStartDateVal.text = "-"
            binding.tvEndDateVal.text = "-"
            binding.btnResetDate.visibility = View.GONE
            applyFilter()
        }
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, dayOfMonth, 0, 0, 0)
                selectedCal.set(Calendar.MILLISECOND, 0)
                onDateSelected(selectedCal.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // --- LOGIC FILTER UTAMA ---
    private fun applyFilter() {
        val query = binding.etSearch.text.toString().lowercase()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val filteredList = originalList.filter { item ->
            // 1. Filter Nama (Search)
            val matchName = item.keterangan.lowercase().contains(query)

            // 2. Filter Tanggal (Range)
            var matchDate = true
            if (filterStartDate != null || filterEndDate != null) {
                try {
                    val itemDate = dateFormat.parse(item.tanggal) // item.tanggal format yyyy-MM-dd
                    if (itemDate != null) {
                        val isAfterStart = filterStartDate == null || !itemDate.before(filterStartDate)
                        val isBeforeEnd = filterEndDate == null || !itemDate.after(filterEndDate)
                        matchDate = isAfterStart && isBeforeEnd
                    }
                } catch (e: Exception) {
                    matchDate = false
                }
            }

            // Gabungkan kondisi (AND)
            matchName && matchDate
        }

        adapter.setData(filteredList)

        // Opsional: Tampilkan pesan jika kosong
        if (filteredList.isEmpty() && originalList.isNotEmpty()) {
            // Bisa tampilkan View Empty State di XML
        }
    }

    private fun fetchYearlyHolidays(lat: Double, lng: Double) {
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val allHolidays = mutableListOf<HolidayItem>()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val apiDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            val outputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            try {
                for (month in 1..12) {
                    val response = repository.getIslamicHolidays(lat, lng, month, currentYear)
                    if (response.isSuccessful && response.body() != null) {
                        val rawData = response.body()!!.data
                        val monthlyHolidays = rawData
                            .filter { it.date.hijri.holidays.isNotEmpty() }
                            .mapNotNull { data ->
                                try {
                                    val dateObj = apiDateFormat.parse(data.date.readable)
                                    if (dateObj != null) {
                                        val holidayNames = data.date.hijri.holidays.joinToString(", ")
                                        val hijriString = "${data.date.hijri.day} ${data.date.hijri.month.en} ${data.date.hijri.year} H"
                                        HolidayItem(
                                            tanggal = outputDateFormat.format(dateObj),
                                            tanggalHijriah = hijriString,
                                            keterangan = holidayNames,
                                            is_cuti = true
                                        )
                                    } else null
                                } catch (e: Exception) { null }
                            }
                        allHolidays.addAll(monthlyHolidays)
                    }
                }

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    if (allHolidays.isNotEmpty()) {
                        // SIMPAN KE ORIGINAL LIST
                        originalList = allHolidays.sortedBy { it.tanggal }

                        // Tampilkan semua data pertama kali (tanpa filter)
                        adapter.setData(originalList)
                    } else {
                        Toast.makeText(this@CalendarActivity, "Tidak ada data", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@CalendarActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}