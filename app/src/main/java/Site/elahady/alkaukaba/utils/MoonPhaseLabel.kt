package site.elahady.alkaukaba.utils

/**
 * Nama fase bulan dari sudut fase Astronomy Engine (0-360, 0=bulan baru), memakai istilah
 * falakiyah dari tabel "Al-Azhillah Al-Qamariyah" — lihat referensi-ilmu-falak.md §2.
 */
object MoonPhaseLabel {
    fun forAngle(phaseAngleDegrees: Double): String {
        val angle = ((phaseAngleDegrees % 360.0) + 360.0) % 360.0
        return when {
            angle < 11.25 || angle >= 348.75 -> "Ijtimak / Konjungsi"
            angle < 78.75 -> "Hilal / Sabit Muda"
            angle < 101.25 -> "Tarbi' Awwal"
            angle < 168.75 -> "Cembung Awal"
            angle < 191.25 -> "Badr / Purnama"
            angle < 258.75 -> "Cembung Akhir"
            angle < 281.25 -> "Tarbi' Akhir"
            else -> "Sabit Tua"
        }
    }
}
