package Site.elahady.alkaukaba.api

import retrofit2.Response // Gunakan Response, BUKAN Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class PrayerResponse(val code: Int, val status: String, val data: PrayerData)
data class PrayerData(val timings: Timings, val date: DateInfo, val meta: Meta)
data class Timings(val Fajr: String, val Dhuhr: String, val Asr: String, val Maghrib: String, val Isha: String)
data class DateInfo(val readable: String, val hijri: Hijri)
data class Hijri(val day: String, val month: HijriMonth, val year: String, val holidays: List<String>)
data class HijriMonth(val en: String, val ar: String)
data class Meta(val timezone: String)
data class CalendarResponse(val code: Int,val status: String,val data: List<PrayerData>)

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