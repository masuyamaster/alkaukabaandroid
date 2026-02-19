package Site.elahady.alkaukaba.utils

import Site.elahady.alkaukaba.model.HilalInput
import Site.elahady.alkaukaba.model.HilalResult
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.*

object EphemerisCalculator {

    fun calculate(input: HilalInput): HilalResult {
        // --- SIMULASI DATA ASTRONOMI (Berdasarkan Modul Gambar) ---
        // Dalam aplikasi nyata, nilai ini diambil dari Library Swiss Ephemeris atau Algoritma Jean Meeus

        // Contoh Data dari screenshot "Ijtima'"
        val fib = 18.0 // Fraction Illumination Bulan (Jam GMT)
        val elm = 175.954 // Ecliptic Longitude Matahari (175 deg 57' 15")
        val alb = 175.528 // Apparent Longitude Bulan (175 deg 31' 43")

        val sb = 0.600 // Sabaq Bulan (per jam)
        val sm = 0.040 // Sabaq Matahari (per jam)

        // 1. RUMUS IJTIMA' (Sesuai Foto Modul)
        // Rumus: Jam FIB + ((ELM - ALB) / (SB - SM)) + 7 (WIB)
        val deltaLong = elm - alb
        val deltaSabaq = sb - sm
        val correctionTime = deltaLong / deltaSabaq

        val ijtimaGMT = fib + correctionTime
        val ijtimaWIB = ijtimaGMT + 7.0 // Offset WIB

        // Konversi Decimal Hour ke Jam:Menit:Detik
        val ijtimaStr = decimalToTime(ijtimaWIB)

        // 2. RUMUS GHURUB (Matahari Terbenam)
        // Rumus sederhana: 18:00 - Equation of Time (Simulasi)
        val ghurubWIB = 17.80 // Jam 17:48 (Simulasi)
        val ghurubStr = decimalToTime(ghurubWIB)

        // 3. GENERATE LOG UNTUK PDF (Sesuai Foto Modul Ijtima')
        val logBuilder = StringBuilder()
        logBuilder.append("PERHITUNGAN AWAL BULAN SYAWAL / RAMADHAN\n")
        logBuilder.append("SISTEM EPHEMERIS HISAB RUKYAT\n\n")

        logBuilder.append("1. Saat (Jam Terjadi) Ijtima'\n")
        logBuilder.append("   Dari Buku EPHEMERIS, data pada tanggal ${formatDate(input.date)}:\n")
        logBuilder.append("   a. FIB (Fraction Illumination Bulan) 18:00 GMT = $fib\n")
        logBuilder.append("   b. ELM (Ecliptic Longitude Matahari) = ${decimalToDMS(elm)}\n")
        logBuilder.append("   c. ALB (Apparent Longitude Bulan) = ${decimalToDMS(alb)}\n")
        logBuilder.append("   d. Sabaq Matahari (SM) = ${decimalToDMS(sm)}\n")
        logBuilder.append("   e. Sabaq Bulan (SB) = ${decimalToDMS(sb)}\n\n")

        logBuilder.append("   RUMUS IJTIMA':\n")
        logBuilder.append("   = Jam FIB + ((ELM - ALB) / (SB - SM)) + 7.00 (WIB)\n")
        logBuilder.append("   = $fib + ((${decimalToDMS(elm)} - ${decimalToDMS(alb)}) / ... ) + 7.00\n")
        logBuilder.append("   = Hasil Akhir: $ijtimaStr WIB\n")

        return HilalResult(
            ijtimaTime = "$ijtimaStr WIB",
            ghurubTime = "$ghurubStr WIB",
            moonAltitude = "04° 37' 53\"", // Data dummy sesuai screenshot
            moonElongation = "04° 53' 03\"",
            calculationLog = logBuilder.toString()
        )
    }

    // Helper: Desimal ke Format Jam:Menit:Detik
    private fun decimalToTime(value: Double): String {
        val hours = floor(value).toInt()
        val minutes = floor((value - hours) * 60).toInt()
        val seconds = round(((value - hours) * 60 - minutes) * 60).toInt()
        return String.format("%02d:%02d:%02d", hours % 24, minutes, seconds)
    }

    // Helper: Desimal ke Derajat Menit Detik
    private fun decimalToDMS(value: Double): String {
        val d = floor(value).toInt()
        val m = floor((value - d) * 60).toInt()
        val s = ((value - d) * 60 - m) * 60
        return String.format("%d° %d' %.2f\"", d, m, s)
    }

    private fun formatDate(cal: java.util.Calendar): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        return sdf.format(cal.time)
    }
}