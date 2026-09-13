package site.elahady.alkaukaba.utils

import site.elahady.alkaukaba.model.OccultationItem
import site.elahady.alkaukaba.model.OccultationResult
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.KM_PER_AU
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.search
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.sqrt

/**
 * Mesin hisab okultasi benda langit oleh Bulan, berbasis "Astronomy Engine"
 * (utils/Astronomy.kt) yang sama dipakai [EclipseCalculator]. Berbeda dari
 * gerhana, library ini tidak punya fungsi pencarian okultasi siap pakai,
 * jadi dicari manual: telusuri waktu jarak sudud Bulan-Venus (topocentric,
 * supaya paralaks Bulan yang besar ikut terhitung) mencapai minimum lokal
 * (konjungsi), lalu tentukan apakah piringan Venus benar-benar tertutup
 * piringan Bulan pada momen itu (okultasi nyata) atau cuma lewat dekat.
 *
 * Baru mendukung Venus - body lain bisa ditambah lewat [findEvents] dengan
 * radius fisik body tersebut.
 */
object OccultationCalculator {

    private const val EVENT_COUNT = 3
    private const val SEARCH_HORIZON_DAYS = 730.0 // 2 tahun, jaga-jaga kalau konjungsi dekat langka
    private const val COARSE_STEP_DAYS = 0.25 // 6 jam - cukup rapat untuk menangkap satu konjungsi bulanan
    private const val DERIVATIVE_EPS_DAYS = 0.02 // ~29 menit, untuk turunan numerik jarak sudut
    private const val CONTACT_WINDOW_DAYS = 0.25 // jendela pencarian kontak awal/akhir di sekitar puncak

    private const val VENUS_RADIUS_KM = 6051.8
    private const val MOON_RADIUS_KM = 1737.4

    fun calculate(latitude: Double, longitude: Double, heightMeters: Double): OccultationResult {
        val observer = Observer(latitude, longitude, heightMeters)
        val now = Time.fromMillisecondsSince1970(System.currentTimeMillis())
        val venusItems = findEvents(Body.Venus, "Venus", VENUS_RADIUS_KM, observer, now)
        return OccultationResult(venusItems)
    }

    private fun findEvents(
        body: Body,
        bodyLabel: String,
        bodyRadiusKm: Double,
        observer: Observer,
        startTime: Time
    ): List<OccultationItem> {
        val items = mutableListOf<OccultationItem>()
        val horizonEnd = startTime.addDays(SEARCH_HORIZON_DAYS)

        var prevT = startTime
        var prevSlope = separationSlope(body, observer, prevT)
        var t = startTime.addDays(COARSE_STEP_DAYS)

        while (t.ut < horizonEnd.ut && items.size < EVENT_COUNT) {
            val slope = separationSlope(body, observer, t)
            if (prevSlope < 0.0 && slope >= 0.0) {
                // Turunan jarak sudud berubah negatif -> positif berarti ada minimum lokal
                // (konjungsi Bulan-Venus) di antara prevT dan t.
                val conjunctionTime = search(prevT, t, 1.0) { time -> separationSlope(body, observer, time) }
                if (conjunctionTime != null) {
                    items.add(buildItem(body, bodyLabel, bodyRadiusKm, observer, conjunctionTime))
                }
            }
            prevT = t
            prevSlope = slope
            t = t.addDays(COARSE_STEP_DAYS)
        }
        return items
    }

    private fun separationSlope(body: Body, observer: Observer, time: Time): Double {
        val sepMinus = topocentricSeparationDeg(body, observer, time.addDays(-DERIVATIVE_EPS_DAYS))
        val sepPlus = topocentricSeparationDeg(body, observer, time.addDays(DERIVATIVE_EPS_DAYS))
        return sepPlus - sepMinus
    }

    private fun topocentricSeparationDeg(body: Body, observer: Observer, time: Time): Double {
        val moonEq = equator(Body.Moon, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val bodyEq = equator(body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        return angularSeparationDeg(moonEq.vec, bodyEq.vec)
    }

    private fun angularSeparationDeg(a: Vector, b: Vector): Double {
        val dot = a.x * b.x + a.y * b.y + a.z * b.z
        val cosAngle = (dot / (a.length() * b.length())).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosAngle))
    }

    private fun angularRadiusDeg(radiusKm: Double, distanceAu: Double): Double {
        val distanceKm = distanceAu * KM_PER_AU
        return Math.toDegrees(atan(radiusKm / sqrt(distanceKm * distanceKm - radiusKm * radiusKm)))
    }

    // Selisih (jarak sudud pusat-ke-pusat) - (jumlah jari-jari sudud Bulan & body).
    // Negatif berarti kedua piringan sedang bertumpuk (okultasi berlangsung).
    private fun contactGap(body: Body, bodyRadiusKm: Double, observer: Observer, time: Time): Double {
        val moonEq = equator(Body.Moon, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val bodyEq = equator(body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val sep = angularSeparationDeg(moonEq.vec, bodyEq.vec)
        val limbSum = angularRadiusDeg(MOON_RADIUS_KM, moonEq.dist) + angularRadiusDeg(bodyRadiusKm, bodyEq.dist)
        return sep - limbSum
    }

    private fun buildItem(
        body: Body,
        bodyLabel: String,
        bodyRadiusKm: Double,
        observer: Observer,
        peakTime: Time
    ): OccultationItem {
        val moonEq = equator(Body.Moon, peakTime, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val bodyEq = equator(body, peakTime, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val minSeparationDeg = angularSeparationDeg(moonEq.vec, bodyEq.vec)
        val moonRadiusDeg = angularRadiusDeg(MOON_RADIUS_KM, moonEq.dist)
        val bodyRadiusDeg = angularRadiusDeg(bodyRadiusKm, bodyEq.dist)
        val isOccultation = minSeparationDeg < (moonRadiusDeg + bodyRadiusDeg)
        val bodyHor = horizon(peakTime, observer, bodyEq.ra, bodyEq.dec, Refraction.Normal)

        var ingressLabel: String? = null
        var egressLabel: String? = null
        if (isOccultation) {
            val windowStart = peakTime.addDays(-CONTACT_WINDOW_DAYS)
            val windowEnd = peakTime.addDays(CONTACT_WINDOW_DAYS)

            // contactGap turun dari positif (belum kontak) ke negatif (tertutup) saat masuk;
            // dibalik tandanya supaya search() (pencari akar menaik) bisa dipakai untuk keduanya.
            val ingressTime = search(windowStart, peakTime, 1.0) { time -> -contactGap(body, bodyRadiusKm, observer, time) }
            val egressTime = search(peakTime, windowEnd, 1.0) { time -> contactGap(body, bodyRadiusKm, observer, time) }
            ingressLabel = ingressTime?.let { formatLocalTime(it) }
            egressLabel = egressTime?.let { formatLocalTime(it) }
        }

        return OccultationItem(
            bodyLabel = bodyLabel,
            eventDateLabel = formatLocalDate(peakTime),
            isOccultation = isOccultation,
            peakTimeLabel = formatLocalTime(peakTime),
            ingressTimeLabel = ingressLabel,
            egressTimeLabel = egressLabel,
            minSeparationArcmin = minSeparationDeg * 60.0,
            visibleFromLocation = bodyHor.altitude > 0.0
        )
    }

    private fun formatLocalTime(time: Time): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale("id", "ID"))
        return sdf.format(Date(time.toMillisecondsSince1970()))
    }

    private fun formatLocalDate(time: Time): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        return sdf.format(Date(time.toMillisecondsSince1970()))
    }
}
