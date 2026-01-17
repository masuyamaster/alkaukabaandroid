package Site.elahady.alkaukaba.viewmodel

import PrayerRepository
import Site.elahady.alkaukaba.adapter.DayUIModel
import Site.elahady.alkaukaba.api.HolidayItem
import Site.elahady.alkaukaba.api.HolidayRetrofitClient
import Site.elahady.alkaukaba.api.Timings
import Site.elahady.alkaukaba.utils.Resource
import android.location.Geocoder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class MainViewModel(private val repository: PrayerRepository) : ViewModel() {

    // LiveData untuk diobservasi oleh Activity
    private val _prayerState = MutableLiveData<Resource<PrayerUIModel>>()
    val prayerState: LiveData<Resource<PrayerUIModel>> = _prayerState

    private val _locationName = MutableLiveData<String>()
    val locationName: LiveData<String> = _locationName

    private val _holidayAlert = MutableLiveData<String?>()
    val holidayAlert: LiveData<String?> = _holidayAlert

    private val _holidayPreview = MutableLiveData<Resource<List<HolidayItem>>>()
    val holidayPreview: LiveData<Resource<List<HolidayItem>>> = _holidayPreview

    private val _weeklyCalendar = MutableLiveData<Resource<List<DayUIModel>>>()
    val weeklyCalendar: LiveData<Resource<List<DayUIModel>>> = _weeklyCalendar

    private var currentCalendar = Calendar.getInstance()
    private var lastLat = 0.0
    private var lastLng = 0.0

    private val _hijriTitle = MutableLiveData<String>()
    val hijriTitle: LiveData<String> = _hijriTitle

    // LiveData Nama Bulan & Tahun (untuk UI Header)
    private val _monthYearTitle = MutableLiveData<String>()
    val monthYearTitle: LiveData<String> = _monthYearTitle

    // LiveData List Tanggal
    private val _calendarData = MutableLiveData<Resource<List<DayUIModel>>>()
    val calendarData: LiveData<Resource<List<DayUIModel>>> = _calendarData


    fun fetchPrayerData(lat: Double, lng: Double) {
        _prayerState.value = Resource.Loading()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = repository.getPrayerTimes(lat, lng)
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data

                    // 1. Proses Waktu Sholat (Business Logic)
                    val uiModel = calculateNextPrayer(data.timings)
                    _prayerState.postValue(Resource.Success(uiModel))

                    // 2. Proses Hari Besar Islam
                    if (data.date.hijri.holidays.isNotEmpty()) {
                        val holidays = data.date.hijri.holidays.joinToString(", ")
                        _holidayAlert.postValue(holidays)
                    } else {
                        // Cek Hari Nasional Masehi (Logic Sederhana)
                        checkNationalHoliday()
                    }

                } else {
                    _prayerState.postValue(Resource.Error(response.message()))
                }
            } catch (e: Exception) {
                _prayerState.postValue(Resource.Error(e.message ?: "Unknown Error"))
            }
        }
    }

    fun fetchAddressName(geocoder: Geocoder, lat: Double, lng: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val kecamatan = address.subLocality ?: address.locality
                    val kota = address.subAdminArea ?: address.adminArea
                    val negara = address.countryName

                    val fullName = listOfNotNull(kecamatan, kota, negara).joinToString(", ")
                    _locationName.postValue(fullName) // PostValue karena dari background thread
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateNextPrayer(timings: Timings): PrayerUIModel {
        val now = Date()
        val mapJadwal = mapOf(
            "Subuh" to parseTime(timings.Fajr),
            "Dzuhur" to parseTime(timings.Dhuhr),
            "Ashar" to parseTime(timings.Asr),
            "Maghrib" to parseTime(timings.Maghrib),
            "Isya" to parseTime(timings.Isha)
        )

        val sortedPrayers = mapJadwal.toList().sortedBy { it.second }
        var targetName = "Subuh"
        var targetTime = mapJadwal["Subuh"]!!
        var isPassed = false

        for ((name, time) in sortedPrayers) {
            if (now.after(time)) {
                targetName = name
                targetTime = time
                isPassed = true
            } else {
                if (!isPassed) {
                    targetName = name
                    targetTime = time
                    isPassed = false
                    break
                }
            }
        }

        // Hitung selisih waktu
        val diff = abs(now.time - targetTime.time)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        val timeDiffString = if (hours > 0) "$hours jam $minutes menit" else "$minutes menit"

        val displayTime = SimpleDateFormat("HH:mm", Locale("id", "ID")).format(targetTime)

        return PrayerUIModel(
            prayerName = "Waktu $targetName",
            prayerTime = displayTime,
            statusText = if (isPassed) "Sudah lewat +$timeDiffString yang lalu" else "Akan datang dalam $timeDiffString",
            isPassed = isPassed,
            topLabel = if (isPassed) "Sholat Terakhir ($displayTime)" else "Sholat Berikutnya ($displayTime)"
        )
    }

    private fun checkNationalHoliday() {
        val todayStr = SimpleDateFormat("dd-MM", Locale.getDefault()).format(Date())
        val nationalHolidays = mapOf(
            "17-08" to "Hari Kemerdekaan RI",
            "01-01" to "Tahun Baru Masehi"
        )
        nationalHolidays[todayStr]?.let {
            _holidayAlert.postValue(it)
        }
    }

    private fun parseTime(timeStr: String): Date {
        val cleanTime = timeStr.substring(0, 5)
        val parts = cleanTime.split(":")
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
        cal.set(Calendar.MINUTE, parts[1].toInt())
        cal.set(Calendar.SECOND, 0)
        return cal.time
    }

    fun fetchUpcomingIslamicHolidays(lat: Double, lng: Double) {
        _holidayPreview.postValue(Resource.Loading())

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cal = Calendar.getInstance()
                val currentMonth = cal.get(Calendar.MONTH) + 1
                val currentYear = cal.get(Calendar.YEAR)

                val response = repository.getIslamicHolidays(lat, lng, currentMonth, currentYear)

                if (response.isSuccessful && response.body() != null) {
                    val rawData = response.body()!!.data
                    val apiDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                    val outputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    val today = Date()

                    val islamicHolidays = rawData
                        .asSequence()
                        .filter {
                            it.date.hijri.holidays.isNotEmpty()
                        }
                        .map { data ->
                            val dateObj = try {
                                apiDateFormat.parse(data.date.readable)
                            } catch (e: Exception) { null }

                            if (dateObj == null) return@map null

                            val holidayNames = data.date.hijri.holidays.joinToString(", ")
                            val hijriDay = data.date.hijri.day
                            val hijriMonth = data.date.hijri.month.en
                            val hijriYear = data.date.hijri.year
                            val hijriString = "$hijriDay $hijriMonth $hijriYear H"

                            HolidayItem(
                                tanggal = outputDateFormat.format(dateObj),
                                tanggalHijriah = hijriString,
                                keterangan = holidayNames,
                                is_cuti = true
                            )
                        }
                        .filterNotNull() // Hapus data yang null akibat gagal parsing
                        .filter {
                            // Logic filter tanggal (Convert string balik ke Date untuk compare)
                            val itemDate = outputDateFormat.parse(it.tanggal)
                            itemDate != null && !itemDate.before(today)
                        }
                        .sortedBy { it.tanggal }
                        .take(3)
                        .toList()

                    if (islamicHolidays.isEmpty()) {
                        _holidayPreview.postValue(Resource.Error("Tidak ada hari besar Islam bulan ini."))
                    } else {
                        _holidayPreview.postValue(Resource.Success(islamicHolidays))
                    }

                } else {
                    _holidayPreview.postValue(Resource.Error(response.message()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _holidayPreview.postValue(Resource.Error(e.message ?: "Gagal memuat data"))
                println("error vm :: " + e.message)
            }
        }
    }

    // Fungsi awal dipanggil dari MainActivity saat dapat lokasi
    fun initCalendar(lat: Double, lng: Double) {
        lastLat = lat
        lastLng = lng
        fetchMonthlyCalendar()
    }

    // Fungsi Navigasi (Next/Prev)
    fun changeMonth(amount: Int) {
        currentCalendar.add(Calendar.MONTH, amount)
        fetchMonthlyCalendar()
    }

    private fun fetchMonthlyCalendar() {
        // Post Loading State
        _calendarData.postValue(Resource.Loading())
        updateTitle() // Update judul Masehi dulu

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // ... (Setup Calendar Logic seperti sebelumnya) ...
                val processingCal = currentCalendar.clone() as Calendar
                processingCal.set(Calendar.DAY_OF_MONTH, 1)

                val month = processingCal.get(Calendar.MONTH) + 1
                val year = processingCal.get(Calendar.YEAR)
                val daysInMonth = processingCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val startDayOfWeek = processingCal.get(Calendar.DAY_OF_WEEK)
                val emptySlots = startDayOfWeek - 1

                val response = repository.getIslamicHolidays(lastLat, lastLng, month, year)

                // ... (Setup DateFormatters) ...
                val apiDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                val localDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = localDateFormat.format(Date())

                val uiList = mutableListOf<DayUIModel>()

                // -- LOGIC JUDUL HIJRIAH --
                // Ambil data Hijriah dari item pertama yang valid di API response
                // untuk dijadikan judul bulan ini (Contoh: "Rajab 1447 H")
                if (response.isSuccessful && response.body()?.data?.isNotEmpty() == true) {
                    val firstItem = response.body()!!.data[0]
                    val hijriMonthName = firstItem.date.hijri.month.en // "Rajab"
                    val hijriYearVal = firstItem.date.hijri.year       // "1447"

                    _hijriTitle.postValue("$hijriMonthName $hijriYearVal H")
                } else {
                    _hijriTitle.postValue("-")
                }
                // -------------------------

                // A. Slot Kosong
                for (i in 0 until emptySlots) {
                    uiList.add(DayUIModel(null, "", "", false, false, true))
                }

                // B. Isi Tanggal
                val apiDataList = response.body()?.data ?: emptyList()
                for (day in 1..daysInMonth) {
                    // ... (Logic looping pencocokan tanggal sama seperti sebelumnya) ...
                    // (Copy paste logic loop dari jawaban sebelumnya)
                    processingCal.set(Calendar.DAY_OF_MONTH, day)
                    val date = processingCal.time
                    val dateStr = localDateFormat.format(date)

                    val matchData = apiDataList.find {
                        try {
                            val apiDate = apiDateFormat.parse(it.date.readable)
                            val apiDateStr = localDateFormat.format(apiDate!!)
                            apiDateStr == dateStr
                        } catch (e: Exception) { false }
                    }

                    val hijriDay = matchData?.date?.hijri?.day ?: "-"
                    val hasHoliday = matchData?.date?.hijri?.holidays?.isNotEmpty() == true
                    val isToday = dateStr == todayStr

                    uiList.add(DayUIModel(
                        date = date,
                        dayValue = day.toString(),
                        hijriDay = hijriDay,
                        isHoliday = hasHoliday,
                        isToday = isToday,
                        isEmpty = false
                    ))
                }

                _calendarData.postValue(Resource.Success(uiList))

            } catch (e: Exception) {
                e.printStackTrace()
                _calendarData.postValue(Resource.Error("Gagal"))
            }
        }
    }
    private fun updateTitle() {
        val format = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        _monthYearTitle.postValue(format.format(currentCalendar.time))
    }

}

data class PrayerUIModel(
    val prayerName: String,
    val prayerTime: String,
    val statusText: String,
    val isPassed: Boolean,
    val topLabel: String
)