package site.elahady.alkaukaba.utils

import site.elahady.alkaukaba.model.GerhanaResult
import site.elahady.alkaukaba.model.LunarEclipseItem
import site.elahady.alkaukaba.model.SolarEclipseItem
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EclipseKind
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.LocalSolarEclipseInfo
import io.github.cosinekitty.astronomy.LunarEclipseInfo
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.localSolarEclipsesAfter
import io.github.cosinekitty.astronomy.lunarEclipsesAfter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Mesin hisab gerhana, berbasis "Astronomy Engine" (utils/Astronomy.kt) yang
 * sudah dipakai fitur Bulan Hijriyah. Gerhana Bulan dicari secara global
 * (lunarEclipsesAfter) lalu visibilitas lokal dihitung manual (altitude Bulan
 * saat puncak); gerhana Matahari dicari langsung per-lokasi
 * (localSolarEclipsesAfter) karena Astronomy Engine sudah punya varian local
 * untuk itu.
 */
object EclipseCalculator {

    private const val EVENT_COUNT = 5

    fun calculate(latitude: Double, longitude: Double, heightMeters: Double): GerhanaResult {
        val observer = Observer(latitude, longitude, heightMeters)
        val now = Time.fromMillisecondsSince1970(System.currentTimeMillis())

        val lunarEclipses = lunarEclipsesAfter(now)
            .take(EVENT_COUNT)
            .map { toLunarItem(it, observer) }
            .toList()

        val solarEclipses = localSolarEclipsesAfter(now, observer)
            .take(EVENT_COUNT)
            .map { toSolarItem(it) }
            .toList()

        return GerhanaResult(lunarEclipses, solarEclipses)
    }

    private fun toLunarItem(info: LunarEclipseInfo, observer: Observer): LunarEclipseItem {
        val moonEq = equator(Body.Moon, info.peak, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val moonHor = horizon(info.peak, observer, moonEq.ra, moonEq.dec, Refraction.Normal)
        return LunarEclipseItem(
            kindLabel = lunarKindLabel(info.kind),
            peakDateLabel = formatLocalDate(info.peak),
            peakTimeLabel = formatLocalTime(info.peak),
            magnitudePercent = info.obscuration * 100.0,
            visibleFromLocation = moonHor.altitude > 0.0
        )
    }

    private fun toSolarItem(info: LocalSolarEclipseInfo): SolarEclipseItem {
        return SolarEclipseItem(
            kindLabel = solarKindLabel(info.kind),
            peakDateLabel = formatLocalDate(info.peak.time),
            partialBeginLabel = formatLocalTime(info.partialBegin.time),
            peakTimeLabel = formatLocalTime(info.peak.time),
            partialEndLabel = formatLocalTime(info.partialEnd.time),
            totalBeginLabel = info.totalBegin?.let { formatLocalTime(it.time) },
            totalEndLabel = info.totalEnd?.let { formatLocalTime(it.time) },
            magnitudePercent = info.obscuration * 100.0,
            visibleFromLocation = info.peak.altitude > 0.0
        )
    }

    private fun lunarKindLabel(kind: EclipseKind): String = when (kind) {
        EclipseKind.Penumbral -> "Penumbra"
        EclipseKind.Partial -> "Sebagian"
        EclipseKind.Total -> "Total"
        EclipseKind.Annular -> "Total" // tidak pernah terjadi untuk gerhana Bulan
    }

    private fun solarKindLabel(kind: EclipseKind): String = when (kind) {
        EclipseKind.Partial -> "Sebagian"
        EclipseKind.Annular -> "Cincin"
        EclipseKind.Total -> "Total"
        EclipseKind.Penumbral -> "Sebagian" // tidak pernah terjadi untuk gerhana Matahari
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
