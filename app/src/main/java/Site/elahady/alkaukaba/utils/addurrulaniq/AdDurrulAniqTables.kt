package site.elahady.alkaukaba.utils.addurrulaniq

/**
 * Data tabel hisab awal bulan metode "Ad-Durrul Aniq fi Ma'rifatil Hilal wal
 * Kusufain bit-Tadqiq" (Ahmad Ghozali Muhammad Fathulloh), ditranskrip dari
 * foto halaman tabel kitab (2026-09-06). Setiap baris komentar berisi tahun
 * Hijriyah/bulan/dst sesuai label asli di kitab untuk memudahkan verifikasi
 * ulang terhadap buku fisik kalau ditemukan kejanggalan hasil hitung.
 *
 * Kolom setiap baris: Alamat (A, hari Julian-like), Hishshatul 'Ardh (F),
 * Khashshah (M'), Markaz (M) -- semua dalam derajat kecuali A.
 */
internal data class DalilRow(val a: Double, val f: Double, val mAksen: Double, val m: Double)

internal object IjtimaTables {

    /** Jadwal gerak tahun Majmu'ah (kelipatan 30 tahun), halaman 156-157. */
    val tahunMajmuah: Map<Int, DalilRow> = mapOf(
        -180 to DalilRow(1884297.0508, 330.6204, 6.7173, 357.5517),
        -150 to DalilRow(1894928.0615, 212.0162, 300.7155, 35.6803),
        -120 to DalilRow(1905559.0722, 93.4118, 234.7155, 73.6088),
        -90 to DalilRow(1916190.0830, 334.8072, 168.7173, 111.5374),
        -60 to DalilRow(1926821.0937, 216.2022, 102.7210, 149.4660), // validasi: contoh Shafar -52H
        -30 to DalilRow(1937452.1045, 97.5970, 36.7265, 187.3946),
        0 to DalilRow(1948083.1153, 338.9915, 330.7338, 225.3232),
        30 to DalilRow(1958714.1262, 220.3857, 264.7429, 263.2517),
        60 to DalilRow(1969345.1370, 101.7797, 198.7539, 301.1803),
        90 to DalilRow(1979976.1479, 343.1734, 132.7667, 339.1089),
        120 to DalilRow(1990607.1588, 224.5668, 66.7813, 17.0374),
        150 to DalilRow(2001238.1698, 105.9599, 0.7978, 54.9660),
        180 to DalilRow(2011869.1807, 347.3527, 294.8161, 92.8945),
        210 to DalilRow(2022500.1917, 228.7453, 228.8362, 130.8231),
        240 to DalilRow(2033131.2027, 110.1376, 162.8582, 168.7517),
        270 to DalilRow(2043762.2138, 351.5296, 96.8821, 206.6802),
        300 to DalilRow(2054393.2249, 232.9214, 30.9078, 244.6087),
        330 to DalilRow(2065024.2359, 114.3128, 324.9353, 282.5373),
        360 to DalilRow(2075655.2471, 355.7040, 258.9647, 320.4658),
        390 to DalilRow(2086286.2582, 237.0949, 192.9959, 358.3943),
        420 to DalilRow(2096917.2694, 118.4855, 127.0291, 36.3229),
        450 to DalilRow(2107548.2806, 359.8759, 61.0640, 74.2514),
        480 to DalilRow(2118179.2918, 241.2659, 355.1009, 112.1799),
        510 to DalilRow(2128810.3030, 122.6557, 289.1396, 150.1084),
        540 to DalilRow(2139441.3143, 4.0452, 223.1801, 188.0370),
        570 to DalilRow(2150072.3255, 245.4344, 157.2226, 225.9655),
        600 to DalilRow(2160703.3369, 126.8233, 91.2669, 263.8940),
        630 to DalilRow(2171334.3482, 8.2120, 25.3131, 301.8225),
        660 to DalilRow(2181965.3596, 249.6003, 319.3612, 339.7510),
        690 to DalilRow(2192596.3710, 130.9884, 253.4111, 17.6795),
        720 to DalilRow(2203227.3824, 12.3762, 187.4630, 55.6080),
        750 to DalilRow(2213858.3938, 253.7637, 121.5167, 93.5365),
        780 to DalilRow(2224489.4053, 135.1509, 55.5723, 131.4650),
        810 to DalilRow(2235120.4168, 16.5379, 349.6298, 169.3935),
        840 to DalilRow(2245751.4283, 257.9245, 283.6892, 207.3219),
        870 to DalilRow(2256382.4398, 139.3109, 217.7504, 245.2504),
        900 to DalilRow(2267013.4514, 20.6970, 151.8136, 283.1789),
        930 to DalilRow(2277644.4630, 262.0828, 85.8787, 321.1074),
        960 to DalilRow(2288275.4746, 143.4683, 19.9457, 359.0358),
        990 to DalilRow(2298906.4862, 24.8535, 314.0145, 36.9643),
        1020 to DalilRow(2309537.4979, 248.0853, 248.0853, 74.8928), // TODO: F dan M' sama persis, kemungkinan salah baca foto -- verifikasi ke buku fisik sebelum dipakai (tidak memengaruhi 4 contoh tervalidasi)
        1050 to DalilRow(2320168.5096, 147.6230, 182.1580, 112.8212),
        1080 to DalilRow(2330799.5213, 29.0074, 116.2326, 150.7497),
        1110 to DalilRow(2341430.5330, 270.3914, 50.3091, 188.6781),
        1140 to DalilRow(2352061.5448, 151.7752, 344.8875, 226.6066),
        1170 to DalilRow(2362692.5566, 33.1587, 278.4679, 264.6350),
        1200 to DalilRow(2373323.5684, 274.5418, 212.5501, 302.4634),
        1230 to DalilRow(2383954.5802, 155.9247, 146.6343, 340.3919),
        1260 to DalilRow(2394585.5921, 37.3073, 80.7204, 18.3203),
        1290 to DalilRow(2405216.6039, 278.6896, 14.8084, 56.2487),
        1320 to DalilRow(2415847.6158, 160.0716, 308.8984, 94.1771),
        1350 to DalilRow(2426478.6278, 41.4534, 242.9902, 132.1056),
        1380 to DalilRow(2437109.6397, 282.8348, 177.0840, 170.0340),
        1410 to DalilRow(2447740.6520, 164.2162, 111.1791, 207.9587), // validasi: contoh Sya'ban & Ramadhan 1434H
        1440 to DalilRow(2458371.6634, 45.5986, 45.5986, 245.5869),
        1470 to DalilRow(2469002.6758, 286.9809, 339.6801, 283.8151),
        1500 to DalilRow(2479633.6876, 168.3632, 273.4506, 321.7433),
        1530 to DalilRow(2490264.6997, 49.7455, 207.5411, 359.6715),
        1560 to DalilRow(2500895.7116, 291.1279, 141.6316, 37.5997),
        1590 to DalilRow(2511526.7235, 172.5102, 75.7221, 75.5278),
        1620 to DalilRow(2522157.7355, 53.8925, 9.8126, 113.4560),
        1650 to DalilRow(2532788.7474, 295.2748, 303.9031, 151.3842),
        1680 to DalilRow(2543419.7593, 176.6572, 237.9936, 189.3124),
        1710 to DalilRow(2554050.7712, 58.0395, 172.0841, 227.2406),
        1740 to DalilRow(2564681.7832, 299.4218, 106.1746, 265.1688),
        1770 to DalilRow(2575312.7951, 180.8041, 40.2651, 303.0970)
    )

    /** Jadwal gerak tahun Mabsuthah (1-30 tahun), halaman 158. */
    val tahunMabsuthah: Map<Int, DalilRow> = mapOf(
        1 to DalilRow(354.3671, 8.0461, 309.8030, 349.2643),
        2 to DalilRow(708.7341, 16.0922, 259.6060, 338.5285),
        3 to DalilRow(1063.1012, 24.1382, 209.4091, 327.7928),
        4 to DalilRow(1417.4683, 32.1843, 159.2121, 317.0571),
        5 to DalilRow(1771.8353, 40.2304, 109.0151, 306.3214),
        6 to DalilRow(2126.2024, 48.2765, 58.8181, 295.5856),
        7 to DalilRow(2480.5694, 56.3225, 8.6211, 284.8499),
        8 to DalilRow(2834.9365, 64.3686, 318.4241, 274.1142), // validasi: contoh Shafar -52H
        9 to DalilRow(3189.3036, 72.4147, 268.2272, 263.3785),
        10 to DalilRow(3543.6706, 80.4608, 218.0302, 252.6427),
        11 to DalilRow(3898.0377, 88.5069, 167.8332, 241.9070),
        12 to DalilRow(4252.4048, 96.5529, 117.6362, 231.1713),
        13 to DalilRow(4606.7718, 104.5990, 67.4392, 220.4355),
        14 to DalilRow(4961.1389, 112.6451, 17.2422, 209.6998),
        15 to DalilRow(5315.5060, 120.6912, 327.0453, 198.9641),
        16 to DalilRow(5669.8730, 128.7372, 276.8483, 188.2284),
        17 to DalilRow(6024.2401, 136.7833, 226.6513, 177.4926),
        18 to DalilRow(6378.6072, 144.8294, 176.4543, 166.7569),
        19 to DalilRow(6732.9742, 152.8755, 126.2573, 156.0212),
        20 to DalilRow(7087.3413, 160.9216, 76.0603, 145.2855),
        21 to DalilRow(7441.7083, 168.9676, 25.8634, 134.5497),
        22 to DalilRow(7796.0754, 177.0137, 335.6664, 123.8140),
        23 to DalilRow(8150.4425, 185.0598, 285.4694, 113.0783),
        24 to DalilRow(8504.8095, 193.1059, 235.2724, 102.3426), // validasi: contoh Ramadhan 1434H
        25 to DalilRow(8859.1766, 201.1519, 185.0754, 91.6068),
        26 to DalilRow(9213.5437, 209.1980, 134.8784, 80.8711),
        27 to DalilRow(9567.9107, 217.2441, 84.6845, 70.1354),
        28 to DalilRow(9922.2778, 225.2902, 34.4845, 59.3996),
        29 to DalilRow(10276.6449, 233.3362, 344.2875, 48.6639),
        30 to DalilRow(10631.0019, 241.3823, 294.0905, 37.9282)
    )

    /** Jadwal gerak bulan Hijriyah (1=Muharram s/d 12=Dzul Hijjah), halaman 158. */
    val bulan: Map<Int, DalilRow> = mapOf(
        1 to DalilRow(29.5306, 30.6705, 25.8169, 29.1054), // Muharram
        2 to DalilRow(59.0612, 61.3410, 51.6338, 58.2107), // Shafar
        3 to DalilRow(88.5918, 92.0115, 77.4508, 87.3161), // Rabi'ul Awwal
        4 to DalilRow(118.1224, 122.6820, 103.2677, 116.4214), // Rabi'ul Akhir
        5 to DalilRow(147.6529, 153.3525, 129.0846, 145.5268), // Jumadal Ula
        6 to DalilRow(177.1835, 184.0230, 154.9015, 174.6321), // Jumadal Akhiroh
        7 to DalilRow(206.7141, 214.6935, 180.7184, 203.7375), // Rajab
        8 to DalilRow(236.2447, 245.3641, 206.5353, 232.8428), // Sya'ban -- validasi
        9 to DalilRow(265.7753, 276.0346, 232.3523, 261.9482), // Ramadhan -- validasi
        10 to DalilRow(295.3059, 306.7051, 258.1692, 291.0536), // Syawwal
        11 to DalilRow(324.8365, 337.3756, 283.9861, 320.1589), // Dzul Qo'dah
        12 to DalilRow(354.3671, 8.0461, 309.8030, 349.2643) // Dzul Hijjah
    )
}

/**
 * Ta'dilul 'Alamah (T1-T8) untuk hisab Ijtima'. Ditranskrip sebagai tabel
 * lookup 31x6 di kitab (halaman 109-122), tapi terbukti (tervalidasi 2x
 * silang: contoh Sya'ban & Ramadhan 1434H, presisi 4 desimal) murni fungsi
 * `amplitudo x sin(dalil)` -- jadi disimpan sebagai konstanta, bukan tabel,
 * supaya presisi lebih tinggi & bebas galat interpolasi.
 */
internal object TadilAlamah {
    // T1: dalil Markaz (M)
    const val C1 = 0.1734
    // T2: dalil 2xMarkaz (2M)
    const val C2 = 0.0021
    // T3: dalil Khashshah (M') -- amplitudo negatif (tervalidasi 2x)
    const val C3 = -0.4068
    // T4: dalil 2xKhashshah (2M')
    const val C4 = 0.0161
    // T5: dalil Markaz+Khashshah (M+M') -- amplitudo negatif (tervalidasi 2x)
    const val C5 = -0.0051
    // T6: dalil Markaz-Khashshah (M-M') -- amplitudo negatif (tervalidasi 2x)
    const val C6 = -0.0074
    // T7: dalil 2xHishshatul 'Ardh (2F)
    const val C7 = 0.0104
    // T8: dalil 2xHishshatul 'Ardh - Khashshah (2F-M')
    const val C8 = 0.0010
}

/** Jadwal konversi tarikh Juliani ke Masehi, halaman 163. */
internal object JulianMasehiTables {
    /** Tahun Majmu'ah Miladiyah (kelipatan 100 tahun) -> hari Julian awal tahun tsb. */
    val tahunMajmuahMiladiyah: Map<Int, Long> = mapOf(
        400 to 1867157L, 500 to 1903682L, 600 to 1940207L, 700 to 1976732L,
        800 to 2013257L, 900 to 2049782L, 1000 to 2086307L, 1100 to 2122832L,
        1200 to 2159357L, 1300 to 2195882L, 1400 to 2232407L, 1500 to 2268932L,
        1600 to 2305447L, 1700 to 2341971L, 1800 to 2378495L, 1900 to 2415019L,
        2000 to 2451544L, 2100 to 2488068L, 2200 to 2524592L, 2300 to 2561116L,
        2400 to 2597641L, 2500 to 2634165L, 2600 to 2670689L, 2700 to 2707213L,
        2800 to 2743738L, 2900 to 2780262L
    )

    /** Tahun Mabsuthah Miladiyah (0-99 tahun) -> hari sejak awal tahun majmu'ah, halaman 163. */
    val tahunMabsuthahMiladiyah: Map<Int, Long> = listOf(
        0L, 365L, 730L, 1095L, 1461L, 1826L, 2191L, 2556L, 2922L, 3287L,
        3652L, 4017L, 4383L, 4748L, 5113L, 5478L, 5844L, 6209L, 6574L, 6939L,
        7305L, 7670L, 8035L, 8400L, 8766L, 9131L, 9496L, 9861L, 10227L, 10592L,
        10957L, 11322L, 11688L, 12053L, 12418L, 12783L, 13149L, 13514L, 13879L, 14244L,
        14610L, 14975L, 15340L, 15705L, 16071L, 16436L, 16801L, 17166L, 17532L, 17897L,
        18262L, 18627L, 18993L, 19358L, 19723L, 20088L, 20454L, 20819L, 21184L, 21549L,
        21915L, 22280L, 22645L, 23010L, 23376L, 23741L, 24106L, 24471L, 24837L, 25202L,
        25567L, 25932L, 26298L, 26663L, 27028L, 27393L, 27759L, 28124L, 28489L, 28854L,
        29220L, 29585L, 29950L, 30315L, 30681L, 31046L, 31411L, 31776L, 32142L, 32507L,
        32872L, 33237L, 33603L, 33968L, 34333L, 34698L, 35064L, 35429L, 35794L, 36159L
    ).withIndex().associate { (idx, v) -> idx to v }

    /** Bulan Miladiyah -> hari sejak awal tahun (0-indexed, non-leap; Feb leap +1 otomatis di kode). */
    val bulanMiladiyah: List<Pair<String, Int>> = listOf(
        "Januari" to 0, "Februari" to 31, "Maret" to 59, "April" to 90,
        "Mei" to 120, "Juni" to 151, "Juli" to 181, "Agustus" to 212,
        "September" to 243, "Oktober" to 273, "November" to 304, "Desember" to 334
    )
}

/**
 * Ta'dil untuk hisab Hilal (bujur/latitude/jarak Bulan & Matahari), halaman
 * 175-185. Sama seperti [TadilAlamah], semuanya terbukti murni
 * `amplitudo x sin(dalil)` (S1-S2, M1-M9, B1-B4) atau `amplitudo x cos(dalil)`
 * (r1-r4) -- tervalidasi terhadap contoh Sya'ban 1434H (Sampang) DAN Ramadhan
 * 1434H (Banyuwangi) di jurnal Notion, kecuali M8 & M9 yang cuma tervalidasi
 * 1x (Sya'ban) karena Ramadhan tidak mencantumkan rincian M1-M9-nya.
 *
 * R1/R2 (jarak Bumi-Matahari, dalil m/2m) SENGAJA tidak dipakai -- dampaknya
 * ke semidiameter Matahari cuma ~1-2 detik busur (jauh di bawah presisi
 * target metode ini), kitab sendiri sudah sediakan alternatif rumus
 * langsung (Ghurub Wasaty cara 2, halaman 12): sd = 0.267/(1-0.017*cos(m)).
 *
 * Kolom "D" (mail/obliquity) di tabel gerak Hilal ditandai kitab sendiri
 * sebagai "(tetap)" -- dipakai konstan [OBLIQUITAS], bukan tabel, karena
 * perubahan sungguhannya (~0.00013 derajat/tahun) tidak relevan untuk
 * rentang tanggal yang dipakai app ini.
 */
internal object TadilHilal {
    const val OBLIQUITAS = 23.437533

    // --- Bujur Matahari (S') ---
    /** S1: dalil Khashshah Matahari (m) */
    const val S1 = 1.9146
    /** S2: dalil 2xKhashshah Matahari (2m) */
    const val S2 = 0.0200

    // --- Bujur Bulan (Mo) ---
    /** M1: dalil Khashshah Bulan (A) */
    const val M1 = 6.2888
    /** M2: dalil 2xBu'd-Khashshah (2D-A) */
    const val M2 = 1.2740
    /** M3: dalil 2xBu'd (2D) */
    const val M3 = 0.6583
    /** M4: dalil 2xKhashshah Bulan (2A) */
    const val M4 = 0.2136
    /** M5: dalil Khashshah Matahari (m) -- amplitudo negatif (tervalidasi) */
    const val M5 = -0.1851
    /** M6: dalil 2xHishshah (2N) -- amplitudo negatif (tervalidasi) */
    const val M6 = -0.1143
    /** M7: dalil 2xBu'd-2xKhashshah (2D-2A) */
    const val M7 = 0.0588
    /** M8: dalil 2D-m-A -- amplitudo & tanda dari 1 titik validasi (Sya'ban) */
    const val M8 = 0.0570
    /** M9: dalil 2D+A */
    const val M9 = 0.0533

    // --- Latitude Bulan (B) ---
    /** B1: dalil Hishshah (N) */
    const val B1 = 5.1282
    /** B2: dalil Khashshah+Hishshah (A+N) */
    const val B2 = 0.2806
    /** B3: dalil Khashshah-Hishshah (A-N) */
    const val B3 = 0.2777
    /** B4: dalil 2xBu'd-Hishshah (2D-N) */
    const val B4 = 0.1732

    // --- Jarak Bumi-Bulan (r, km) -- semua cosinus, amplitudo negatif ---
    /** r1: dalil Khashshah Bulan (A) */
    const val R1 = -20905.355
    /** r2: dalil 2xBu'd-Khashshah (2D-A) */
    const val R2 = -3699.111
    /** r3: dalil 2xBu'd (2D) */
    const val R3 = -2955.968
    /** r4: dalil 2xKhashshah Bulan (2A) */
    const val R4 = -569.925
}
