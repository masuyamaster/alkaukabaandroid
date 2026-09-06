package site.elahady.alkaukaba.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Sudut orientasi limb terang Bulan (arah Matahari) sebagaimana tampak di
 * langit pengamat, diukur dari atas (zenith) searah jarum jam — dipakai untuk
 * merotasi ilustrasi hilal (MoonPhaseView.setBrightLimbAngle) supaya sesuai
 * kemiringan sungguhan saat rukyah, bukan sekadar kiri/kanan generik.
 *
 * Diturunkan dengan proyeksi vektor: arah Matahari diproyeksikan ke bidang
 * tangen langit di posisi Bulan, dengan basis "atas" = komponen zenith yang
 * tegak lurus arah pandang ke Bulan, dan "kanan" tegak lurus keduanya, searah
 * dengan kanan sejati pengamat (fakta kompas: menghadap Utara, tangan kanan
 * = Timur — azimuth lebih besar = lebih ke kanan pengamat, tidak dicerminkan).
 *
 * Tervalidasi terhadap kasus "hilal senyum" khatulistiwa: saat azimuth
 * Matahari & Bulan sama dan Matahari jauh di bawah ufuk relatif Bulan, limb
 * terang mengarah lurus ke bawah (menuju ufuk) — sesuai fenomena hilal tipis
 * yang dikenal di lintang rendah. Kasus ini TIDAK menguji tanda komponen
 * kiri-kanan (dA=0 membuat suku itu nol).
 *
 * Komponen kiri-kanan sendiri tervalidasi terhadap pengamatan langsung di
 * Stellarium (posisi Matahari relatif Bulan di layar, bukan cuma kecocokan
 * rumus lain) — versi sebelumnya (`cross(screenUp, moon)`, tanpa pembalikan
 * argumen) ternyata mencerminkan arah kiri-kanan secara horizontal walau
 * lolos cocok dengan rumus posisi-sudut Meeus (χ) + parallactic angle,
 * karena kombinasi tanda χ dan q yang dipakai saat itu ikut tercermin juga
 * sehingga saling "membenarkan" satu sama lain. `cross(moon, screenUp)` di
 * bawah ini adalah versi yang sudah dikoreksi.
 *
 * Rumus tertutup (hasil reduksi aljabar dari operasi vektor di atas — dua
 * suku ini yang sebenarnya dihitung, vektor 3D cuma alat bantu penurunan):
 * dengan Am/hm = azimuth/altitude Bulan, As/hs = azimuth/altitude Matahari,
 * dan dA = As - Am:
 *
 *   theta = atan2(
 *       cos(hs) * sin(dA),
 *       sin(hs) * cos(hm) - cos(hs) * sin(hm) * cos(dA)
 *   )
 *
 * Bentuk ini persis rumus initial bearing/forward azimuth trigonometri bola
 * (dipakai juga untuk parallactic angle di astronomi & great-circle course
 * di navigasi), dengan altitude berperan seperti latitude dan azimuth
 * seperti longitude — theta adalah "arah kompas" dari Bulan menuju Matahari
 * di langit sebagaimana benar-benar terlihat mata, diukur dari zenith
 * (bukan dari utara sejati).
 */
object MoonTilt {

    fun brightLimbAngleDegrees(
        moonAzimuthDeg: Double,
        moonAltitudeDeg: Double,
        sunAzimuthDeg: Double,
        sunAltitudeDeg: Double
    ): Double {
        val moon = toEnu(moonAzimuthDeg, moonAltitudeDeg)
        val sun = toEnu(sunAzimuthDeg, sunAltitudeDeg)
        val up = doubleArrayOf(0.0, 0.0, 1.0)

        val screenUp = normalize(subtract(up, scale(moon, dot(up, moon))))
        val screenRight = cross(moon, screenUp)

        val sunPerp = subtract(sun, scale(moon, dot(sun, moon)))
        val right = dot(sunPerp, screenRight)
        val top = dot(sunPerp, screenUp)

        return Math.toDegrees(atan2(right, top))
    }

    private fun toEnu(azimuthDeg: Double, altitudeDeg: Double): DoubleArray {
        val az = Math.toRadians(azimuthDeg)
        val alt = Math.toRadians(altitudeDeg)
        return doubleArrayOf(cos(alt) * sin(az), cos(alt) * cos(az), sin(alt))
    }

    private fun dot(a: DoubleArray, b: DoubleArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

    private fun cross(a: DoubleArray, b: DoubleArray) = doubleArrayOf(
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0]
    )

    private fun subtract(a: DoubleArray, b: DoubleArray) =
        doubleArrayOf(a[0] - b[0], a[1] - b[1], a[2] - b[2])

    private fun scale(a: DoubleArray, s: Double) = doubleArrayOf(a[0] * s, a[1] * s, a[2] * s)

    private fun normalize(a: DoubleArray): DoubleArray {
        val mag = sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2])
        return if (mag < 1e-9) a else doubleArrayOf(a[0] / mag, a[1] / mag, a[2] / mag)
    }
}
