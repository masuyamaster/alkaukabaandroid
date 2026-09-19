package site.elahady.alkaukaba.utils

import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Satu peringatan (tahlilan) orang meninggal: [hariKe] (7/40/100/1000) dan [tanggal] Masehi
 * jatuhnya, jam-nya sudah dinolkan (00:00 waktu lokal). */
data class PeringatanKematian(
    val hariKe: Int,
    val tanggal: Calendar
)

/** Hitung tanggal peringatan 7, 40, 100, dan 1.000 hari wafatnya seseorang (tradisi tahlilan
 * di Indonesia). Murni fungsi tanggal, tidak ada dependency ke Android framework. */
object PeringatanKematianCalculator {

    /** Hari ke-N yang diperingati, urut dari yang tercepat. */
    val HARI_PERINGATAN = listOf(7, 40, 100, 1000)

    /**
     * Tanggal tiap peringatan untuk wafat di [tanggalWafat].
     *
     * [hariWafatDihitungKe1] menentukan cara hitung: `true` (default, yang paling lazim di
     * Indonesia) berarti hari wafat itu sendiri = hari ke-1, jadi hari ke-7 jatuh pada
     * wafat + 6 hari (wafat Senin -> hari ke-7 Ahad). `false` berarti hari wafat = hari ke-0,
     * jadi hari ke-7 = wafat + 7 hari (wafat Senin -> hari ke-7 Senin berikutnya).
     */
    fun hitung(tanggalWafat: Calendar, hariWafatDihitungKe1: Boolean = true): List<PeringatanKematian> {
        val offset = if (hariWafatDihitungKe1) 1 else 0
        return HARI_PERINGATAN.map { hariKe ->
            val tanggal = awalHari(tanggalWafat).apply { add(Calendar.DAY_OF_MONTH, hariKe - offset) }
            PeringatanKematian(hariKe, tanggal)
        }
    }

    /** Selisih hari kalender dari [dari] ke [ke]: positif kalau [ke] di masa depan, 0 kalau
     * hari yang sama, negatif kalau sudah lewat. Dihitung per tanggal kalender (bukan per 24
     * jam), jadi aman terhadap perubahan DST dan jam di dalam hari. */
    fun selisihHari(dari: Calendar, ke: Calendar): Long {
        val a = awalHari(dari)
        val b = awalHari(ke)
        // Bandingkan sebagai "hari sejak epoch di UTC" supaya offset zona/DST tidak ikut terhitung.
        val hariA = TimeUnit.MILLISECONDS.toDays(a.timeInMillis + a.get(Calendar.ZONE_OFFSET) + a.get(Calendar.DST_OFFSET))
        val hariB = TimeUnit.MILLISECONDS.toDays(b.timeInMillis + b.get(Calendar.ZONE_OFFSET) + b.get(Calendar.DST_OFFSET))
        return hariB - hariA
    }

    private fun awalHari(cal: Calendar): Calendar = (cal.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
