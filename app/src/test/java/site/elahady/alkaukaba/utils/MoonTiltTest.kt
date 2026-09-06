package site.elahady.alkaukaba.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import site.elahady.alkaukaba.model.HilalInput
import kotlin.math.abs

/**
 * Regresi untuk bug tanda terbalik di `screenRight` (`cross(screenUp, moon)`
 * seharusnya `cross(moon, screenUp)`) -- versi lama mencerminkan (mirror)
 * orientasi limb terang secara horizontal. Bug ini lolos dari test lama
 * karena dua sebab: (1) kasus "hilal senyum" (dA=0) tidak menguji komponen
 * kiri-kanan sama sekali (suku itu nol), dan (2) validasi silang terhadap
 * rumus posisi-sudut Meeus (χ) + parallactic angle (q) saat itu memakai
 * kombinasi tanda `χ - q` yang ternyata IKUT tercermin, jadi dua bug saling
 * "membenarkan" satu sama lain -- baru ketahuan setelah dicocokkan langsung
 * ke pengamatan visual di Stellarium (bukan ke rumus lain).
 *
 * Kasus dA=0 di bawah tetap dipakai sebagai kasus DEGENERATE yang exact
 * (theta pasti 0 atau 180 dari rumus, tanpa perlu hitung tangan), sementara
 * kasus dA!=0 dipakai untuk menguji SISI kiri/kanan secara eksplisit
 * berdasarkan fakta kompas: menghadap Utara (moon az=0), tangan kanan =
 * Timur -- jadi Matahari di Timur (azimuth lebih besar) harus jatuh di
 * KANAN layar, bukan kiri.
 */
class MoonTiltTest {

    @Test
    fun `matahari segaris azimuth dengan bulan dan lebih rendah membuat limb terang menghadap lurus ke bawah`() {
        // "Hilal senyum" khatulistiwa: dA=0, Matahari jauh di bawah ufuk relatif
        // Bulan -> limb terang menghadap lurus ke bawah (|theta|=180, dari
        // zenith). Dibandingkan lewat abs() karena dA=0.0 eksak membuat
        // pembilang atan2 bertanda -0.0 (bukan +0.0) - atan2(-0.0, negatif)
        // mengembalikan -180.0, bukan +180.0, walau keduanya arah yang sama
        // persis (lurus ke bawah); ini artefak tanda-nol floating point, bukan
        // masalah logika.
        val theta = MoonTilt.brightLimbAngleDegrees(
            moonAzimuthDeg = 100.0,
            moonAltitudeDeg = 10.0,
            sunAzimuthDeg = 100.0,
            sunAltitudeDeg = -5.0
        )
        assertEquals(180.0, abs(theta), 1e-6)
    }

    @Test
    fun `matahari segaris azimuth dengan bulan tapi lebih tinggi membuat limb terang menghadap lurus ke atas`() {
        // Kebalikan kasus di atas (dA=0, tapi Matahari lebih TINGGI dari Bulan)
        // -> limb terang menghadap lurus ke atas (theta=0).
        val theta = MoonTilt.brightLimbAngleDegrees(
            moonAzimuthDeg = 100.0,
            moonAltitudeDeg = 10.0,
            sunAzimuthDeg = 100.0,
            sunAltitudeDeg = 50.0
        )
        assertEquals(0.0, theta, 1e-6)
    }

    @Test
    fun `matahari di timur bulan membuat limb terang menghadap kanan layar bukan kiri`() {
        // Bulan tepat di ufuk menghadap utara (az=0, alt=0) -- pada posisi ini
        // "atas" layar = zenith persis dan "kanan" layar = TIMUR persis
        // (fakta kompas: menghadap Utara, tangan kanan = Timur).
        // Matahari diletakkan di TIMUR bulan (az=30, sedikit di bawah ufuk,
        // wajar utk saat ghurub) -- karena kanan layar = timur, Matahari di
        // timur harus jatuh di SISI KANAN layar, yaitu theta POSITIF (limb
        // condong kanan-bawah). Versi lama (bug tanda kebalik) menghasilkan
        // theta NEGATIF (kiri-bawah) untuk input yang sama -- persis salah
        // arah dari yang benar-benar terlihat di langit/Stellarium.
        val theta = MoonTilt.brightLimbAngleDegrees(
            moonAzimuthDeg = 0.0,
            moonAltitudeDeg = 0.0,
            sunAzimuthDeg = 30.0,
            sunAltitudeDeg = -5.0
        )
        assertTrue("theta ($theta) harus positif (limb condong ke kanan layar)", theta > 0.0)
        assertTrue("theta ($theta) harus di kuadran kanan-bawah (90..180)", theta in 90.0..180.0)
    }

    @Test
    fun `matahari di barat bulan adalah cerminan kasus timur - limb terang menghadap kiri layar`() {
        // Cerminan tepat dari test di atas (Matahari di BARAT bulan, az=-30
        // alih-alih +30, altitude sama) -- kanan layar = timur, jadi Matahari
        // di barat harus jatuh di SISI KIRI layar, theta NEGATIF, dan besarnya
        // sama persis (simetri cermin) dengan kasus timur di atas.
        val thetaTimur = MoonTilt.brightLimbAngleDegrees(
            moonAzimuthDeg = 0.0,
            moonAltitudeDeg = 0.0,
            sunAzimuthDeg = 30.0,
            sunAltitudeDeg = -5.0
        )
        val thetaBarat = MoonTilt.brightLimbAngleDegrees(
            moonAzimuthDeg = 0.0,
            moonAltitudeDeg = 0.0,
            sunAzimuthDeg = -30.0,
            sunAltitudeDeg = -5.0
        )
        assertTrue("theta ($thetaBarat) harus negatif (limb condong ke kiri layar)", thetaBarat < 0.0)
        assertEquals(-thetaTimur, thetaBarat, 1e-6)
    }

    @Test
    fun `data sungguhan dari Stellarium - limb terang di kuadran kiri-atas sesuai pengamatan visual`() {
        // Sumber kebenaran EKSTERNAL (bukan rumus lain, bukan penalaran
        // vektor sendiri): pengamatan visual langsung di Stellarium Web,
        // 2026-09-07 18:32 WIB, lokasi -7.11316,112.43213 -- inilah kasus
        // yang MEMBONGKAR bug mirror `screenRight` (lihat KDoc kelas ini).
        // User melihat sendiri di screenshot Stellarium: Matahari berada di
        // kiri-atas Bulan. Data Az/Alt persis dari panel info Stellarium
        // saat itu:
        //   Bulan    Az=305.038 Alt=-59.174
        //   Matahari Az=274.139 Alt=-15.947
        // (Kedua benda di bawah ufuk -- Stellarium dipakai dengan ground
        // disembunyikan, jadi ini bukan momen bisa dirukyah sungguhan, tapi
        // tetap data Az/Alt riil yang sah untuk uji arah.)
        // Matahari (az lebih kecil -> kiri, alt lebih tinggi -> atas)
        // dibanding Bulan, jadi limb terang harus di KIRI-ATAS: theta
        // negatif, di rentang -90..0. Versi lama (bug kebalik) akan
        // menghasilkan +41 (kanan-atas), bukan -41 (kiri-atas).
        val theta = MoonTilt.brightLimbAngleDegrees(
            moonAzimuthDeg = 305.038,
            moonAltitudeDeg = -59.174,
            sunAzimuthDeg = 274.139,
            sunAltitudeDeg = -15.947
        )
        assertTrue("theta ($theta) harus negatif (limb condong ke kiri layar)", theta < 0.0)
        assertTrue("theta ($theta) harus di kuadran kiri-atas (-90..0)", theta in -90.0..0.0)
        assertEquals(-41.015, theta, 0.01)
    }

    @Test
    fun `dipakai dengan geometri hilal sungguhan saat ghurub, limb terang menghadap paruh bawah bukan paruh atas`() {
        // Integrasi dgn EphemerisCalculator persis seperti dipanggil di
        // AwalBulanActivity -- saat ghurub, Matahari selalu di bawah ufuk
        // relatif Bulan (itulah syarat hilal), jadi limb terang harus selalu
        // condong ke PARUH BAWAH (|theta| > 90), tidak pernah ke paruh atas,
        // utk lokasi/tanggal berapa pun.
        val input = HilalInput(latitude = -6.2088, longitude = 106.8456, heightMeters = 50.0)
        val result = EphemerisCalculator.calculate(input)

        val theta = MoonTilt.brightLimbAngleDegrees(
            moonAzimuthDeg = result.azimuthHilal,
            moonAltitudeDeg = result.tinggiHilal,
            sunAzimuthDeg = result.azimuthMatahari,
            sunAltitudeDeg = result.tinggiMatahari
        )
        println("Jakarta -> azHilal=${result.azimuthHilal}, azMatahari=${result.azimuthMatahari}, theta=$theta")

        assertTrue("theta ($theta) harus di paruh bawah (abs > 90)", abs(theta) > 90.0)
    }
}
