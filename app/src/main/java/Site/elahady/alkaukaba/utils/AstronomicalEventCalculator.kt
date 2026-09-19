package site.elahady.alkaukaba.utils

import site.elahady.alkaukaba.api.HolidayItem
import site.elahady.alkaukaba.model.AstronomicalEvent
import site.elahady.alkaukaba.model.AstronomiKategori
import site.elahady.alkaukaba.model.EventJenis
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EclipseKind
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.globalSolarEclipsesAfter
import io.github.cosinekitty.astronomy.lunarEclipsesAfter
import io.github.cosinekitty.astronomy.searchHourAngle
import io.github.cosinekitty.astronomy.searchMoonPhase
import io.github.cosinekitty.astronomy.searchRelativeLongitude
import io.github.cosinekitty.astronomy.searchSunLongitude
import io.github.cosinekitty.astronomy.seasons
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Mesin hisab "Event Besar" fenomena astronomi, berbasis "Astronomy Engine"
 * (utils/Astronomy.kt) yang sama dipakai [EclipseCalculator] dan [OccultationCalculator].
 * Semua waktu dihitung lokal di perangkat (tanpa API), jadi tetap jalan offline.
 *
 * Cakupan: Hari Tanpa Bayangan (per koordinat), ekuinoks & solstis, Purnama Panen (Harvest
 * Moon), oposisi planet luar, puncak hujan meteor utama, dan gerhana (global — keterlihatan
 * per lokasi ada di menu Gerhana).
 *
 * Okultasi Venus oleh Bulan **tidak** digabung ke sini: [OccultationCalculator] memindai
 * konjungsi tiap 6 jam dengan paralaks topocentric, terlalu berat untuk dijalankan tiap kali
 * beranda dibuka. Fenomena itu tetap ada di menu Okultasi.
 */
object AstronomicalEventCalculator {

    private const val DAY_MS = 86_400_000L

    /**
     * Ambang jarak zenit Matahari (derajat) saat kulminasi untuk dianggap "tanpa bayangan":
     * setara jari-jari sudut piringan Matahari (~0,27°). Deklinasi Matahari berubah paling
     * cepat ~0,4°/hari di sekitar ekuinoks, jadi hari terdekat ke titik potong paling jauh
     * meleset ~0,2° — masih di bawah ambang ini. Di lintang yang Matahari tidak pernah
     * mencapai zenit (di luar ~23,7°) jarak zenit minimum selalu di atas ambang, jadi event
     * tidak muncul.
     */
    private const val ZENITH_TOLERANCE_DEG = 0.27

    private const val TROPICAL_YEAR_DAYS = 365.2422

    private class MeteorShower(val name: String, val sunLongitude: Double, val info: String)

    /**
     * Puncak hujan meteor utama dalam bujur matahari (λ☉, ekuinoks J2000) — nilai rujukan
     * kalender IMO 2026, jadi tanggalnya dihitung dari posisi Matahari (stabil antar-tahun),
     * bukan tanggal hardcode. Waktu puncak sebenarnya bisa meleset dari perkiraan ini.
     */
    private val METEOR_SHOWERS = listOf(
        MeteorShower("Quadrantid", 283.15, "induk: asteroid 2003 EH1"),
        MeteorShower("Lyrid", 32.32, "induk: komet Thatcher"),
        MeteorShower("Eta Aquarid", 45.5, "induk: komet Halley"),
        MeteorShower("Perseid", 140.0, "induk: komet Swift-Tuttle"),
        MeteorShower("Sextantid (siang hari)", 188.0, "puncaknya tidak pasti menurut IMO"),
        MeteorShower("Draconid", 195.4, "induk: komet Giacobini-Zinner"),
        MeteorShower("Orionid", 208.0, "induk: komet Halley"),
        MeteorShower("Taurid Selatan", 223.0, "induk: komet Encke"),
        MeteorShower("Taurid Utara", 230.0, "induk: komet Encke"),
        MeteorShower("Leonid", 235.27, "induk: komet Tempel-Tuttle"),
        MeteorShower("Geminid", 262.2, "induk: asteroid 3200 Phaethon"),
        MeteorShower("Ursid", 270.7, "induk: komet Tuttle")
    )

    private class OuterPlanet(val body: Body, val name: String, val info: String)

    private val OUTER_PLANETS = listOf(
        OuterPlanet(Body.Mars, "Mars", "berlawanan arah dengan Matahari: paling terang, tampak sepanjang malam"),
        OuterPlanet(Body.Jupiter, "Jupiter", "berlawanan arah dengan Matahari: paling terang, tampak sepanjang malam"),
        OuterPlanet(Body.Saturn, "Saturnus", "berlawanan arah dengan Matahari: paling terang, tampak sepanjang malam"),
        OuterPlanet(Body.Uranus, "Uranus", "berlawanan arah dengan Matahari; di batas mata telanjang, lebih mudah dengan binokular/teleskop"),
        OuterPlanet(Body.Neptune, "Neptunus", "berlawanan arah dengan Matahari; tidak terlihat mata telanjang, butuh teleskop")
    )

    /**
     * Hitung semua event astronomi dengan waktu puncak di [startMillis] (inklusif) sampai
     * [endMillis] (eksklusif), urut menurut waktu. [latitude]/[longitude]/[heightMeters]
     * hanya mempengaruhi Hari Tanpa Bayangan; event lain berlaku global.
     */
    fun calculate(
        latitude: Double,
        longitude: Double,
        heightMeters: Double,
        startMillis: Long,
        endMillis: Long,
        zone: TimeZone = TimeZone.getDefault()
    ): List<AstronomicalEvent> {
        val observer = Observer(latitude, longitude, heightMeters)
        val startYear = utcYear(startMillis)
        val endYear = utcYear(endMillis)

        val events = mutableListOf<AstronomicalEvent>()
        for (year in startYear..endYear) {
            events += seasonEvents(year, zone)
            events += harvestMoonEvent(year, zone)
        }
        events += shadowlessDayEvents(observer, startMillis, endMillis, zone)
        events += outerPlanetOppositionEvents(startMillis, endMillis, zone)
        events += meteorShowerEvents(startYear - 1, endYear)
        events += eclipseEvents(startMillis, endMillis, zone)

        return events
            .filter { it.epochMillis in startMillis until endMillis }
            .sortedBy { it.epochMillis }
    }

    private fun seasonEvents(year: Int, zone: TimeZone): List<AstronomicalEvent> {
        val s = seasons(year)
        return listOf(
            AstronomicalEvent(
                s.marchEquinox.toMillisecondsSince1970(), AstronomiKategori.EKUINOKS_SOLSTIS,
                "Ekuinoks Maret",
                "Pukul ${clock(s.marchEquinox, zone)}. Matahari tepat di atas ekuator; siang dan malam hampir sama panjang."
            ),
            AstronomicalEvent(
                s.juneSolstice.toMillisecondsSince1970(), AstronomiKategori.EKUINOKS_SOLSTIS,
                "Solstis Juni",
                "Pukul ${clock(s.juneSolstice, zone)}. Matahari di titik paling utara (deklinasi sekitar +23,4°)."
            ),
            AstronomicalEvent(
                s.septemberEquinox.toMillisecondsSince1970(), AstronomiKategori.EKUINOKS_SOLSTIS,
                "Ekuinoks September",
                "Pukul ${clock(s.septemberEquinox, zone)}. Matahari tepat di atas ekuator; siang dan malam hampir sama panjang."
            ),
            AstronomicalEvent(
                s.decemberSolstice.toMillisecondsSince1970(), AstronomiKategori.EKUINOKS_SOLSTIS,
                "Solstis Desember",
                "Pukul ${clock(s.decemberSolstice, zone)}. Matahari di titik paling selatan (deklinasi sekitar −23,4°)."
            )
        )
    }

    /** Purnama terdekat dengan ekuinoks September (istilah Purnama Panen / Harvest Moon). */
    private fun harvestMoonEvent(year: Int, zone: TimeZone): List<AstronomicalEvent> {
        val equinox = seasons(year).septemberEquinox
        // Jendela 30 hari > periode sinodis (29,27–29,83 hari) menjamin minimal satu purnama.
        val first = searchMoonPhase(180.0, equinox.addDays(-15.0), 30.0) ?: return emptyList()
        val second = searchMoonPhase(180.0, first.addDays(1.0), 30.0)
        val nearest = listOfNotNull(first, second).minByOrNull { abs(it.ut - equinox.ut) } ?: return emptyList()
        return listOf(
            AstronomicalEvent(
                nearest.toMillisecondsSince1970(), AstronomiKategori.PURNAMA,
                "Purnama Panen (Harvest Moon)",
                "Pukul ${clock(nearest, zone)}. Purnama yang paling dekat dengan ekuinoks September."
            )
        )
    }

    private class TransitSample(val timeMillis: Long, val zenithDistanceDeg: Double)

    /**
     * Hari Tanpa Bayangan: hari saat Matahari berkulminasi tepat di zenit — terjadi ketika
     * deklinasi Matahari sama dengan lintang pengamat, jadi hanya ada di lintang tropis
     * (dua kali setahun, atau sekali kalau lintangnya tepat di batas Garis Balik).
     *
     * Tiap hari lokal disampel pada saat kulminasi (transit meridian), lalu dicari hari
     * dengan jarak zenit minimum lokal di bawah [ZENITH_TOLERANCE_DEG]. Satu hari sampel
     * ekstra di kiri-kanan jendela dipakai supaya minimum di tepi jendela tetap terdeteksi.
     */
    private fun shadowlessDayEvents(
        observer: Observer,
        startMillis: Long,
        endMillis: Long,
        zone: TimeZone
    ): List<AstronomicalEvent> {
        if (abs(observer.latitude) > 24.0) return emptyList()

        val cal = Calendar.getInstance(zone)
        cal.timeInMillis = startMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, -1)

        val samples = mutableListOf<TransitSample>()
        while (cal.timeInMillis < endMillis + DAY_MS) {
            samples += sampleSolarTransit(observer, cal.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val events = mutableListOf<AstronomicalEvent>()
        for (i in 1 until samples.size - 1) {
            val s = samples[i]
            val isLocalMinimum = s.zenithDistanceDeg <= samples[i - 1].zenithDistanceDeg &&
                s.zenithDistanceDeg < samples[i + 1].zenithDistanceDeg
            if (isLocalMinimum && s.zenithDistanceDeg <= ZENITH_TOLERANCE_DEG) {
                events += AstronomicalEvent(
                    s.timeMillis, AstronomiKategori.HARI_TANPA_BAYANGAN,
                    "Hari Tanpa Bayangan (Kulminasi Matahari)",
                    "Matahari tepat di atas kepala pukul ${clock(s.timeMillis, zone)} " +
                        "di lokasi Anda; benda tegak nyaris tanpa bayangan."
                )
            }
        }
        return events
    }

    private fun sampleSolarTransit(observer: Observer, fromMillis: Long): TransitSample {
        val transit = searchHourAngle(Body.Sun, observer, 0.0, Time.fromMillisecondsSince1970(fromMillis)).time
        val dec = equator(Body.Sun, transit, observer, EquatorEpoch.OfDate, Aberration.Corrected).dec
        return TransitSample(transit.toMillisecondsSince1970(), abs(observer.latitude - dec))
    }

    private fun outerPlanetOppositionEvents(
        startMillis: Long,
        endMillis: Long,
        zone: TimeZone
    ): List<AstronomicalEvent> {
        val events = mutableListOf<AstronomicalEvent>()
        val start = Time.fromMillisecondsSince1970(startMillis)
        for (planet in OUTER_PLANETS) {
            // Periode sinodis planet luar >= 367 hari, jadi paling banyak satu oposisi per
            // tahun; loop dijaga supaya tetap benar kalau jendela pemanggil lebih panjang.
            var time = searchRelativeLongitude(planet.body, 0.0, start)
            while (time.toMillisecondsSince1970() < endMillis) {
                events += AstronomicalEvent(
                    time.toMillisecondsSince1970(), AstronomiKategori.OPOSISI_PLANET,
                    "Oposisi ${planet.name}",
                    "Pukul ${clock(time, zone)}. ${planet.name} ${planet.info}."
                )
                time = searchRelativeLongitude(planet.body, 0.0, time.addDays(1.0))
            }
        }
        return events
    }

    /**
     * Cari tanggal Matahari mencapai λ☉ puncak tiap hujan meteor. Perkiraan awalnya linear
     * dari ekuinoks Maret (λ☉ = 0°) — meleset paling jauh ~2 hari karena kecepatan Matahari
     * di ekliptika tidak seragam — lalu dipoles [searchSunLongitude] dalam jendela ±6 hari.
     *
     * [fromBaseYear] mulai dari satu tahun sebelum jendela: dihitung dari ekuinoks Maret tahun
     * dasar, λ☉ ≥ 270° (mis. Quadrantid, awal Januari) jatuh di tahun kalender berikutnya.
     */
    private fun meteorShowerEvents(fromBaseYear: Int, toBaseYear: Int): List<AstronomicalEvent> {
        val events = mutableListOf<AstronomicalEvent>()
        for (baseYear in fromBaseYear..toBaseYear) {
            val marchEquinox = seasons(baseYear).marchEquinox
            for (shower in METEOR_SHOWERS) {
                val estimate = marchEquinox.addDays(shower.sunLongitude / 360.0 * TROPICAL_YEAR_DAYS)
                val peak = searchSunLongitude(shower.sunLongitude, estimate.addDays(-6.0), 12.0) ?: continue
                events += AstronomicalEvent(
                    peak.toMillisecondsSince1970(), AstronomiKategori.HUJAN_METEOR,
                    "Puncak Hujan Meteor ${shower.name}",
                    "Perkiraan puncak (λ☉ ${"%.1f".format(Locale.US, shower.sunLongitude)}°); ${shower.info}. " +
                        "Waktu terbaik mengamati: dini hari di tempat gelap."
                )
            }
        }
        return events
    }

    /** Gerhana global. Keterlihatan per lokasi dihitung di [EclipseCalculator] (menu Gerhana). */
    private fun eclipseEvents(startMillis: Long, endMillis: Long, zone: TimeZone): List<AstronomicalEvent> {
        val start = Time.fromMillisecondsSince1970(startMillis)
        val events = mutableListOf<AstronomicalEvent>()

        lunarEclipsesAfter(start)
            .takeWhile { it.peak.toMillisecondsSince1970() < endMillis }
            .forEach {
                events += AstronomicalEvent(
                    it.peak.toMillisecondsSince1970(), AstronomiKategori.GERHANA,
                    "Gerhana Bulan ${eclipseKindLabel(it.kind, isSolar = false)}",
                    "Puncak pukul ${clock(it.peak, zone)} (global). Cek keterlihatan di lokasi Anda pada menu Gerhana."
                )
            }

        globalSolarEclipsesAfter(start)
            .takeWhile { it.peak.toMillisecondsSince1970() < endMillis }
            .forEach {
                events += AstronomicalEvent(
                    it.peak.toMillisecondsSince1970(), AstronomiKategori.GERHANA,
                    "Gerhana Matahari ${eclipseKindLabel(it.kind, isSolar = true)}",
                    "Puncak pukul ${clock(it.peak, zone)} (global). Cek keterlihatan di lokasi Anda pada menu Gerhana."
                )
            }

        return events
    }

    private fun eclipseKindLabel(kind: EclipseKind, isSolar: Boolean): String = when (kind) {
        EclipseKind.Total -> "Total"
        EclipseKind.Annular -> "Cincin"
        EclipseKind.Partial -> "Sebagian"
        EclipseKind.Penumbral -> if (isSolar) "Sebagian" else "Penumbra"
    }

    private fun utcYear(millis: Long): Int {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = millis
        return cal.get(Calendar.YEAR)
    }

    private fun clock(time: Time, zone: TimeZone): String = clock(time.toMillisecondsSince1970(), zone)

    // Emulator/perangkat tanpa data locale "id" mengembalikan "GMT+07:00" untuk zona Indonesia,
    // jadi singkatan zona waktu Indonesia dipetakan manual; zona lain pakai nama bawaan.
    private val INDONESIA_ZONE_LABELS = mapOf(
        "Asia/Jakarta" to "WIB",
        "Asia/Pontianak" to "WIB",
        "Asia/Makassar" to "WITA",
        "Asia/Jayapura" to "WIT"
    )

    /** "HH:mm WIB" — zona mengikuti [zone] (default: zona perangkat). */
    private fun clock(millis: Long, zone: TimeZone): String {
        val locale = Locale("id", "ID")
        val sdf = SimpleDateFormat("HH:mm", locale)
        sdf.timeZone = zone
        val date = Date(millis)
        val zoneLabel = INDONESIA_ZONE_LABELS[zone.id]
            ?: zone.getDisplayName(zone.inDaylightTime(date), TimeZone.SHORT, locale)
        return "${sdf.format(date)} $zoneLabel"
    }
}

/** Bentuk kartu di daftar Event Besar (tanggal lokal `yyyy-MM-dd`, sama dengan hari besar lain). */
fun AstronomicalEvent.toHolidayItem(zone: TimeZone = TimeZone.getDefault()): HolidayItem {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    dateFormat.timeZone = zone
    return HolidayItem(
        tanggal = dateFormat.format(Date(epochMillis)),
        tanggalHijriah = "Astronomi · ${kategori.label}",
        keterangan = judul,
        is_cuti = false,
        jenis = EventJenis.ASTRONOMI,
        catatan = keterangan
    )
}
