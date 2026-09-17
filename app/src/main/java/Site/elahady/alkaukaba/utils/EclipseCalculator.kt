package site.elahady.alkaukaba.utils

import site.elahady.alkaukaba.model.GerhanaResult
import site.elahady.alkaukaba.model.LunarEclipseItem
import site.elahady.alkaukaba.model.SolarEclipseItem
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EclipseKind
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.GlobalSolarEclipseInfo
import io.github.cosinekitty.astronomy.LocalSolarEclipseInfo
import io.github.cosinekitty.astronomy.LunarEclipseInfo
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.globalSolarEclipsesAfter
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.lunarEclipsesAfter
import io.github.cosinekitty.astronomy.searchLocalSolarEclipse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Mesin hisab gerhana, berbasis "Astronomy Engine" (utils/Astronomy.kt) yang
 * sudah dipakai fitur Bulan Hijriyah. Gerhana Bulan maupun gerhana Matahari
 * dicari secara **global** (lunarEclipsesAfter / globalSolarEclipsesAfter) —
 * bukan lewat varian pencarian per-lokasi (localSolarEclipsesAfter) — supaya
 * semua event ke depan tetap tampil di daftar walau tidak terlihat sama
 * sekali dari markaz yang sedang dipakai; visibilitas lokal dihitung/ditandai
 * terpisah per item. Lihat docs/features/gerhana.md §5 untuk detail alasan.
 */
object EclipseCalculator {

    private const val EVENT_COUNT = 5

    // Gerhana Matahari beruntun (new moon ke new moon) berjarak >=29 hari, jadi toleransi 3
    // hari ini aman untuk membedakan "sirkumstansi lokal dari event global yang sama" vs
    // "ketemu event lokal lain yang lebih belakangan" (lihat matchLocalCircumstance()).
    private const val LOCAL_MATCH_TOLERANCE_DAYS = 3.0

    /**
     * Titik acuan kasar untuk keterangan "terlihat dari wilayah mana" per event (bukan markaz
     * user) — 9 wilayah makro dunia, satu titik representatif per wilayah, BUKAN grid rapat ala
     * `WorldVisibilityCalculator` (yang butuh ratusan titik x kalkulasi penuh per titik, terlalu
     * lambat untuk 10 event gerhana sekaligus di layar ini). Ini aproksimasi kasar: satu wilayah
     * ditandai "terlihat" kalau titik representatifnya melihat puncak gerhana, walau jalur
     * gerhana sebenarnya (terutama Matahari, lebar totalitas cuma puluhan-ratusan km) bisa saja
     * cuma menyentuh sebagian kecil wilayah itu atau meleset tipis dari titik representatifnya.
     */
    private data class Region(val label: String, val lat: Double, val lng: Double)

    private val VISIBILITY_REGIONS = listOf(
        Region("Indonesia & Asia Tenggara", -2.5, 118.0),
        Region("Asia Timur", 35.0, 105.0),
        Region("Asia Selatan", 20.0, 78.0),
        Region("Timur Tengah", 25.0, 45.0),
        Region("Eropa", 50.0, 10.0),
        Region("Afrika", 5.0, 20.0),
        Region("Amerika Utara", 40.0, -100.0),
        Region("Amerika Selatan", -15.0, -60.0),
        Region("Australia & Oseania", -25.0, 140.0)
    )

    fun calculate(latitude: Double, longitude: Double, heightMeters: Double): GerhanaResult {
        val observer = Observer(latitude, longitude, heightMeters)
        val now = Time.fromMillisecondsSince1970(System.currentTimeMillis())

        val lunarEclipses = lunarEclipsesAfter(now)
            .take(EVENT_COUNT)
            .map { toLunarItem(it, observer) }
            .toList()

        val solarEclipses = globalSolarEclipsesAfter(now)
            .take(EVENT_COUNT)
            .map { toSolarItem(it, observer) }
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
            visibleFromLocation = moonHor.altitude > 0.0,
            visibleRegions = visibleRegionsForLunar(info.peak)
        )
    }

    private fun visibleRegionsForLunar(peak: Time): List<String> = VISIBILITY_REGIONS.filter { region ->
        val regionObserver = Observer(region.lat, region.lng, 0.0)
        val moonEq = equator(Body.Moon, peak, regionObserver, EquatorEpoch.OfDate, Aberration.Corrected)
        val moonHor = horizon(peak, regionObserver, moonEq.ra, moonEq.dec, Refraction.Normal)
        moonHor.altitude > 0.0
    }.map { it.label }

    private fun toSolarItem(info: GlobalSolarEclipseInfo, observer: Observer): SolarEclipseItem {
        val visibleRegions = visibleRegionsForSolar(info)
        val local = matchLocalCircumstance(info, observer)
        if (local != null) {
            return SolarEclipseItem(
                kindLabel = solarKindLabel(info.kind),
                peakDateLabel = formatLocalDate(info.peak),
                partialBeginLabel = formatLocalTime(local.partialBegin.time),
                peakTimeLabel = formatLocalTime(local.peak.time),
                partialEndLabel = formatLocalTime(local.partialEnd.time),
                totalBeginLabel = local.totalBegin?.let { formatLocalTime(it.time) },
                totalEndLabel = local.totalEnd?.let { formatLocalTime(it.time) },
                magnitudePercent = local.obscuration * 100.0,
                visibleFromLocation = local.peak.altitude > 0.0,
                visibleRegions = visibleRegions
            )
        }

        // Tidak ada sirkumstansi lokal (event ini tidak terlihat sama sekali dari markaz) ->
        // tetap tampilkan event-nya (waktu puncak global + jenis), tapi tanpa rincian jam lokal.
        // Magnitude gerhana Sebagian tanpa titik observasi memang undefined di Astronomy
        // Engine (lihat KDoc GlobalSolarEclipseInfo.obscuration), jadi dibiarkan null.
        return SolarEclipseItem(
            kindLabel = solarKindLabel(info.kind),
            peakDateLabel = formatLocalDate(info.peak),
            partialBeginLabel = null,
            peakTimeLabel = formatLocalTime(info.peak),
            partialEndLabel = null,
            totalBeginLabel = null,
            totalEndLabel = null,
            magnitudePercent = if (info.kind == EclipseKind.Partial) null else info.obscuration * 100.0,
            visibleFromLocation = false,
            visibleRegions = visibleRegions
        )
    }

    /** Sama seperti [matchLocalCircumstance] per markaz user, tapi dites ke 9 [VISIBILITY_REGIONS]
     *  sekaligus -> daftar nama wilayah yang puncak gerhananya di atas ufuk (kriteria sama persis
     *  dengan `visibleFromLocation`: `peak.altitude > 0.0`, bukan cuma sirkumstansi parsial). */
    private fun visibleRegionsForSolar(info: GlobalSolarEclipseInfo): List<String> = VISIBILITY_REGIONS
        .filter { region ->
            val regionObserver = Observer(region.lat, region.lng, 0.0)
            matchLocalCircumstance(info, regionObserver)?.peak?.altitude?.let { it > 0.0 } ?: false
        }
        .map { it.label }

    /**
     * Cocokkan gerhana Matahari global dengan hasil `searchLocalSolarEclipse` pada bulan baru
     * yang sama (toleransi [LOCAL_MATCH_TOLERANCE_DAYS]). `searchLocalSolarEclipse` melompat ke
     * event berikutnya kalau event saat ini sama sekali tidak terlihat dari observer (lihat
     * implementasinya di Astronomy.kt) -> kalau hasil pencariannya bukan event yang sama, berarti
     * memang tidak ada sirkumstansi lokal untuk event global ini di markaz tersebut.
     */
    private fun matchLocalCircumstance(info: GlobalSolarEclipseInfo, observer: Observer): LocalSolarEclipseInfo? {
        val candidate = searchLocalSolarEclipse(info.peak.addDays(-LOCAL_MATCH_TOLERANCE_DAYS), observer)
        return if (abs(candidate.peak.time.ut - info.peak.ut) < LOCAL_MATCH_TOLERANCE_DAYS) candidate else null
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
