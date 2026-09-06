package site.elahady.alkaukaba.utils.addurrulaniq

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Validasi AdDurrulAniqHilalCalculator terhadap contoh "Akhir Sya'ban 1434H /
 * 8 Juli 2013 M" di kitab Ad-Durrul Aniq (markaz Sampang, rangkuman Notion).
 *
 * Dalil (S,m,M,A,N) di bawah adalah HASIL PENJUMLAHAN majmu'ah(1410) +
 * mabsuthah(24) + bulan(8/Sya'ban) + hari(29) yang SUDAH tervalidasi persis
 * di kitab (bukan hasil baca foto) -- dipakai di sini sebagai data uji untuk
 * memvalidasi rumus Ghurub+Hilal, terlepas dari isu transkrip tabel dasar
 * untuk tanggal lain yang belum lengkap.
 */
class AdDurrulAniqHilalCalculatorTest {

    private val phi = -7.2 // -07 12' LS
    private val lambda = 113.25 // 113 15' BT
    private val tt = 5.0 // meter

    @Test
    fun `data matahari akhir Syaban 1434H`() {
        val dalil = AdDurrulAniqHilalCalculator.HilalDalil(
            s = 106.542, m = 183.3721, mBulan = 109.3086, a = 195.9644, n = 245.6933
        )
        val matahari = AdDurrulAniqHilalCalculator.hitungMatahari(dalil, phi, tt)
        println(
            "Matahari -> S'=${matahari.sAksen}, dm=${matahari.deklinasi}, am=${matahari.asensiorekta}, " +
                "sd=${matahari.semidiameter}, e=${matahari.equationOfTime}, dip=${matahari.dip}, " +
                "hm=${matahari.tinggi}, GM=?, GRM(LMT)=${matahari.ghurubLmtJam}, azm=${matahari.azimuth}"
        )
        // Ekspektasi kitab: dm=22 25'36.75" ; sd=0 15'43.9" ; e=-0 5'6.57" ; Dip=0 3'56.13"
        // hm=-0 54'10.03" ; GRM(LMT)=17:57:5.77 ; azm=292 29'39.86"
        assertEquals(22.4269, matahari.deklinasi, 0.01)
        assertEquals(0.2622, matahari.semidiameter, 0.005)
        assertEquals(-0.0851, matahari.equationOfTime, 0.01)
        assertEquals(0.0656, matahari.dip, 0.005)
        assertEquals(-0.9028, matahari.tinggi, 0.01)
        assertEquals(17.9516, matahari.ghurubLmtJam, 0.02)
        assertEquals(292.4944, matahari.azimuth, 0.5)
    }

    @Test
    fun `data bulan akhir Syaban 1434H`() {
        val dalil = AdDurrulAniqHilalCalculator.HilalDalil(
            s = 106.542, m = 183.3721, mBulan = 109.3086, a = 195.9644, n = 245.6933
        )
        val matahari = AdDurrulAniqHilalCalculator.hitungMatahari(dalil, phi, tt)

        // Ghurub WIB 17:24:05.77 (TZ+7) -> UT 10:24:05.77, 8 Juli 2013.
        val jd = AdDurrulAniqHilalCalculator.julianDay(2013, 7, 8, 10.0 + 24.0 / 60.0 + 5.77 / 3600.0)
        val gmst = AdDurrulAniqHilalCalculator.gmstDerajat(jd)
        println("JD=$jd, GMST=$gmst")

        val bulan = AdDurrulAniqHilalCalculator.hitungBulan(dalil, matahari, phi, lambda, gmst, tt)
        println(
            "Bulan -> Mo=${bulan.bujur}, B=${bulan.latitude}, dc=${bulan.deklinasi}, ac=${bulan.asensiorekta}, " +
                "r=${bulan.jarakKm}, Hp=${bulan.horizontalParallax}, sdc=${bulan.semidiameter}, " +
                "hc=${bulan.tinggiGeosentris}, azc=${bulan.azimuth}, hc'=${bulan.tinggiTopocentric}, " +
                "Elo=${bulan.elongasiGeosentris}, Elo'=${bulan.elongasiTopocentric}, " +
                "illum=${bulan.illuminationFraction}, mukuts=${bulan.mukutsMenit}"
        )
        // Ekspektasi kitab: Mo=107 51'6.84" ; B=-4 27'26.64" ; dc=17 49'37.95" ; ac=108 43'34.2"
        // r=405311.618 km ; Hp=0 54'6" ; sdc=0 14'44.46" ; hc=0 32'58.93" ; azc=288 2'51.34"
        // hc'=-0 21'6.92" ; Elo(geo)=4 40'40.39" ; Elo'(topo)=4 28'49.87" ; illum=0.17% ; mh=0:08:25(?? cek ulang, sebenarnya utk contoh lain)
        assertEquals(107.8519, bulan.bujur, 0.05)
        assertEquals(-4.4574, bulan.latitude, 0.05)
        assertEquals(17.8272, bulan.deklinasi, 0.05)
        assertEquals(108.7262, bulan.asensiorekta, 0.1)
        assertEquals(405311.618, bulan.jarakKm, 500.0)
        assertEquals(0.9017, bulan.horizontalParallax, 0.01)
        assertEquals(0.5527, bulan.tinggiGeosentris, 0.05)
        assertEquals(288.0476, bulan.azimuth, 1.0)
        assertEquals(-0.3519, bulan.tinggiTopocentric, 0.05)
        assertEquals(4.6779, bulan.elongasiGeosentris, 0.1)
        assertEquals(4.4805, bulan.elongasiTopocentric, 0.1)
        assertEquals(0.17, bulan.illuminationFraction * 100.0, 0.05)
    }
}
