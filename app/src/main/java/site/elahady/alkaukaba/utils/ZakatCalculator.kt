package site.elahady.alkaukaba.utils

/** Hasil hitung zakat fitrah: [totalBerasKg] pakai acuan 2.5 kg beras per jiwa (konsensus
 * mayoritas ormas Indonesia, mis. Baznas/Muhammadiyah/NU), [totalUang] adalah nilai setara
 * dalam Rupiah dari harga beras acuan yang diisi user (tidak ada API harga beras, jadi manual
 * input - lihat diskusi fitur di docs/features/zakat.md). */
data class ZakatFitrahResult(
    val jumlahJiwa: Int,
    val totalBerasKg: Double,
    val totalUang: Double
)

/** Hasil hitung zakat mal. [nisabRupiah] = 85 gram x harga emas per gram saat ini.
 * [hartaBersih] = totalHarta - totalHutang. Tidak ada pengecekan haul (kepemilikan genap 1
 * tahun) - asumsi user sudah tahu hartanya memenuhi syarat haul sebelum pakai kalkulator ini. */
data class ZakatMalResult(
    val nisabRupiah: Double,
    val hartaBersih: Double,
    val wajibZakat: Boolean,
    val jumlahZakat: Double
)

/** Hasil hitung zakat profesi, metode "akumulasi tahunan ala zakat mal": total penghasilan
 * setahun dizakati 2.5% kalau totalnya capai nisab 85 gram emas - metode ini yang dipilih
 * (bukan metode per-panen ala zakat pertanian), lihat docs/features/zakat.md untuk alasan. */
data class ZakatProfesiResult(
    val nisabRupiah: Double,
    val wajibZakat: Boolean,
    val jumlahZakat: Double
)

object ZakatCalculator {

    private const val BERAS_PER_JIWA_KG = 2.5
    private const val NISAB_EMAS_GRAM = 85.0
    private const val TARIF_ZAKAT = 0.025

    fun hitungFitrah(jumlahJiwa: Int, hargaBerasPerKg: Double): ZakatFitrahResult {
        val totalBerasKg = jumlahJiwa * BERAS_PER_JIWA_KG
        return ZakatFitrahResult(
            jumlahJiwa = jumlahJiwa,
            totalBerasKg = totalBerasKg,
            totalUang = totalBerasKg * hargaBerasPerKg
        )
    }

    fun hitungMal(totalHarta: Double, totalHutang: Double, hargaEmasPerGram: Double): ZakatMalResult {
        val nisabRupiah = NISAB_EMAS_GRAM * hargaEmasPerGram
        val hartaBersih = (totalHarta - totalHutang).coerceAtLeast(0.0)
        val wajibZakat = hartaBersih >= nisabRupiah
        return ZakatMalResult(
            nisabRupiah = nisabRupiah,
            hartaBersih = hartaBersih,
            wajibZakat = wajibZakat,
            jumlahZakat = if (wajibZakat) hartaBersih * TARIF_ZAKAT else 0.0
        )
    }

    fun hitungProfesi(totalPenghasilanSetahun: Double, hargaEmasPerGram: Double): ZakatProfesiResult {
        val nisabRupiah = NISAB_EMAS_GRAM * hargaEmasPerGram
        val wajibZakat = totalPenghasilanSetahun >= nisabRupiah
        return ZakatProfesiResult(
            nisabRupiah = nisabRupiah,
            wajibZakat = wajibZakat,
            jumlahZakat = if (wajibZakat) totalPenghasilanSetahun * TARIF_ZAKAT else 0.0
        )
    }
}
