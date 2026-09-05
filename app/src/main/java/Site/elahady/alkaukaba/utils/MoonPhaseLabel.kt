package site.elahady.alkaukaba.utils

/** Nama fase bulan (Bahasa Indonesia) dari sudut fase Astronomy Engine (0-360, 0=bulan baru). */
object MoonPhaseLabel {
    fun forAngle(phaseAngleDegrees: Double): String {
        val angle = ((phaseAngleDegrees % 360.0) + 360.0) % 360.0
        return when {
            angle < 11.25 || angle >= 348.75 -> "Bulan Baru"
            angle < 78.75 -> "Sabit Awal"
            angle < 101.25 -> "Kuartal Pertama"
            angle < 168.75 -> "Cembung Awal"
            angle < 191.25 -> "Purnama"
            angle < 258.75 -> "Cembung Akhir"
            angle < 281.25 -> "Kuartal Akhir"
            else -> "Sabit Akhir"
        }
    }
}
