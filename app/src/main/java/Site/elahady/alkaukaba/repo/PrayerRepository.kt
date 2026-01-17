// Di file PrayerRepository.kt

// Tambahkan import ini
import Site.elahady.alkaukaba.api.AladhanApi
import Site.elahady.alkaukaba.api.PrayerResponse
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrayerRepository(private val api: AladhanApi) {

    suspend fun getPrayerTimes(lat: Double, lng: Double): Call<PrayerResponse> {
        // Buat tanggal hari ini dengan format dd-MM-yyyy
        val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

        // Masukkan tanggal ke request
        return api.getTimings(today, lat, lng,20)
    }
}