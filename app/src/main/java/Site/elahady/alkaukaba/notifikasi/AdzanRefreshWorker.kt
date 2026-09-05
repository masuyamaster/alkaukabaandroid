package site.elahady.alkaukaba.notifikasi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import site.elahady.alkaukaba.api.RetrofitClient
import site.elahady.alkaukaba.repo.PrayerRepository
import site.elahady.alkaukaba.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

/** Ambil jadwal sholat hari ini via [PrayerRepository] lalu jadwalkan alarm lewat [AdzanScheduler]. */
class AdzanRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val UNIQUE_WORK_NAME = "adzan_refresh_periodic"
        const val UNIQUE_WORK_NAME_IMMEDIATE = "adzan_refresh_immediate"

        // Default Jakarta - sama dengan fallback yang sudah dipakai MainActivity saat izin lokasi ditolak.
        private const val DEFAULT_LAT = -6.2088
        private const val DEFAULT_LNG = 106.8456
    }

    override suspend fun doWork(): Result {
        return try {
            val (lat, lng) = resolveLocation()
            val repository = PrayerRepository(RetrofitClient.instance, applicationContext)
            val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            val response = repository.getTimingPrayers(lat, lng, today)
            val timings = response.body()?.data?.timings

            if (response.isSuccessful && timings != null) {
                AdzanScheduler.scheduleFromTimings(applicationContext, timings)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("AdzanRefreshWorker", "Gagal refresh jadwal adzan", e)
            Result.retry()
        }
    }

    private suspend fun resolveLocation(): Pair<Double, Double> {
        val sessionManager = SessionManager(applicationContext)
        if (sessionManager.isManualLocationMode()) {
            return sessionManager.getManualLat() to sessionManager.getManualLng()
        }
        val location = getLastLocationOrNull()
        return if (location != null) location.latitude to location.longitude else DEFAULT_LAT to DEFAULT_LNG
    }

    private suspend fun getLastLocationOrNull(): Location? {
        val hasFine = ActivityCompat.checkSelfPermission(
            applicationContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ActivityCompat.checkSelfPermission(
            applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return null

        return suspendCancellableCoroutine { cont ->
            try {
                LocationServices.getFusedLocationProviderClient(applicationContext).lastLocation
                    .addOnSuccessListener { location -> cont.resume(location) }
                    .addOnFailureListener { cont.resume(null) }
            } catch (e: SecurityException) {
                cont.resume(null)
            }
        }
    }
}
