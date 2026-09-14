package site.elahady.alkaukaba.utils

import io.github.cosinekitty.astronomy.Observer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import site.elahady.alkaukaba.model.HilalInput
import site.elahady.alkaukaba.model.HilalResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * [HijriCalendarEngine] dipakai widget Kalender beranda supaya tanggal 1 Hijriyah SELALU sinkron
 * dengan hasil fitur Awal Bulan ([EphemerisCalculator], kriteria Neo-MABIMS). Test ini
 * membandingkan keduanya langsung (dua jalur pencarian ijtima' yang berbeda - maju dari
 * "sekarang" vs mundur+maju dari tanggal target - harus konvergen ke hari yang sama), bukan
 * cuma mengecek "tidak error".
 */
class HijriCalendarEngineTest {

    private val tanggalFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    private val jakarta = Observer(-6.2088, 106.8456, 50.0)

    private fun expectedNextMonthStart(ephemeris: HilalResult): Calendar {
        val ghurubDate = tanggalFormat.parse(ephemeris.tanggalGhurubLabel)!!
        val extraDays = if (ephemeris.hilalMemenuhiKriteria) 1 else 2
        return Calendar.getInstance().apply {
            time = ghurubDate
            add(Calendar.DAY_OF_MONTH, extraDays)
        }
    }

    @Test
    fun `tanggal 1 bulan Hijriyah berikutnya dari buildCalendar sama persis dengan hasil EphemerisCalculator`() {
        val ephemeris = EphemerisCalculator.calculate(HilalInput(-6.2088, 106.8456, 50.0))
        val expectedStart = expectedNextMonthStart(ephemeris)

        val monthStart = expectedStart.clone() as Calendar
        monthStart.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)

        val calendarDays = HijriCalendarEngine.buildCalendar(jakarta, monthStart, daysInMonth)
        val hijriDay = calendarDays[expectedStart.get(Calendar.DAY_OF_MONTH) - 1]

        println(
            "expectedStart=${tanggalFormat.format(expectedStart.time)}, hijriDay=$hijriDay, " +
                "ephemeris.bulanHijriyahLabel=${ephemeris.bulanHijriyahLabel}"
        )

        assertEquals("Tanggal 1 Hijriyah harus jatuh persis di hari yang sama dengan Awal Bulan", 1, hijriDay.day)
        assertTrue(
            "Nama bulan (${hijriDay.monthName}) harus muncul di label Awal Bulan (${ephemeris.bulanHijriyahLabel})",
            ephemeris.bulanHijriyahLabel.contains(hijriDay.monthName)
        )
    }

    @Test
    fun `hari sebelum tanggal 1 baru masih ikut bulan lama (hari ke-29 atau ke-30)`() {
        val ephemeris = EphemerisCalculator.calculate(HilalInput(-6.2088, 106.8456, 50.0))
        val expectedStart = expectedNextMonthStart(ephemeris)
        val dayBefore = (expectedStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) }

        val monthStart = dayBefore.clone() as Calendar
        monthStart.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)

        val calendarDays = HijriCalendarEngine.buildCalendar(jakarta, monthStart, daysInMonth)
        val hijriDayBefore = calendarDays[dayBefore.get(Calendar.DAY_OF_MONTH) - 1]

        assertTrue(
            "Hari sebelum awal bulan baru harus hari ke-29 atau ke-30, malah ${hijriDayBefore.day}",
            hijriDayBefore.day == 29 || hijriDayBefore.day == 30
        )
    }

    @Test
    fun `hijriTitleFor tidak error untuk beberapa bulan ke depan dan ke belakang`() {
        val today = Calendar.getInstance()
        for (offset in -6..6) {
            val cal = (today.clone() as Calendar).apply { add(Calendar.MONTH, offset) }
            val label = HijriCalendarEngine.hijriTitleFor(jakarta, cal)
            assertTrue("Label '$label' harus diakhiri ' H'", label.endsWith(" H"))
        }
    }

    @Test
    fun `buildCalendar tidak pernah melompat lebih dari 1 hari Hijriyah antar tanggal Masehi berurutan`() {
        val monthStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)
        val calendarDays = HijriCalendarEngine.buildCalendar(jakarta, monthStart, daysInMonth)

        for (i in 1 until calendarDays.size) {
            val prev = calendarDays[i - 1]
            val curr = calendarDays[i]
            val sameMonth = prev.monthName == curr.monthName && prev.year == curr.year
            if (sameMonth) {
                assertEquals("Hari Hijriyah harus naik 1 tiap hari Masehi dalam bulan yang sama", prev.day + 1, curr.day)
            } else {
                assertEquals("Saat ganti bulan Hijriyah, hari harus reset ke 1", 1, curr.day)
                assertTrue("Hari terakhir bulan lama harus 29 atau 30", prev.day == 29 || prev.day == 30)
            }
        }
    }
}
