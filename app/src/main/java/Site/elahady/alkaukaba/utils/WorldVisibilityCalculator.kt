package site.elahady.alkaukaba.utils

import site.elahady.alkaukaba.model.HilalInput
import site.elahady.alkaukaba.model.VisibilityGridPoint
import site.elahady.alkaukaba.model.WorldVisibilityResult

// Peta visibilitas hilal dunia versi "kasar": grid renggang (15° lintang x 15° bujur, bukan
// grid rapat ala HilalMap/al-habib.info yang punya resolusi jauh lebih halus) supaya tetap
// bisa dihitung di HP dalam waktu wajar — tiap sel butuh satu pemanggilan penuh
// EphemerisCalculator.calculate() (cari ijtima' + ghurub + posisi Matahari/Bulan), jadi grid
// rapat ala referensi (ribuan titik) akan terlalu lambat tanpa optimasi mesin hisab lebih
// lanjut (lihat known limitation di docs/features/peta-visibilitas.md).
//
// Kriteria yang dipakai Neo-MABIMS (2 kategori: memenuhi/belum, dari
// HilalResult.hilalMemenuhiKriteria) — BUKAN Odeh 2006 (4 kategori) yang dipakai peta
// referensi, supaya reuse mesin hisab & kriteria yang sudah divalidasi di fitur lain
// (Awal Bulan, Hisab Nasional), bukan implementasi kriteria baru dari nol.
object WorldVisibilityCalculator {

    private val LATITUDES = (-60..75 step 15).map { it.toDouble() }
    private val LONGITUDES = (-180..165 step 15).map { it.toDouble() }

    fun calculate(monthOffset: Int = 0): WorldVisibilityResult {
        val points = mutableListOf<VisibilityGridPoint>()
        var bulanLabel = ""
        var ghurubRefLabel = ""

        for (lat in LATITUDES) {
            for (lng in LONGITUDES) {
                try {
                    val hasil = EphemerisCalculator.calculate(HilalInput(lat, lng, 0.0, monthOffset))
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
