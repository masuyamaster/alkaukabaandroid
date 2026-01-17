package Site.elahady.alkaukaba.viewmodel

import PrayerRepository
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

    fun fetchUpcomingHolidays() {
        _holidayPreview.postValue(Resource.Loading())

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = HolidayRetrofitClient.instance.getNationalHolidays()

                // 2. Filter & Sort Logic
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val upcomingItems = response
                    .filter { it.tanggal >= today }
                    .sortedBy { it.tanggal }
                    .take(3)

                _holidayPreview.postValue(Resource.Success(upcomingItems))

            } catch (e: Exception) {
                _holidayPreview.postValue(Resource.Error(e.message ?: "Gagal memuat libur"))
            }
        }
    }

}

// Data Class untuk UI (Agar ViewModel mengirim data 'matang')
data class PrayerUIModel(
    val prayerName: String,
    val prayerTime: String,
    val statusText: String,
    val isPassed: Boolean,
    val topLabel: String
)