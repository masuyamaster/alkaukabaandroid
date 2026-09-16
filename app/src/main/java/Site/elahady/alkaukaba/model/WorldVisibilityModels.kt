package site.elahady.alkaukaba.model

// Satu sel grid kasar peta visibilitas hilal dunia (lihat WorldVisibilityCalculator).
data class VisibilityGridPoint(
    val latitude: Double,
    val longitude: Double,
    val memenuhiKriteria: Boolean
)

data class WorldVisibilityResult(
    val bulanHijriyahLabel: String,
    val ghurubRefLabel: String, // label ghurub markaz referensi (0,0) buat konteks tanggal di kartu info
    val points: List<VisibilityGridPoint>
)
