package Site.elahady.alkaukaba.api

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Response // Gunakan Response, BUKAN Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class PrayerResponse(val code: Int, val status: String, val data: PrayerData)
data class PrayerData(val timings: Timings, val date: DateInfo, val meta: Meta)

data class PrayerTimeResponse(val code: Int, val status: String, val data: PrayerTimeData)
data class PrayerTimeData(val timings: TimingPrayers, val date: DateInfo, val meta: Meta)

data class Timings(val Fajr: String, val Dhuhr: String, val Asr: String, val Maghrib: String, val Isha: String)

data class DateInfo(val readable: String, val hijri: Hijri)
data class Hijri(val day: String, val month: HijriMonth, val year: String, val holidays: List<String>)
data class HijriMonth(val en: String, val ar: String)
data class Meta(val timezone: String)
data class CalendarResponse(val code: Int,val status: String,val data: List<PrayerData>)

data class TimingPrayers(
    @SerializedName("Imsak") val imsak: String,
    @SerializedName("Fajr") val subuh: String,
    @SerializedName("Dhuhr") val dzuhur: String,
    @SerializedName("Asr") val ashar: String,
    @SerializedName("Maghrib") val maghrib: String,
    @SerializedName("Isha") val isya: String
)

data class Method(
    val name: String // Untuk memastikan Method 20 (Kemenag)
)

data class QiblaResponse(
    val data: QiblaData
)

data class QiblaData(
    val latitude: Double,
    val longitude: Double,
    val direction: Double
)

interface AladhanApi {
    @GET("v1/timings/{date}")
    suspend fun getTimings(
        @Path("date") date: String,
        @Query("latitude") lat: Double,
        @Query("longitude") lng: Double,
        @Query("method") method: Int = 20
    ): Response<PrayerResponse>

    @GET("v1/calendar")
    suspend fun getCalendar(
        @Query("latitude") lat: Double,
        @Query("longitude") lng: Double,
        @Query("method") method: Int = 20,
        @Query("month") month: Int,
        @Query("year") year: Int
    ): Response<CalendarResponse>

    @GET("v1/qibla/{latitude}/{longitude}")
    suspend fun getQiblaDirection(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double
    ): QiblaResponse

    @GET("v1/timings")
    fun getTimingPrayers(
        @Query("latitude") lat: Double,
        @Query("longitude") long: Double,
        @Query("method") method: Int = 20,
        @Query("date") date: String
    ): Call<PrayerTimeResponse>

}

object RetrofitClient {
    private const val BASE_URL = "https://api.aladhan.com/"

    val instance: AladhanApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(AladhanApi::class.java)
    }
}
