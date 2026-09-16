package site.elahady.alkaukaba.viewmodel.jadwalimsakiyah

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.cosinekitty.astronomy.Observer
import kotlinx.coroutines.launch
import site.elahady.alkaukaba.api.PrayerData
import site.elahady.alkaukaba.api.Timings
import site.elahady.alkaukaba.repo.PrayerRepository
import site.elahady.alkaukaba.utils.HijriCalendarEngine
import site.elahady.alkaukaba.viewmodel.waktusholat.PrayerKind
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ImsakiyahRow(val hijriDay: Int, val gregorianLabel: String, val times: List<String>)

data class ImsakiyahUiState(
    val monthLabel: String,
    val columnLabels: List<String>,
    val rows: List<ImsakiyahRow>
)

// Tabel jadwal imsakiyah 1 bulan Hijriyah penuh: kolom hari + semua PrayerKind yang ada di
// menu Waktu Sholat (lihat PrayerTimesViewModel). Data diambil sekali/dua kali panggil lewat
// PrayerRepository.getIslamicHolidays() (endpoint /v1/calendar, 1 bulan Masehi per panggilan),
// BUKAN loop per-hari - 1 bulan Hijriyah membentang 1-2 bulan Masehi.
class JadwalImsakiyahViewModel(private val repository: PrayerRepository) : ViewModel() {

    companion object {
        // Waktu Dhuha tidak disediakan langsung oleh API Aladhan, dihitung dari Sunrise + offset ini
        // (sama seperti PrayerTimesViewModel.DHUHA_OFFSET_MINUTES).
        private const val DHUHA_OFFSET_MINUTES = 15

        private val COLUMN_ORDER = listOf(
            PrayerKind.TSULUTSUL_LAIL, PrayerKind.IMSAK, PrayerKind.SUBUH, PrayerKind.DHUHA,
            PrayerKind.DZUHUR, PrayerKind.ASHAR, PrayerKind.MAGHRIB, PrayerKind.ISYA
        )
        private val COLUMN_LABELS = listOf(
            "Sepertiga\nMalam", "Imsak", "Subuh", "Dhuha", "Dzuhur", "Ashar", "Maghrib", "Isya"
        )
    }

    private val _uiState = MutableLiveData<ImsakiyahUiState>()
    val uiState: LiveData<ImsakiyahUiState> = _uiState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var lastLat: Double? = null
    private var lastLng: Double? = null
    private var monthOffset: Int = 0

    fun loadMonth(lat: Double, lng: Double, offset: Int) {
        lastLat = lat
        lastLng = lng
        monthOffset = offset
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val observer = Observer(lat, lng, 0.0)
                val range = HijriCalendarEngine.monthRangeForOffset(observer, Calendar.getInstance(), offset)

                val dayCalendars = (0 until range.dayCount).map { dayIndex ->
                    (range.startDate.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, dayIndex) }
                }
                val monthYearPairs = dayCalendars
                    .map { (it.get(Calendar.MONTH) + 1) to it.get(Calendar.YEAR) }
                    .distinct()

                val apiData = mutableListOf<PrayerData>()
                for ((month, year) in monthYearPairs) {
                    val response = repository.getIslamicHolidays(lat, lng, month, year)
                    if (response.isSuccessful) {
                        response.body()?.data?.let { apiData.addAll(it) }
                    }
                }

                val apiDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                val localDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val labelDateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

                val rows = dayCalendars.mapIndexed { index, dayCal ->
                    val dateStr = localDateFormat.format(dayCal.time)
                    val match = apiData.find { data ->
                        try {
                            val apiDate = apiDateFormat.parse(data.date.readable)
                            apiDate != null && localDateFormat.format(apiDate) == dateStr
                        } catch (e: Exception) {
                            false
                        }
                    }
                    ImsakiyahRow(
                        hijriDay = index + 1,
                        gregorianLabel = labelDateFormat.format(dayCal.time),
                        times = buildTimes(match?.timings)
                    )
                }

                _uiState.value = ImsakiyahUiState(
                    monthLabel = "${range.monthName} ${range.year} H",
                    columnLabels = COLUMN_LABELS,
                    rows = rows
                )
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat jadwal imsakiyah: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun nextMonth() {
        val lat = lastLat ?: return
        val lng = lastLng ?: return
        loadMonth(lat, lng, monthOffset + 1)
    }

    fun prevMonth() {
        val lat = lastLat ?: return
        val lng = lastLng ?: return
        loadMonth(lat, lng, monthOffset - 1)
    }

    private fun buildTimes(timings: Timings?): List<String> {
        if (timings == null) return COLUMN_ORDER.map { "-" }
        val dhuha = timings.sunrise?.take(5)?.let { addMinutes(it, DHUHA_OFFSET_MINUTES) } ?: "-"
        return COLUMN_ORDER.map { kind ->
            when (kind) {
                PrayerKind.TSULUTSUL_LAIL -> timings.lastThird?.take(5) ?: "-"
                PrayerKind.IMSAK -> timings.imsak?.take(5) ?: "-"
                PrayerKind.SUBUH -> timings.Fajr.take(5)
                PrayerKind.DHUHA -> dhuha
                PrayerKind.DZUHUR -> timings.Dhuhr.take(5)
                PrayerKind.ASHAR -> timings.Asr.take(5)
                PrayerKind.MAGHRIB -> timings.Maghrib.take(5)
                PrayerKind.ISYA -> timings.Isha.take(5)
            }
        }
    }

    private fun addMinutes(time: String, minutesToAdd: Int): String {
        return try {
            val parts = time.split(":")
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            cal.set(Calendar.MINUTE, parts[1].toInt())
            cal.set(Calendar.SECOND, 0)
            cal.add(Calendar.MINUTE, minutesToAdd)
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
        } catch (e: Exception) {
            time
        }
    }
}
