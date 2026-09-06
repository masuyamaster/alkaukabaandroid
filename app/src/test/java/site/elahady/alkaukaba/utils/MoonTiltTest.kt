package site.elahady.alkaukaba.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import site.elahady.alkaukaba.model.HilalInput
import kotlin.math.abs

/**
 * Regresi untuk bug tanda terbalik di `screenRight` yang diperbaiki commit
 * "Perbaiki tanda komponen kanan di rumus kemiringan limb terang MoonTilt" --
 * versi lama mencerminkan (mirror) orientasi limb terang secara horizontal,
 * lolos dari test lama karena kasus "hilal senyum" (dA=0) tidak menguji
 * komponen kiri-kanan sama sekali (lihat KDoc MoonTilt.kt).
 *
 * Kasus dA=0 di bawah tetap dipakai sebagai kasus DEGENERATE yang exact
 * (theta pasti 0 atau 180 dari rumus, tanpa perlu hitung tangan), sementara
 * kasus dA!=0 dipakai untuk menguji SISI kiri/kanan secara eksplisit --
 * itulah yang sempat kebalik dan tidak tertangkap sebelumnya.
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
    fun `matahari di timur bulan membuat limb terang menghadap kiri layar bukan kanan`() {
        // Bulan tepat di ufuk menghadap utara (az=0, alt=0) -- pada posisi ini
        // "atas" layar = zenith persis dan "kanan" layar = BARAT persis (lihat
        // KDoc MoonTilt: arah kanan layar berlawanan dgn arah timur kompas).
        // Matahari diletakkan di TIMUR bulan (az=30, sedikit di bawah ufuk,
        // wajar utk saat ghurub) -- karena kanan layar = barat, Matahari di
        // timur harus jatuh di SISI KIRI layar, yaitu theta NEGATIF (limb
        // condong kiri-bawah). Versi lama (bug tanda kebalik) akan
        // menghasilkan theta POSITIF (kanan-bawah) untuk input yang sama --
        // persis salah arah yang dilihat user.
        val theta = MoonTilt.brightLimbAngleDegrees(
            moonAzimuthDeg = 0.0,
            moonAltitudeDeg = 0.0,
            sunAzimuthDeg = 30.0,
            sunAltitudeDeg = -5.0
        )
        assertTrue("theta ($theta) harus negatif (limb condong ke kiri layar)", theta < 0.0)
        assertTrue("theta ($theta) harus di kuadran kiri-bawah (-180..-90)", theta in -180.0..-90.0)
    }

    @Test
    fun `matahari di barat bulan adalah cerminan kasus timur - limb terang menghadap kanan layar`() {
        // Cerminan tepat dari test di atas (Matahari di BARAT bulan, az=-30
        // alih-alih +30, altitude sama) -- kanan layar = barat, jadi Matahari
        // di barat harus jatuh di SISI KANAN layar, theta POSITIF, dan besarnya
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
        assertTrue("theta ($thetaBarat) harus positif (limb condong ke kanan layar)", thetaBarat > 0.0)
        assertEquals(-thetaTimur, thetaBarat, 1e-6)
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
