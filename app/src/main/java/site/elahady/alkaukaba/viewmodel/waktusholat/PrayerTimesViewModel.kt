package site.elahady.alkaukaba.viewmodel.waktusholat
import site.elahady.alkaukaba.repo.PrayerRepository
import site.elahady.alkaukaba.api.TimingPrayers
import site.elahady.alkaukaba.utils.QiblaCalculator
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import site.elahady.alkaukaba.utils.prayerbreakdown.PrayerBreakdownSection
import site.elahady.alkaukaba.utils.prayerbreakdown.PrayerCalculationBreakdownRegistry
import java.util.*

enum class PrayerKind {
    TSULUTSUL_LAIL, IMSAK, SUBUH, DHUHA, DZUHUR, ASHAR, MAGHRIB, ISYA
}

data class PrayerScheduleItem(val kind: PrayerKind, val label: String, val time: String)

data class PrayerScheduleUiState(
    val items: List<PrayerScheduleItem>,
    val activeIndex: Int,
    val nextPrayerLabel: String,
    val nextPrayerTime: String
)

class PrayerTimesViewModel(private val repository: PrayerRepository) : ViewModel() {

    companion object {
        // Waktu Dhuha tidak disediakan langsung oleh API Aladhan, dihitung dari Sunrise + offset ini
        private const val DHUHA_OFFSET_MINUTES = 15
    }

    private val _prayerSchedule = MutableLiveData<PrayerScheduleUiState>()
    val prayerSchedule: LiveData<PrayerScheduleUiState> = _prayerSchedule

    private val _qiblaDetailText = MutableLiveData<String>()
    val qiblaDetailText: LiveData<String> = _qiblaDetailText

    private val _qiblaDegreeUI = MutableLiveData<String>()
    val qiblaDegreeUI: LiveData<String> = _qiblaDegreeUI

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // Null kalau metode aktif tidak punya breakdown detail (lihat PrayerCalculationBreakdownRegistry)
    private val _calculationBreakdown = MutableLiveData<List<PrayerBreakdownSection>?>()
    val calculationBreakdown: LiveData<List<PrayerBreakdownSection>?> = _calculationBreakdown

    fun loadData(lat: Double, long: Double) {
        _isLoading.value = true
        calculateQibla(lat, long)
        calculatePrayerBreakdown(lat, long)
        fetchPrayerTimes(lat, long)
    }

    private fun calculatePrayerBreakdown(lat: Double, long: Double) {
        val offsetMillis = TimeZone.getDefault().getOffset(System.currentTimeMillis())
        val timeZoneHour = offsetMillis / (1000.0 * 60 * 60)
        val provider = PrayerCalculationBreakdownRegistry.providerFor(repository.getSelectedMethodId())
        _calculationBreakdown.value = provider?.breakdown(lat, long, timeZoneHour)
    }

    private fun calculateQibla(lat: Double, long: Double) {
        val result = QiblaCalculator.calculateQibla(lat, long)
        _qiblaDetailText.value = result.detailFormulaSteps
        _qiblaDegreeUI.value = String.format("%.0f° UTSB", 270 + result.qiblaDegree)
    }

    private fun fetchPrayerTimes(lat: Double, long: Double) {
        val date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

        viewModelScope.launch {
            try {
                val response = repository.getTimingPrayers(lat, long, date)

                if (response.isSuccessful) {
                    response.body()?.data?.timings?.let { _prayerSchedule.value = buildSchedule(it) }
                } else {
                    _errorMessage.value = "Gagal mengambil data: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Terjadi kesalahan koneksi: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Business logic "sholat mana yang sedang aktif sekarang" - sengaja di ViewModel,
    // bukan Activity, supaya Activity cuma perlu bind PrayerScheduleUiState ke View.
    private fun buildSchedule(timings: TimingPrayers): PrayerScheduleUiState {
        val dhuha = addMinutes(timings.sunrise.take(5), DHUHA_OFFSET_MINUTES)

        val items = listOf(
            PrayerScheduleItem(PrayerKind.TSULUTSUL_LAIL, "Tsulutsul Lail Akhir", timings.tsulutsulLailAkhir.take(5)),
            PrayerScheduleItem(PrayerKind.IMSAK, "Imsak", timings.imsak.take(5)),
            PrayerScheduleItem(PrayerKind.SUBUH, "Subuh", timings.subuh.take(5)),
            PrayerScheduleItem(PrayerKind.DHUHA, "Dhuha", dhuha),
            PrayerScheduleItem(PrayerKind.DZUHUR, "Dzuhur", timings.dzuhur.take(5)),
            PrayerScheduleItem(PrayerKind.ASHAR, "Ashar", timings.ashar.take(5)),
            PrayerScheduleItem(PrayerKind.MAGHRIB, "Maghrib", timings.maghrib.take(5)),
            PrayerScheduleItem(PrayerKind.ISYA, "Isya", timings.isya.take(5))
        )

        val currentMinutes = timeToMinutes(
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        )
        val activeIndex = items.indexOfLast { timeToMinutes(it.time) <= currentMinutes }
            .let { if (it == -1) items.size - 1 else it }

        return PrayerScheduleUiState(
            items = items,
            activeIndex = activeIndex,
            nextPrayerLabel = items[activeIndex].label,
            nextPrayerTime = items[activeIndex].time
        )
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

    private fun timeToMinutes(time: String): Int {
        return try {
            val parts = time.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            (hour * 60) + minute
        } catch (e: Exception) {
            0
        }
    }
}
