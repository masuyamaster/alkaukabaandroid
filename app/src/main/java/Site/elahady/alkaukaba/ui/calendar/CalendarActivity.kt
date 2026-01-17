package Site.elahady.alkaukaba.ui.calendar

import Site.elahady.alkaukaba.adapter.HolidayAdapter
import Site.elahady.alkaukaba.api.HolidayRetrofitClient
import Site.elahady.alkaukaba.api.HolidayItem
import Site.elahady.alkaukaba.databinding.ActivityCalendarBinding
import android.os.Bundle
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
    private val adapter = HolidayAdapter() // Kita buat adapter di bawah

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        fetchHolidays()

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        binding.rvHolidays.layoutManager = LinearLayoutManager(this)
        binding.rvHolidays.adapter = adapter
    }

    private fun fetchHolidays() {
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Ambil Data Nasional dari API
                val response = HolidayRetrofitClient.instance.getNationalHolidays()

                // 2. Filter data tahun ini (Opsional)
                val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
                val filteredList = response.filter { it.tanggal.startsWith(currentYear) }

                // 3. Update UI
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    adapter.setData(filteredList)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@CalendarActivity, "Gagal memuat: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}