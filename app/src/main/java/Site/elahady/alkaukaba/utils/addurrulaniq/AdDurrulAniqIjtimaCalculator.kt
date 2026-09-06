package site.elahady.alkaukaba.utils.addurrulaniq

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.math.pow
import kotlin.math.sin

/**
 * Hisab Ijtima' (konjungsi/new moon) memakai metode kitab "Ad-Durrul Aniq fi
 * Ma'rifatil Hilal wal Kusufain bit-Tadqiq" (Ahmad Ghozali Muhammad
 * Fathulloh). Ini bagian pertama (Ijtima') dari metode kedua hisab awal bulan
 * -- pelengkap `EphemerisCalculator` (Astronomy Engine) yang sudah ada,
 * bukan pengganti. Ghurub & posisi Hilal menyusul di file terpisah.
 *
 * Tervalidasi terhadap 4 contoh manual di kitab (rangkuman Notion):
 * - Ijtima' akhir Sya'ban 1434H -> Senin Pon, 8 Juli 2013, 14:15:10.9 WIB
 * - Ijtima' akhir Ramadhan 1434H -> Rabu Pon, 7 Agustus 2013, 04:50:42.1 WIB
 * - Ijtima' akhir Shafar 1434H -> Jum'at Kliwon, 11 Januari 2013, 11:45:25.5 (Kuwait, TZ+3)
 * - Ijtima' akhir Shafar -52H -> Jum'at Legi, 10 April 571M, 09:54:40.4 (Makkah, TZ+3)
 */
object AdDurrulAniqIjtimaCalculator {

    data class IjtimaResult(
        val alamatMutlak: Double,
        val alamatMuaddalah: Double,
        val gregorianYear: Int,
        val gregorianMonth: Int, // 1-12
        val gregorianDay: Int,
        val jamEt: Double, // jam desimal, waktu astronomis (ephemeris time)
        val deltaTDetik: Double,
        val jamUt: Double, // jam desimal, universal time (basis tanggal UT di atas)
        private val hariMingguanIndex: Int,
        private val hariPasaranIndex: Int
    ) {
        /** Nama hari (basis tanggal UT / [gregorianDay]), sesuai penyebutan asli di kitab kalau tidak ada pergeseran zona waktu. */
        val hariMingguan: String get() = HARI_MINGGUAN[hariMingguanIndex]
        val hariPasaran: String get() = HARI_PASARAN[hariPasaranIndex]

        /**
         * Waktu ijtima' pada zona waktu [timeZoneOffsetHours] (mis. 7.0 untuk WIB):
         * jam lokal, pergeseran tanggal (-1/0/+1 hari dari [gregorianDay]), tanggal
         * Masehi lokal, dan nama hari mingguan/pasaran yang ikut bergeser (ini yang
         * dipakai kitab saat menyebut "hari Rabu Pon" dst., bukan basis UT).
         */
        fun waktuDaerah(timeZoneOffsetHours: Double): LocalIjtima {
            var jamWd = jamUt + timeZoneOffsetHours
            var dayShift = 0
            if (jamWd < 0) { jamWd += 24.0; dayShift = -1 }
            if (jamWd >= 24.0) { jamWd -= 24.0; dayShift = 1 }
            val cal = GregorianCalendar(TimeZone.getTimeZone("UTC"))
            cal.clear()
            cal.set(gregorianYear, gregorianMonth - 1, gregorianDay)
            cal.add(Calendar.DAY_OF_MONTH, dayShift)
            val hariMingguanLokal = HARI_MINGGUAN[(((hariMingguanIndex + dayShift) % 7 + 7) % 7)]
            val hariPasaranLokal = HARI_PASARAN[(((hariPasaranIndex + dayShift) % 5 + 5) % 5)]
            return LocalIjtima(dayShift, jamWd, cal, hariMingguanLokal, hariPasaranLokal)
        }
    }

    data class LocalIjtima(
        val dayShift: Int,
        val jamLokal: Double,
        val tanggal: Calendar,
        val hariMingguan: String,
        val hariPasaran: String
    )

    private val HARI_MINGGUAN = arrayOf("Sabtu", "Ahad", "Senin", "Selasa", "Rabu", "Kamis", "Jum'at")
    private val HARI_PASARAN = arrayOf("Kliwon", "Legi", "Pahing", "Pon", "Wage")

    private fun norm360(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    private fun sinDeg(deg: Double) = sin(Math.toRadians(deg))

    /**
     * Jumlah Alamat/F/M'/M untuk "ijtima' akhir bulan [hijriMonth] tahun [hijriYear]"
     * (mengikuti konvensi kitab: dalil bulan N dipakai untuk ijtima' yang MENGAKHIRI
     * bulan N / memulai bulan N+1), sebelum ta'dil.
     */
    private fun sumDalil(hijriYear: Int, hijriMonth: Int): DalilRow {
        val majmuahKey = Math.floorDiv(hijriYear - 1, 30) * 30
        val mabsuthahKey = hijriYear - majmuahKey
        val majmuah = IjtimaTables.tahunMajmuah[majmuahKey]
            ?: throw IllegalArgumentException("Tahun Majmu'ah $majmuahKey di luar jangkauan tabel (-180..1770)")
        val mabsuthah = IjtimaTables.tahunMabsuthah[mabsuthahKey]
            ?: throw IllegalArgumentException("Tahun Mabsuthah $mabsuthahKey di luar jangkauan tabel (1..30)")
        val bulan = IjtimaTables.bulan[hijriMonth]
            ?: throw IllegalArgumentException("Bulan $hijriMonth di luar jangkauan (1..12)")

        val a = majmuah.a + mabsuthah.a + bulan.a
        val f = norm360(majmuah.f + mabsuthah.f + bulan.f)
        val mAksen = norm360(majmuah.mAksen + mabsuthah.mAksen + bulan.mAksen)
        val m = norm360(majmuah.m + mabsuthah.m + bulan.m)
        return DalilRow(a, f, mAksen, m)
    }

    /** Delta T (ET-UT) dalam detik, rumus pendekatan halaman 8-9 kitab. */
    fun deltaTDetik(gregorianYear: Int, gregorianMonth: Int, gregorianDay: Int): Double {
        val tm = gregorianYear + (gregorianMonth - 1) / 12.0 + gregorianDay / 365.0
        return when {
            tm <= -500 -> { val t = tm / 100.0 - 18.2; -20 + 32 * t.pow(2) }
            tm <= 500 -> {
                val t = tm / 100.0
                10583.6 - 1014.41 * t + 33.78311 * t.pow(2) - 5.952053 * t.pow(3) -
                    0.1798452 * t.pow(4) + 0.022174192 * t.pow(5) + 0.0090316521 * t.pow(6)
            }
            tm <= 1600 -> {
                val t = tm / 100.0 - 10
                1574.2 - 556.01 * t + 71.23472 * t.pow(2) + 0.319781 * t.pow(3) -
                    0.8503463 * t.pow(4) - 0.005050998 * t.pow(5) + 0.0083572073 * t.pow(6)
            }
            tm <= 1700 -> { val t = tm - 1600; 120 - 0.9808 * t - 0.01532 * t.pow(2) + t.pow(3) / 7129.0 }
            tm <= 1800 -> {
                val t = tm - 1700
                8.83 + 0.1603 * t - 0.0059285 * t.pow(2) + 0.00013336 * t.pow(3) - t.pow(4) / 1174000.0
            }
            tm <= 1860 -> {
                val t = tm - 1800
                13.72 - 0.332447 * t + 0.0068612 * t.pow(2) + 0.0041116 * t.pow(3) -
                    0.00037436 * t.pow(4) + 0.0000121272 * t.pow(5) - 0.0000001699 * t.pow(6) +
                    0.000000000875 * t.pow(7)
            }
            tm <= 1900 -> {
                val t = tm - 1860
                7.62 + 0.5737 * t - 0.251754 * t.pow(2) + 0.0168066877 * t.pow(3) -
                    0.0004473624 * t.pow(4) + t.pow(5) / 233174.0
            }
            tm <= 1920 -> {
                val t = tm - 1900
                -2.79 + 1.494119 * t - 0.0598939 * t.pow(2) + 0.0061966 * t.pow(3) - 0.000197 * t.pow(4)
            }
            tm <= 1941 -> { val t = tm - 1920; 21.2 + 0.84493 * t - 0.0761 * t.pow(2) + 0.0020936 * t.pow(3) }
            tm <= 1961 -> { val t = tm - 1941; 29.07 + 0.407 * t - t.pow(2) / 233.0 + t.pow(3) / 2547.0 }
            tm <= 1986 -> { val t = tm - 1975; 45.45 + 1.067 * t - t.pow(2) / 260.0 - t.pow(3) / 718.0 }
            tm <= 2005 -> {
                val t = tm - 2000
                63.86 + 0.3345 * t - 0.060374 * t.pow(2) + 0.0017275 * t.pow(3) +
                    0.000651814 * t.pow(4) + 0.00002373599 * t.pow(5)
            }
            tm <= 2050 -> { val t = tm - 2000; 62.92 + 0.32217 * t + 0.005589 * t.pow(2) }
            tm <= 2150 -> {
                val t = (tm - 1820) / 100.0
                -20 + 32 * t.pow(2) - 0.5628 * (2150 - tm)
            }
            else -> { val t = (tm - 1820) / 100.0; -20 + 32 * t.pow(2) }
        }
    }

    private fun isLeapMasehi(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    /** Konversi hari Julian-like (bagian bulat dari Alamat, sebelum -1) ke tanggal Masehi, halaman 163. */
    private fun julianToMasehi(alamatMutlakIntPart: Long): Triple<Int, Int, Int> {
        val b = alamatMutlakIntPart - 1
        val majmuahEntry = JulianMasehiTables.tahunMajmuahMiladiyah.entries
            .filter { it.value <= b }.maxByOrNull { it.value }
            ?: throw IllegalArgumentException("Tahun Masehi di luar jangkauan tabel konversi (< 400)")
        val d = b - majmuahEntry.value
        val mabsuthahEntry = JulianMasehiTables.tahunMabsuthahMiladiyah.entries
            .filter { it.value <= d }.maxByOrNull { it.value }
            ?: throw IllegalArgumentException("Selisih hari di luar jangkauan tabel mabsuthah (0..99 tahun)")
        val gregorianYear = majmuahEntry.key + mabsuthahEntry.key
        val g = d - mabsuthahEntry.value
        val leap = isLeapMasehi(gregorianYear)
        // Bulan Miladiyah: Januari/Pebruari punya varian leap (K, offset -1/-1) vs non-leap (B, offset 0/31).
        val bulanOffsets = if (leap) {
            listOf("Januari" to -1, "Pebruari" to 30, "Maret" to 59, "April" to 90, "Mei" to 120,
                "Juni" to 151, "Juli" to 181, "Agustus" to 212, "September" to 243, "Oktober" to 273,
                "Nopember" to 304, "Desember" to 334)
        } else {
            listOf("Januari" to 0, "Pebruari" to 31, "Maret" to 59, "April" to 90, "Mei" to 120,
                "Juni" to 151, "Juli" to 181, "Agustus" to 212, "September" to 243, "Oktober" to 273,
                "Nopember" to 304, "Desember" to 334)
        }
        val monthIdx = bulanOffsets.indexOfLast { it.second <= g }
        val h = bulanOffsets[monthIdx].second
        val tanggal = (g - h).toInt()
        return Triple(gregorianYear, monthIdx + 1, tanggal)
    }

    /** Hisab Ijtima' yang mengakhiri bulan Hijriyah [hijriMonth] tahun [hijriYear] (mis. bulan=8/Sya'ban -> ijtima' awal Ramadhan). */
    fun calculate(hijriYear: Int, hijriMonth: Int): IjtimaResult {
        val sum = sumDalil(hijriYear, hijriMonth)

        val dalilM = sum.m
        val dalil2M = norm360(2 * sum.m)
        val dalilMAksen = sum.mAksen
        val dalil2MAksen = norm360(2 * sum.mAksen)
        val dalilMPlusMAksen = norm360(sum.m + sum.mAksen)
        val dalilMMinusMAksen = norm360(sum.m - sum.mAksen)
        val dalil2F = norm360(2 * sum.f)
        val dalil2FMinusMAksen = norm360(2 * sum.f - sum.mAksen)

        val t1 = TadilAlamah.C1 * sinDeg(dalilM)
        val t2 = TadilAlamah.C2 * sinDeg(dalil2M)
        val t3 = TadilAlamah.C3 * sinDeg(dalilMAksen)
        val t4 = TadilAlamah.C4 * sinDeg(dalil2MAksen)
        val t5 = TadilAlamah.C5 * sinDeg(dalilMPlusMAksen)
        val t6 = TadilAlamah.C6 * sinDeg(dalilMMinusMAksen)
        val t7 = TadilAlamah.C7 * sinDeg(dalil2F)
        val t8 = TadilAlamah.C8 * sinDeg(dalil2FMinusMAksen)
        val totalTadil = t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8

        val alamatMutlak = sum.a
        val alamatMuaddalah = alamatMutlak + totalTadil + 0.5

        val intPart = Math.floor(alamatMuaddalah).toLong()
        val fracPart = alamatMuaddalah - intPart
        val jamEt = fracPart * 24.0

        val (gYear, gMonth, gDay) = julianToMasehi(intPart)
        val deltaT = deltaTDetik(gYear, gMonth, gDay)
        val jamUt = jamEt - deltaT / 3600.0

        val hMingguanIdx = (((intPart + 2) % 7 + 7) % 7).toInt()
        val hPasaranIdx = (((intPart + 1) % 5 + 5) % 5).toInt()

        return IjtimaResult(
            alamatMutlak = alamatMutlak,
            alamatMuaddalah = alamatMuaddalah,
            gregorianYear = gYear,
            gregorianMonth = gMonth,
            gregorianDay = gDay,
            jamEt = jamEt,
            deltaTDetik = deltaT,
            jamUt = jamUt,
            hariMingguanIndex = hMingguanIdx,
            hariPasaranIndex = hPasaranIdx
        )
    }
}
