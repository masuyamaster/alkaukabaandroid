package site.elahady.alkaukaba.utils

/**
 * Nama fase bulan dari sudut fase Astronomy Engine (0-360, 0=bulan baru), memakai nama
 * fase astronomis apa adanya (kolom "Fase Astronomis" di referensi-ilmu-falak.md §2), bukan
 * istilah falakiyah — lihat catatan §4 dokumen fitur (fase-bulan.md) untuk alasan pemilihan ini.
 */
object MoonPhaseLabel {
    fun forAngle(phaseAngleDegrees: Double): String {
        val angle = ((phaseAngleDegrees % 360.0) + 360.0) % 360.0
        return when {
            angle < 11.25 || angle >= 348.75 -> "New Moon"
            angle < 78.75 -> "Waxing Crescent"
            angle < 101.25 -> "First Quarter"
            angle < 168.75 -> "Waxing Gibbous"
            angle < 191.25 -> "Full Moon"
            angle < 258.75 -> "Waning Gibbous"
            angle < 281.25 -> "Last Quarter"
            else -> "Waning Crescent"
        }
    }
}
