// Di file PrayerRepository.kt

// Tambahkan import ini
import Site.elahady.alkaukaba.api.AladhanApi
import Site.elahady.alkaukaba.api.PrayerResponse
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrayerRepository(private val api: AladhanApi) {

    suspend fun getPrayerTimes(lat: Double, lng: Double): Response<PrayerResponse> {
        val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val method = 20
        return api.getTimings(today, lat, lng,method)
    }
}