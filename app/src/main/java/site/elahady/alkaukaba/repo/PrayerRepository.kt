package site.elahady.alkaukaba.repo

import android.content.Context
import site.elahady.alkaukaba.api.AladhanApi
import site.elahady.alkaukaba.api.CalendarResponse
import site.elahady.alkaukaba.api.PrayerResponse
import site.elahady.alkaukaba.api.PrayerTimeResponse
import site.elahady.alkaukaba.utils.PrayerCalculationMethods
import site.elahady.alkaukaba.utils.SessionManager
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrayerRepository(private val api: AladhanApi, context: Context) {

    companion object {
        // Method Aladhan (Kemenag RI) yang dipakai sebagai mesin hitung sementara untuk
        // Ephemeris (Al Hasib), selama engine hisab manualnya belum ada di app ini.
        private const val EPHEMERIS_FALLBACK_ALADHAN_METHOD_ID = 20
    }

    private val sessionManager = SessionManager(context.applicationContext)

    private fun currentMethod(): Int {
        val selected = sessionManager.getPrayerMethodId()
        return if (selected == PrayerCalculationMethods.EPHEMERIS_ID) EPHEMERIS_FALLBACK_ALADHAN_METHOD_ID else selected
    }

    private fun currentMethodSettings(): String? = sessionManager.getMethodSettingsQuery()

    /** Id metode yang benar-benar dipilih user (mis. id Ephemeris), beda dari [currentMethod]
     *  yang bisa jadi sudah di-fallback-kan ke method Aladhan asli. Dipakai UI untuk
     *  keputusan yang sifatnya tampilan, mis. cari breakdown perhitungan lewat
     *  PrayerCalculationBreakdownRegistry. */
    fun getSelectedMethodId(): Int = sessionManager.getPrayerMethodId()

    suspend fun getPrayerTimes(lat: Double, lng: Double): Response<PrayerResponse> {
        val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        return api.getTimings(today, lat, lng, currentMethod(), currentMethodSettings())
    }

    suspend fun getIslamicHolidays(lat: Double, lng: Double, month: Int, year: Int): Response<CalendarResponse> {
        return api.getCalendar(lat, lng, currentMethod(), month, year, currentMethodSettings())
    }

    suspend fun getTimingPrayers(lat: Double, lng: Double, date: String): Response<PrayerTimeResponse> {
        return api.getTimingPrayers(lat, lng, currentMethod(), date, currentMethodSettings())
    }
}
