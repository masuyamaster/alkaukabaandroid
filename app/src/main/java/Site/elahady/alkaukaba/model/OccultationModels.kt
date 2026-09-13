package site.elahady.alkaukaba.model

data class OccultationItem(
    val bodyLabel: String, // "Venus"
    val eventDateLabel: String, // "15 Maret 2026"
    val isOccultation: Boolean, // true = benda benar-benar tertutup piringan Bulan, false = konjungsi dekat tanpa kontak
    val peakTimeLabel: String, // waktu jarak sudut terdekat (closest approach)
    val ingressTimeLabel: String?, // waktu mulai tertutup, null kalau bukan okultasi nyata
    val egressTimeLabel: String?, // waktu muncul kembali, null kalau bukan okultasi nyata
    val minSeparationArcmin: Double, // jarak sudut terdekat pusat-ke-pusat, menit busur
    val visibleFromLocation: Boolean // benda langit di atas ufuk lokasi markaz saat puncak
)

data class OccultationResult(
    val venusOccultations: List<OccultationItem>
)
