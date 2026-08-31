package site.elahady.alkaukaba.model

data class LunarEclipseItem(
    val kindLabel: String, // "Total" / "Sebagian" / "Penumbra"
    val peakDateLabel: String, // "15 Maret 2026"
    val peakTimeLabel: String, // "15 Maret 2026, 20:12:34"
    val magnitudePercent: Double, // obscuration puncak, 0-100
    val visibleFromLocation: Boolean // Bulan di atas ufuk lokasi markaz saat puncak
)

data class SolarEclipseItem(
    val kindLabel: String, // "Total" / "Cincin" / "Sebagian"
    val peakDateLabel: String,
    val partialBeginLabel: String,
    val peakTimeLabel: String,
    val partialEndLabel: String,
    val totalBeginLabel: String?, // null kalau tidak sampai fase total/cincin
    val totalEndLabel: String?,
    val magnitudePercent: Double, // obscuration puncak, 0-100
    val visibleFromLocation: Boolean // Matahari di atas ufuk lokasi markaz saat puncak
)

data class GerhanaResult(
    val lunarEclipses: List<LunarEclipseItem>,
    val solarEclipses: List<SolarEclipseItem>
)
