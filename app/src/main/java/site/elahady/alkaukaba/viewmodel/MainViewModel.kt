package site.elahady.alkaukaba.viewmodel

import site.elahady.alkaukaba.repo.PrayerRepository
import site.elahady.alkaukaba.adapter.DayUIModel
import site.elahady.alkaukaba.api.HolidayItem
import site.elahady.alkaukaba.api.Timings
import site.elahady.alkaukaba.utils.HijriCalendarEngine
import site.elahady.alkaukaba.utils.HijriDateUtil
import site.elahady.alkaukaba.utils.HijriHolidayTranslator
import site.elahady.alkaukaba.utils.Resource
import android.location.Geocoder
import io.github.cosinekitty.astronomy.Observer
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
    private var selectedDate: Calendar = Calendar.getInstance()
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
                        val holidays = HijriHolidayTranslator.translateJoined(
                            data.date.hijri.holidays.joinToString(", ")
                        )
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

        // Batas toleransi menampilkan "sholat terakhir" sebelum beralih ke sholat berikutnya
        val toleranceMillis = TimeUnit.MINUTES.toMillis(20)

        val lastPassed = sortedPrayers.lastOrNull { now.after(it.second) }
        val upcomingToday = sortedPrayers.firstOrNull { now.before(it.second) }

        val targetName: String
        val targetTime: Date
        val isPassed: Boolean

        if (lastPassed != null && (now.time - lastPassed.second.time) <= toleranceMillis) {
            // Masih dalam toleransi setelah waktu sholat terakhir lewat
            targetName = lastPassed.first
            targetTime = lastPassed.second
            isPassed = true
        } else if (upcomingToday != null) {
            // Sudah lewat toleransi -> tampilkan sholat berikutnya hari ini
            targetName = upcomingToday.first
            targetTime = upcomingToday.second
            isPassed = false
        } else {
            // Semua sholat hari ini sudah lewat toleransi -> tampilkan Subuh besok
            val tomorrowFajr = Calendar.getInstance().apply {
                time = mapJadwal["Subuh"]!!
                add(Calendar.DAY_OF_MONTH, 1)
            }.time
            targetName = "Subuh"
            targetTime = tomorrowFajr
            isPassed = false
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

                            val holidayNames = HijriHolidayTranslator.translateJoined(
                                data.date.hijri.holidays.joinToString(", ")
                            )
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

    /** Dipanggil saat user memilih tanggal lain (tap sel tanggal, atau lewat date-picker judul).
     * Pindah tampilan ke bulan Masehi dari [date] dan tandai [date] sebagai tanggal terpilih -
     * judul bulan Hijriyah & Masehi otomatis menyesuaikan lewat [fetchMonthlyCalendar]. */
    fun selectDate(date: Calendar) {
        currentCalendar = date.clone() as Calendar
        selectedDate = date.clone() as Calendar
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
                val selectedDateStr = localDateFormat.format(selectedDate.time)

                val uiList = mutableListOf<DayUIModel>()

                // Judul Hijriyah mengikuti tanggal yang SEDANG DIPILIH (bukan selalu tanggal 1),
                // soalnya 1 bulan Masehi hampir selalu memuat 2 bulan Hijriyah - kalau tanggal
                // terpilih ada di sisi bulan Hijriyah berikutnya, judul harus ikut situ, bukan
                // tetap nampilin bulan Hijriyah di awal bulan Masehi. Prev/next (tanpa pilih
                // tanggal spesifik di bulan ini) tetap default ke tanggal 1.
                val titleDayIndex = if (
                    selectedDate.get(Calendar.YEAR) == year && selectedDate.get(Calendar.MONTH) + 1 == month
                ) {
                    selectedDate.get(Calendar.DAY_OF_MONTH) - 1
                } else {
                    0
                }

                // -- LOGIC TANGGAL & JUDUL HIJRIAH --
                // Sumber kebenaran tanggal 1 Hijriyah dipindah dari API pihak ketiga ke mesin
                // hisab yang sama dengan fitur Awal Bulan (lihat HijriCalendarEngine), supaya
                // selalu sinkron. API hari besar Islam di bawah ini HANYA dipakai untuk info
                // hari libur (dicocokkan lewat tanggal Masehi, bukan tanggal Hijriyahnya).
                val observer = Observer(lastLat, lastLng, 0.0)
                val hijriDaysForMonth = try {
                    HijriCalendarEngine.buildCalendar(observer, processingCal, daysInMonth)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                if (hijriDaysForMonth != null) {
                    _hijriTitle.postValue(hijriDaysForMonth[titleDayIndex].label)
                } else {
                    // Fallback tabular offline kalau perhitungan astronomi gagal (mis. lokasi ekstrem)
                    val titleCal = processingCal.clone() as Calendar
                    titleCal.set(Calendar.DAY_OF_MONTH, titleDayIndex + 1)
                    val (monthName, hijriYear) = HijriDateUtil.monthYearAt(titleCal)
                    _hijriTitle.postValue("$monthName $hijriYear H")
                }
                // -------------------------

                // A. Slot Kosong
                for (i in 0 until emptySlots) {
                    uiList.add(DayUIModel(null, "", "", false, false, true))
                }

                // B. Isi Tanggal
                val apiDataList = response.body()?.data ?: emptyList()
                for (day in 1..daysInMonth) {
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

                    val hijriDay = hijriDaysForMonth?.get(day - 1)?.day?.toString()
                        ?: HijriDateUtil.fullDateLabel(processingCal).substringBefore(" ")
                    val hasHoliday = matchData?.date?.hijri?.holidays?.isNotEmpty() == true
                    val isToday = dateStr == todayStr
                    val isSelected = dateStr == selectedDateStr

                    uiList.add(DayUIModel(
                        date = date,
                        dayValue = day.toString(),
                        hijriDay = hijriDay,
                        isHoliday = hasHoliday,
                        isToday = isToday,
                        isEmpty = false,
                        isSelected = isSelected
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