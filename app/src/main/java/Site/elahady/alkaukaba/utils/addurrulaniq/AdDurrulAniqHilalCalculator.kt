package site.elahady.alkaukaba.utils.addurrulaniq

import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Hisab Ghurub + posisi Hilal metode "Ad-Durrul Aniq". Pelengkap
 * [AdDurrulAniqIjtimaCalculator] (Ijtima') -- bersama keduanya membentuk
 * metode kedua hisab awal bulan, alternatif dari Astronomy Engine
 * (`EphemerisCalculator`).
 *
 * Dalil "D" di sini artinya **elongasi rata-rata Bulan-Matahari (D=M-S)**,
 * BUKAN kolom "D" (obliquitas/mail, konstan [TadilHilal.OBLIQUITAS]) di
 * tabel dasar -- dua makna "D" yang kebetulan sama-sama dipakai kitab,
 * dibedakan lewat nama variabel di kode ini.
 *
 * Tervalidasi terhadap contoh Sya'ban 1434H / Sampang (Notion journal):
 * S=106.542, m=183.3721, M=109.3086, A=195.9644, N=245.6933 (dalil sudah
 * dijumlah majmu'ah+mabsuthah+bulan+hari, siap pakai) -> S'=106.4317,
 * dm=22.4268(22°25'36.75"), sd=0°15'43.9", hm=-0°54'11.28",
 * GM=87°59'47.93", Ghurub LMT=17:57:5.77, tinggi hilal geo=0°32'59",
 * topo=-0°21'07", elongasi geo=4°40'40", topo=4°28'50", illum=0.17%.
 */
object AdDurrulAniqHilalCalculator {

    /** Dalil (S,m,M,A,N) yang SUDAH dijumlah majmu'ah+mabsuthah+bulan+hari(+jam kalau perlu), dalam derajat, sebelum di-mod 360. */
    data class HilalDalil(val s: Double, val m: Double, val mBulan: Double, val a: Double, val n: Double)

    data class MatahariHasil(
        val sAksen: Double, // S' (bujur Matahari terkoreksi)
        val deklinasi: Double, // dm
        val asensiorekta: Double, // am
        val semidiameter: Double, // sd (derajat)
        val equationOfTime: Double, // e (jam)
        val dip: Double, // derajat
        val tinggi: Double, // hm
        val azimuth: Double, // azm
        val ghurubLmtJam: Double // GRM, jam desimal
    )

    data class BulanHasil(
        val bujur: Double, // Mo
        val latitude: Double, // B
        val deklinasi: Double, // dc
        val asensiorekta: Double, // ac
        val jarakKm: Double, // r
        val horizontalParallax: Double, // Hp
        val semidiameter: Double, // sdc
        val tinggiGeosentris: Double, // hc
        val azimuth: Double, // azc
        val tinggiTopocentric: Double, // hc'
        val elongasiGeosentris: Double, // Elo
        val elongasiTopocentric: Double, // Elo'
        val illuminationFraction: Double, // 0..1
        val mukutsMenit: Double // mh
    )

    private fun norm360(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    private fun sinD(deg: Double) = sin(Math.toRadians(deg))
    private fun cosD(deg: Double) = cos(Math.toRadians(deg))
    private fun tanD(deg: Double) = tan(Math.toRadians(deg))
    private fun asinD(x: Double) = Math.toDegrees(asin(x.coerceIn(-1.0, 1.0)))
    private fun acosD(x: Double) = Math.toDegrees(acos(x.coerceIn(-1.0, 1.0)))
    private fun atanD(x: Double) = Math.toDegrees(atan(x))

    /**
     * Sudut waktu Matahari/hour-angle-based altitude formula:
     * cosH = -tan(phi)*tan(dec) + sin(alt)/cos(phi)/cos(dec)
     */
    private fun sudutWaktu(phiDeg: Double, decDeg: Double, altDeg: Double): Double {
        val cosH = -tanD(phiDeg) * tanD(decDeg) + sinD(altDeg) / cosD(phiDeg) / cosD(decDeg)
        return acosD(cosH)
    }

    /**
     * Azimuth dari sudut waktu H (0-180, hasil acos) dan deklinasi, dari Utara.
     * Rumus kitab `tan⁻¹(-sinphi/tanH + cosphi*tandec/sinH)` cuma beri hasil
     * mentah -90..90 (rentang atan) -- utk konteks ghurub/moonset (sore hari,
     * H mewakili sudut waktu SETELAH kulminasi) azimuth sejati selalu ada di
     * belahan barat (180-360), jadi hasil mentah digeser +270 (bukan quadrant
     * generik +360-jika-negatif). Tervalidasi persis (<0.01 derajat) terhadap
     * azimuth Matahari & Bulan contoh Sya'ban 1434H.
     */
    private fun azimuthDariSudutWaktu(phiDeg: Double, decDeg: Double, hDeg: Double): Double {
        val azMentah = atanD(-sinD(phiDeg) / tanD(hDeg) + cosD(phiDeg) * tanD(decDeg) / sinD(hDeg))
        return norm360(azMentah + 270.0)
    }

    fun hitungMatahari(dalil: HilalDalil, phiDeg: Double, tinggiTempatM: Double): MatahariHasil {
        val mM = norm360(dalil.m)
        val s1 = TadilHilal.S1 * sinD(mM)
        val s2 = TadilHilal.S2 * sinD(norm360(2 * mM))
        val sAksen = norm360(dalil.s) + s1 + s2

        val dm = asinD(sinD(sAksen) * sinD(TadilHilal.OBLIQUITAS))
        var am = atanD(tanD(sAksen) * cosD(TadilHilal.OBLIQUITAS))
        // koreksi kuadran am sesuai kuadran sAksen (0-90:I,90-180:II,180-270:III,270-360:IV)
        am = koreksiKuadranTangen(am, sAksen)

        val sd = 0.267 / (1 - 0.017 * cosD(mM))
        val e = (norm360(dalil.s) - am) / 15.0
        val dip = (1.76 / 60.0) * sqrt(tinggiTempatM)
        val hm = -(sd + 34.5 / 60.0 + dip)

        val gm = sudutWaktu(phiDeg, dm, hm)
        val grmLmt = gm / 15.0 + 12.0 - e
        val azm = azimuthDariSudutWaktu(phiDeg, dm, gm)

        return MatahariHasil(sAksen, dm, am, sd, e, dip, hm, azm, grmLmt)
    }

    fun hitungBulan(
        dalil: HilalDalil,
        matahari: MatahariHasil,
        phiDeg: Double,
        lambdaDeg: Double,
        gmstDeg: Double,
        tinggiTempatM: Double
    ): BulanHasil {
        val mM = norm360(dalil.m)
        val aA = norm360(dalil.a)
        val nN = norm360(dalil.n)
        val dElongasi = norm360(dalil.mBulan - dalil.s)

        val mo = norm360(
            dalil.mBulan +
                TadilHilal.M1 * sinD(aA) +
                TadilHilal.M2 * sinD(norm360(2 * dElongasi - aA)) +
                TadilHilal.M3 * sinD(norm360(2 * dElongasi)) +
                TadilHilal.M4 * sinD(norm360(2 * aA)) +
                TadilHilal.M5 * sinD(mM) +
                TadilHilal.M6 * sinD(norm360(2 * nN)) +
                TadilHilal.M7 * sinD(norm360(2 * dElongasi - 2 * aA)) +
                TadilHilal.M8 * sinD(norm360(2 * dElongasi - mM - aA)) +
                TadilHilal.M9 * sinD(norm360(2 * dElongasi + aA))
        )

        val b =
            TadilHilal.B1 * sinD(nN) +
                TadilHilal.B2 * sinD(norm360(aA + nN)) +
                TadilHilal.B3 * sinD(norm360(aA - nN)) +
                TadilHilal.B4 * sinD(norm360(2 * dElongasi - nN))

        val dc = asinD(sinD(b) * cosD(TadilHilal.OBLIQUITAS) + cosD(b) * sinD(TadilHilal.OBLIQUITAS) * sinD(mo))
        var ac = acosD(cosD(mo) * cosD(b) / cosD(dc))
        if (norm360(mo) > 180.0) ac = 360.0 - ac

        val r = 385000.56 +
            TadilHilal.R1 * cosD(aA) +
            TadilHilal.R2 * cosD(norm360(2 * dElongasi - aA)) +
            TadilHilal.R3 * cosD(norm360(2 * dElongasi)) +
            TadilHilal.R4 * cosD(norm360(2 * aA))
        val hp = asinD(6378.14 / r)
        val sdc = 0.272476 * hp

        val gc = norm360(gmstDeg - ac + lambdaDeg)
        val hc = asinD(sinD(phiDeg) * sinD(dc) + cosD(phiDeg) * cosD(dc) * cosD(gc))
        val azc = azimuthDariSudutWaktu(phiDeg, dc, gc.let { if (it > 180.0) 360.0 - it else it })
        val z = azc - matahari.azimuth

        val p = hp * cosD(hc)
        val raw = hc - p
        val ref = if (raw > 0) 0.0167 / tanD(hc + 7.31 / (hc + 4.4)) else 0.0
        val dipBulan = (1.76 / 60.0) * sqrt(tinggiTempatM)
        val hcAksen = if (raw <= 0) raw else raw + ref + dipBulan - sdc

        val eloGeo = acosD(sinD(hc) * sinD(matahari.tinggi) + cosD(hc) * cosD(matahari.tinggi) * cosD(z))
        val eloTopo = acosD(sinD(hcAksen) * sinD(matahari.tinggi) + cosD(hcAksen) * cosD(matahari.tinggi) * cosD(z))
        val illumFraction = (1.0 - cosD(eloGeo)) / 2.0
        val mukutsMenit = hc * 4.0

        return BulanHasil(
            bujur = mo,
            latitude = b,
            deklinasi = dc,
            asensiorekta = ac,
            jarakKm = r,
            horizontalParallax = hp,
            semidiameter = sdc,
            tinggiGeosentris = hc,
            azimuth = azc,
            tinggiTopocentric = hcAksen,
            elongasiGeosentris = eloGeo,
            elongasiTopocentric = eloTopo,
            illuminationFraction = illumFraction,
            mukutsMenit = mukutsMenit
        )
    }

    /** Koreksi kuadran hasil atan (selalu -90..90) supaya sesuai kuadran sudut aslinya (0-360). */
    private fun koreksiKuadranTangen(atanResultDeg: Double, referensiSudutDeg: Double): Double {
        val ref = norm360(referensiSudutDeg)
        return when {
            ref < 90.0 -> norm360(atanResultDeg)
            ref < 180.0 -> norm360(180.0 + atanResultDeg)
            ref < 270.0 -> norm360(180.0 + atanResultDeg)
            else -> norm360(360.0 + atanResultDeg)
        }
    }

    /**
     * Dalil (S,m,M,A,N) langsung dari Julian Day pakai rumus gerak rata-rata
     * standar astronomi (Meeus, "Astronomical Algorithms" bab 22 & 47) --
     * PENGGANTI tabel Jadwal Gerak (majmu'ah/mabsuthah/bulan/hari/jam) di
     * kitab. Dipakai karena rate harian yang diturunkan dari data kitab
     * sendiri (via contoh hari 29 & hari 30, Sya'ban 1434H) ternyata PERSIS
     * sama dengan konstanta gerak rata-rata Matahari/Bulan standar ini
     * (S=mean longitude Matahari, m=anomali rata-rata Matahari, M=mean
     * longitude Bulan (L'), A=anomali rata-rata Bulan (M'), N=argumen
     * lintang Bulan (F)) -- artinya S,m,M,A,N di kitab BUKAN tabel
     * proprietary, melainkan representasi tabular dari elemen orbit rata-rata
     * yang sudah baku, sehingga rumus langsung ini lebih presisi & berlaku
     * utk tanggal apa pun tanpa perlu transkrip tabel yang rawan salah baca.
     */
    fun dalilDariJulianDay(julianDay: Double): HilalDalil {
        val t = (julianDay - 2451545.0) / 36525.0
        val t2 = t * t
        val t3 = t2 * t
        val s = norm360(280.46646 + 36000.76983 * t + 0.0003032 * t2)
        val m = norm360(357.5291092 + 35999.0502909 * t - 0.0001536 * t2 + t3 / 24490000.0)
        val mBulan = norm360(218.3164477 + 481267.88123421 * t - 0.0015786 * t2 + t3 / 538841.0)
        val a = norm360(134.9633964 + 477198.8675055 * t + 0.0087414 * t2 + t3 / 69699.0)
        val n = norm360(93.2720950 + 483202.0175233 * t - 0.0036539 * t2 - t3 / 3526000.0)
        return HilalDalil(s, m, mBulan, a, n)
    }

    /** Julian Day (dengan pecahan jam UT) dari tanggal Masehi -- standar astronomis, dipakai utk [gmstDerajat]. */
    fun julianDay(year: Int, month: Int, day: Int, utJamDesimal: Double): Double {
        val y = if (month <= 2) year - 1 else year
        val m = if (month <= 2) month + 12 else month
        val a = y / 100
        val b = 2 - a + a / 4
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + day + utJamDesimal / 24.0 + b - 1524.5
    }

    /** Greenwich Mean Sidereal Time (derajat, 0-360) dari Julian Day -- pengganti tabel "O" kitab, rumus standar Meeus. */
    fun gmstDerajat(julianDay: Double): Double {
        val t = (julianDay - 2451545.0) / 36525.0
        val gmst = 280.46061837 + 360.98564736629 * (julianDay - 2451545.0) +
            0.000387933 * t * t - t * t * t / 38710000.0
        return norm360(gmst)
    }
}
