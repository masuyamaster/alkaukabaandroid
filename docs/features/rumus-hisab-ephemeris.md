# Rumus Hisab Ephemeris — Waktu Sholat

> Ini dokumen **referensi rumus**, bukan dokumen arsitektur fitur (itu ada di
> [`waktu-sholat.md`](waktu-sholat.md)). Isinya transkrip rumus & contoh
> perhitungan manual dari handout **"Perhitungan Waktu Sholat"** oleh
> M. Khoirul Anam (Ketua Lajnah Falakiyah PCNU Kab. Lamongan), yang jadi acuan
> implementasi `EphemerisPrayerCalculator.kt`. Kalau ada perbedaan antara kode
> dan dokumen ini di kemudian hari, dokumen ini yang jadi rujukan "kenapa
> rumusnya begini", bukan komentar di kode.

## 1. Data yang dibutuhkan

| Simbol | Arti |
|---|---|
| φ (Lintang tempat) | Derajat, negatif untuk Lintang Selatan |
| λtp (Bujur tempat) | Derajat, BT positif |
| δ° (Deklinasi matahari) | Dari data Ephemeris, berubah tiap hari/jam |
| e° (Equation of time / perata waktu) | Dari data Ephemeris |
| h° (Tinggi matahari) | Berbeda per waktu sholat, lihat tabel section 3 |
| Kwd (Koreksi waktu daerah) | `(λdh - λtp) / 15`, λdh = bujur meridian standar zona waktu (WIB 105°, WITA 120°, WIT 135°) |
| i (Ikhtiyat) | Kehati-hatian, 1-2 menit |

## 2. Rumus inti

**Sudut waktu matahari (t):**

```
Cos t = - tan φ · tan δ + sin h / cos φ / cos δ
```

(setara dengan bentuk baku `cos t = (sin h - sin φ sin δ) / (cos φ cos δ)`).

**Rumus umum awal waktu:**

```
Awal waktu = 12 - e + t + Kwd + i
```

Ketentuan per waktu (section 3 di bawah untuk nilai h° masing-masing):

- **Dzuhur**: rumus dipakai tanpa `t` → `12 - e + Kwd + i`.
- **Ashar, Maghrib, Isya** (sesudah istiwa'/kulminasi): `t` ditambahkan →
  `12 - e + t + Kwd + i`.
- **Subuh, Terbit/Syuruq, Dhuha** (sebelum istiwa'): `t` dikurangkan →
  `12 - e - t + Kwd + i`.
- **Khusus Terbit/Syuruq**: `i` ikut dikurangkan (bukan ditambah), jadi
  `12 - e - t + Kwd - i` — supaya waktu Terbit tidak dinyatakan lebih lambat
  dari kejadian sebenarnya.

**Tinggi matahari untuk Ashar** (bukan sudut tetap seperti waktu lain, tapi
dihitung dari panjang bayangan = panjang benda + bayangan saat istiwa'):

```
Cotan h° = tan |φ - δ| + 1
```

## 3. Tinggi matahari (h°) per waktu sholat

| Waktu | h° | Catatan |
|---|---|---|
| Terbit/Syuruq | dari ufuk Timur (hasil hitung, contoh di bawah: dekat 0°, tergantung tanggal) | |
| Imsak | -22° | dari ufuk Timur |
| Subuh | -20° | dari ufuk Timur |
| Dhuha | 4,5° | dari ufuk Timur |
| Dzuhur | - | `t = 0` (matahari di meridian) |
| Ashar | `arccot(tan|φ-δ|+1)` | lihat rumus di atas |
| Maghrib | -1° | dari ufuk Barat |
| Isya | -18° | dari ufuk Barat |

Imsak tidak eksplisit di rumus umum buku (paragraf umumnya cuma menyebut
Maghrib/Isya/Subuh/Terbit/Dhuha), tapi contoh perhitungannya memakai struktur
yang sama dengan Subuh (`12-e-t+Kwd`, h°=-22° yaitu Subuh dikurangi 2°) —
margin kehati-hatian Imsak sudah melekat di sudut -22°-nya, bukan ditambah
ikhtiyat terpisah lagi.

## 4. Contoh perhitungan manual (rujukan validasi)

Lokasi: **Lamongan**, tanggal **1 Januari 2009**.

- φ = -07° 08' LS, λtp = 112° 25' BT
- Kwd = (105° - 112°25') / 15 = **-0° 29' 40"**

Data Ephemeris per waktu (deklinasi & equation of time beda tiap jam GMT
acuan — ini karakteristik data tabel buku Ephemeris asli, bukan nilai
konstan sepanjang hari):

| Waktu | Jam GMT acuan | δ° | e° | h° | t/15 | Hasil (WIB) |
|---|---|---|---|---|---|---|
| Dzuhur | 05:00 | - | -0°03'32" | - | - | **11:35** |
| Ashar | 08:00 | -22°58'51" | -0°03'35" | 37°54'54" | 03:26:51 | **15:02** |
| Maghrib | 11:00 | -22°58'13" | -0°03'39" | -1° | 06:16:33 | **17:52** |
| Isya' | 12:00 | -22°58'00" | -0°03'40" | -18° | 07:32:08 | **19:08** |
| Subuh | 21:00 (31 Des) | -23°01'07" | -0°03'22" | -20° | 07:41:17 | **03:54** |
| Imsak | 21:00 (31 Des) | -23°01'07" | -0°03'22" | -22° | 07:50:25 | **03:44** |
| Terbit/Syuruq | 22:00 (31 Des) | -23°00'55" | -0°03'23" | 1° | 06:07:48 | **05:26** |
| Dhuha | 00:00 | -23°00'31" | -0°03'26" | 4,5° | 05:52:29 | **05:43** |

Ikhtiyat yang dipakai di contoh ini berkisar 1-2 menit (mis. Dzuhur +1'08",
Isya +1'52") — nilai-nilai kecil ini adalah pembulatan hasil kalkulator saat
itu, bukan konstanta baku, sehingga implementasi app memakai nilai tetap
(2 menit) untuk konsistensi lintas tanggal/lokasi — lihat bagian "Perbedaan
dengan implementasi app" di bawah.

## 5. Perbedaan dengan implementasi app (`EphemerisPrayerCalculator.kt`)

Buku rujukan mengambil δ° dan e° dari **tabel Ephemeris tercetak** (nilai per
jam, per tanggal, dilihat manual dari buku). App ini **tidak** replikasi
tabel tersebut secara statis — sebagai gantinya, δ° dan e° diturunkan dari
posisi matahari riil hasil "Astronomy Engine" (`utils/Astronomy.kt`, sudah
dipakai juga untuk Awal Bulan Hijriyah di `EphemerisCalculator.kt`), lewat
waktu transit/istiwa' yang dicari dengan `searchHourAngle(Sun, 0°)`. Ini
setara secara prinsip (posisi matahari riil, bukan tabel), lebih presisi
(kontinu per detik, bukan interpolasi tabel per jam), dan otomatis berlaku
untuk tanggal/lokasi manapun tanpa perlu tabel data terpisah di app.

Yang **persis sama** dengan buku: rumus gabungan tiap waktu sholat (Kwd, h°
per waktu, `cos t`, `cotan h°` untuk Ashar, arah tanda `+t`/`-t`/`-i` untuk
Terbit) — lihat section 2-3 di atas, diimplementasikan apa adanya di kode.

Yang **disederhanakan** dari buku: ikhtiyat dipakai konstanta 2 menit
(ditambah untuk Subuh/Dzuhur/Ashar/Maghrib/Isya/Dhuha, dikurangi untuk
Terbit, nol untuk Imsak) — bukan angka hasil pembulatan kalkulator per-kasus
seperti di contoh section 4.

## 6. Referensi

Handout asli: *"Perhitungan Waktu Sholat"*, oleh M. Khoirul Anam (Ketua
Lajnah Falakiyah PCNU Kab. Lamongan, Pengurus Badan Hisab Rukyat Prop. Jatim),
sumber data: Buku Ephemeris Hisab Rukyat.
