package site.elahady.alkaukaba.utils

import site.elahady.alkaukaba.model.HilalInput
import site.elahady.alkaukaba.model.VisibilityGridPoint
import site.elahady.alkaukaba.model.WorldVisibilityResult

// Peta visibilitas hilal dunia versi "kasar": grid renggang (15° lintang x 15° bujur, bukan
// grid rapat ala HilalMap/al-habib.info yang punya resolusi jauh lebih halus) supaya tetap
// bisa dihitung di HP dalam waktu wajar — tiap sel masih butuh satu pemanggilan
// EphemerisCalculator.calculate() penuh (cari ghurub + posisi Matahari/Bulan di titik itu),
// walau pencarian ijtima' (peristiwa global, sama untuk semua titik) sudah tidak diulang per sel
// (lihat findIjtima() di bawah). Grid rapat ala referensi (ribuan titik) tetap akan terasa berat
// tanpa optimasi lebih lanjut di sisi ghurub/posisi per titik (lihat docs/features/peta-visibilitas.md).
//
// Kriteria yang dipakai Neo-MABIMS (2 kategori: memenuhi/belum, dari
// HilalResult.hilalMemenuhiKriteria) — BUKAN Odeh 2006 (4 kategori) yang dipakai peta
// referensi, supaya reuse mesin hisab & kriteria yang sudah divalidasi di fitur lain
// (Awal Bulan, Hisab Nasional), bukan implementasi kriteria baru dari nol.
object WorldVisibilityCalculator {

    // 5° step (naik dari 15° semula) -- feasible setelah pencarian ijtima' tidak lagi diulang per
    // titik (lihat calculate() di bawah): benchmark JVM 1960 titik (5°) ~0.4 detik, vs 240 titik
    // (15°) versi lama yang terasa "beberapa detik" karena redundansi ijtima' per titik.
    // Lintang dihitung sampai +-85 (bukan +-90: di kutub persis, bujur degenerate dan matahari
    // tidak terbenam). Sisa 85..90 dan sel yang gagal dihitung diisi WorldMapView dari sel
    // terdekat supaya zona memenuhi/belum menutup seluruh peta.
    private val LATITUDES = (-85..85 step 5).map { it.toDouble() }
    private val LONGITUDES = (-180..175 step 5).map { it.toDouble() }

    fun calculate(monthOffset: Int = 0): WorldVisibilityResult {
        val points = mutableListOf<VisibilityGridPoint>()
        var bulanLabel = ""
        var ghurubRefLabel = ""

        // Ijtima' dihitung sekali (peristiwa global, tidak tergantung observer) lalu dipakai ulang
        // di semua 240 titik grid lewat overload EphemerisCalculator.calculate(input, ijtima) --
        // menghindari 240x pencarian rantai new-moon yang hasilnya identik (lihat dokumentasi
        // overload itu).
        val ijtima = EphemerisCalculator.findIjtima(monthOffset)

        for (lat in LATITUDES) {
            for (lng in LONGITUDES) {
                try {
                    val hasil = EphemerisCalculator.calculate(HilalInput(lat, lng, 0.0, monthOffset), ijtima)
                    points.add(VisibilityGridPoint(lat, lng, hasil.hilalMemenuhiKriteria))
                    if (bulanLabel.isEmpty()) {
                        bulanLabel = hasil.bulanHijriyahLabel
                        ghurubRefLabel = hasil.ijtimaTime
                    }
                } catch (e: Exception) {
                    // Lewati sel yang gagal dihitung (mis. kasus tepi ekstrem dekat kutub) —
                    // titik itu cukup tidak digambar di peta, tidak menggagalkan seluruh grid.
                }
            }
        }

        return WorldVisibilityResult(bulanLabel, ghurubRefLabel, points)
    }
}
