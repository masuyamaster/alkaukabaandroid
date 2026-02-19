package site.elahady.alkaukaba.viewmodel.waktusholat
import site.elahady.alkaukaba.api.RetrofitClient
import site.elahady.alkaukaba.api.TimingPrayers
import site.elahady.alkaukaba.utils.QiblaCalculator
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import site.elahady.alkaukaba.utils.PrayerTextCalculator
import java.util.*

class PrayerTimesViewModel : ViewModel() {

    private val _prayerTimings = MutableLiveData<TimingPrayers?>()
    val prayerTimings: LiveData<TimingPrayers?> = _prayerTimings

    private val _qiblaDetailText = MutableLiveData<String>()
    val qiblaDetailText: LiveData<String> = _qiblaDetailText

    private val _qiblaDegreeUI = MutableLiveData<String>()
    val qiblaDegreeUI: LiveData<String> = _qiblaDegreeUI

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _prayerCalcDetailText = MutableLiveData<String>()
    val prayerCalcDetailText: LiveData<String> = _prayerCalcDetailText

    fun loadData(lat: Double, long: Double) {
        _isLoading.value = true
        calculateQibla(lat, long)
        calculatePrayerDetails(lat, long) // <--- Panggil fungsi baru ini
        fetchPrayerTimes(lat, long)
    }

    // Fungsi Baru
    private fun calculatePrayerDetails(lat: Double, long: Double) {
        val timeZone = TimeZone.getDefault()
        val now = System.currentTimeMillis()
        val offsetMillis = timeZone.getOffset(now)
        val timeZoneHour = offsetMillis / (1000.0 * 60 * 60)
        val details = PrayerTextCalculator.generatePrayerDetails(lat, long, timeZoneHour)
        _prayerCalcDetailText.value = details
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
                // PERBAIKAN 1: Cara panggil fungsi extension
                // Panggil .awaitResponse() di belakang fungsi API
                val response = RetrofitClient.instance
                    .getTimingPrayers(lat, long, 20, date)
                    .awaitResponse()

                if (response.isSuccessful) {
                    _prayerTimings.value = response.body()?.data?.timings
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

    // PERBAIKAN 2: Definisi Extension Function yang Benar
    // Fungsi ini menempel pada kelas Call<T>, bukan pada package retrofit2
    private suspend fun <T> Call<T>.awaitResponse(): Response<T> {
        return withContext(Dispatchers.IO) {
            this@awaitResponse.execute()
        }
    }
}