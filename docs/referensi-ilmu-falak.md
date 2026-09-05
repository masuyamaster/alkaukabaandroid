# Referensi Ilmu Falak — Fase Bulan, Manazil, Hisab & Kriteria Hilal

> Ini dokumen **referensi keilmuan/domain**, bukan dokumen arsitektur fitur.
> Isinya rangkuman riset tentang Ilmu Falak (fase Bulan, manazil al-qamar,
> parameter visibilitas hilal, sejarah hisab Nusantara, dan kriteria fikih
> penentuan awal bulan) yang jadi latar belakang konseptual untuk fitur
> [Bulan Hijriyah / Awal Bulan](features/bulan-hijriyah.md) dan
> [Fase Bulan](features/fase-bulan.md). Kalau butuh rumus/implementasi teknis
> yang persis dipakai di kode, itu ada di dokumen fitur masing-masing dan di
> [rumus-hisab-ephemeris.md](features/rumus-hisab-ephemeris.md) — dokumen ini
> untuk konteks "kenapa" di balik istilah dan kriteria yang dipakai/ditampilkan
> di app (mis. kartu kriteria Neo-MABIMS di layar Awal Bulan).

## 1. Ilmu Falak: definisi & enam nomenklatur

*Falak* secara etimologis berarti orbit/lintasan benda langit. Disiplin ini
punya enam nama yang merepresentasikan fokus kajiannya masing-masing:

| Nama | Fokus |
|---|---|
| Ilmu Falak | Lintasan benda langit secara umum |
| Ilmu Rashd | Observasi empiris pakai instrumen visual |
| Ilmu Miqat | Penentuan batas waktu ibadah (awal waktu salat, awal bulan) |
| Ilmu Hisab | Operasi matematis/algoritma perhitungan koordinat |
| Ilmu Hai'ah | Struktur makrokosmos/kosmologi alam semesta |
| Ilmu Handasah | Geometri ruang & trigonometri bola (spherical trigonometry) |

Landasan teologis kalender Kamariah (lunar) ada di Al-Qur'an — QS Yunus:5,
Yasin:39, Al-Isra:12 — yang menyebut Matahari sebagai *dhiya'* (sumber
cahaya) dan Bulan sebagai *nur* (pemantul cahaya), serta penetapan *manazil*
(fase/stasiun orbit) bagi Bulan supaya manusia bisa menghitung tahun & bulan.

**Periode sinodis** Bulan (interval kembali ke ijtimak yang sama) = **29,5306
hari** (29 hari 12 jam 44 menit) — bukan bilangan bulat, sehingga umur bulan
Hijriyah selalu 29 atau 30 hari (prinsip *istikmal* kalau digenapkan 30).
Pergantian hari dimulai saat piringan atas Matahari terbenam penuh (ghurub),
bukan tengah malam — makanya observasi hilal terpusat di senja hari ke-29.

## 2. Delapan fase Bulan (fase iluminasi)

Fase Bulan murni ilusi optik geometris dari posisi relatif Bumi-Bulan-Matahari
(**bukan** bayangan Bumi menutupi Bulan — itu gerhana, fenomena berbeda).
Periode sideris (relatif bintang jauh) = 27,3 hari; periode sinodis (relatif
Matahari, yang dipakai kalender) = 29,5 hari karena Bumi juga bergerak
mengelilingi Matahari. Bulan bergeser ~12°/hari ke timur relatif Matahari,
terbit ~50 menit lebih lambat tiap hari.

| Fase Astronomis | Istilah Falakiyah | Karakteristik |
|---|---|---|
| New Moon | Ijtimak / Konjungsi | Bujur ekliptika Bulan = Matahari, iluminasi 0%, tidak terlihat |
| Waxing Crescent | Hilal / Sabit Muda | Sesaat pasca ijtimak, sabit tipis mulai tampak — penanda awal bulan Hijriyah |
| First Quarter | Tarbi' Awwal | ~hari ke-7, elongasi ~90°, separuh kanan bersinar |
| Waxing Gibbous | Cembung Awal | >separuh tersinari, cahaya terus bertambah |
| Full Moon | Badr / Purnama | Tanggal 13-15 (Ayyamul Bidh), elongasi 180°, piringan penuh |
| Waning Gibbous | Cembung Akhir | Elongasi >180°, mulai menyusut dari kanan |
| Last Quarter | Tarbi' Akhir | ~minggu ke-3, elongasi ~270°, separuh kiri bersinar |
| Waning Crescent | Sabit Tua | Sabit tipis di kiri, terlihat sesaat sebelum fajar (morning crescent) |
| Dark Moon | Muhak / Bulan Mati | 1-2 hari menjelang ijtimak berikutnya, tenggelam dalam silau Matahari |

Catatan implementasi: app membedakan dua "phase angle" berbeda dari
Astronomy Engine (`illumination().phaseAngle` vs `moonPhase()`) — detail
teknisnya ada di [fase-bulan.md §4](features/fase-bulan.md).

## 3. Manazil Al-Qamar (28 tempat persinggahan Bulan)

Selain fase iluminasi, astronomi Islam klasik mengembangkan *Manazil
Al-Qamar* — 28 kawasan langit yang dilewati orbit Bulan (~12°51' busur per
manzilah), masing-masing dinamai dari bintang/pola bintang dominan di
kawasan itu (mis. Asy-Syaratain, Ats-Tsuraya, Ad-Dabaran, Al-Haq'ah). Orbit
Bulan miring ~5° terhadap ekliptika tapi tetap melintasi rasi-rasi zodiakal
yang sama (Taurus, Leo, Virgo, Scorpio, Sagittarius, dst).

Fungsi historisnya: masyarakat pra-kalender-tertulis bisa mengetahui tanggal
berjalan dari manzilah tempat Bulan berada malam itu (berguna kalau hilal
tak terlihat karena mendung berhari-hari), sekaligus jadi pedoman navigasi
bahari dan siklus agrikultur. Dasar teologisnya: QS Yasin:39 — Bulan
ditetapkan manzilah-manzilahnya hingga kembali seperti pelepah kurma tua
(*urjunil qadim*, merujuk fase sabit tua).

## 4. Parameter visibilitas hilal

Ijtimak (konjungsi geosentris, umur Bulan = 0) tidak otomatis berarti hilal
terlihat — perlu parameter fisis berikut dihitung saat ghurub hari ke-29:

- **Irtifa' (Ketinggian/Altitude)** — jarak sudut vertikal dari ufuk ke
  piringan Bulan. Makin tinggi, makin terhindar dari ekstingsi atmosfer
  bawah (debu/uap air/polusi).
- **Elongasi (Arc of Light)** — jarak sudut Bulan-Matahari. Menentukan jarak
  dari silau Matahari sekaligus ketebalan sabit yang tersinari.
- **Umur Bulan (Moon Age)** — waktu sejak ijtimak sampai ghurub. Makin tua,
  makin besar elongasi & ketinggian.
- **Beda Azimut** — jarak sudut horizontal ufuk barat antara titik terbenam
  Matahari dan Bulan. Makin besar, makin keluar dari kolom silau senja.
- **Lag Time (Mukuts)** — durasi antara ghurub Matahari dan ghurub Bulan;
  jendela waktu observasi (ini istilah yang sama dengan field "mukuts" di
  hasil hisab app — lihat [bulan-hijriyah.md §4](features/bulan-hijriyah.md)).

Karena observasi dilakukan dari permukaan Bumi (toposentris, bukan
geosentris), hisab modern mengoreksi: **Horizontal Parallax** (pergeseran
sudut pandang pengamat vs pusat Bumi), **Refraksi** (pembiasan atmosfer,
benda tampak lebih tinggi dari posisi aslinya), **Semi-Diameter** (jari-jari
angular piringan Bulan), dan **Dip of Horizon** (kerendahan ufuk, bergantung
elevasi lokasi pengamat).

## 5. Limit Danjon & Kriteria Odeh

- **Limit Danjon** (André Danjon, 1932): hilal tidak bisa membentuk sabit
  berkesinambungan kalau elongasi **< 7°** dari Matahari. Bradley Schaefer
  (1990-an) merasionalisasi: relief topografi Bulan (dinding kawah,
  pegunungan) memutus sabit tipis di elongasi ekstrem. Data observasi
  kontemporer merevisi batas minimum ke **~6,4°**.
- **Kriteria Odeh** (Mohammad Odeh, 2004): regresi statistik dari data
  International Crescent Observation Project (ICOP, 1859-2005),
  mengklasifikasikan visibilitas ke Zona A (mudah dilihat mata telanjang),
  Zona B (perlu bantuan teleskop dulu), Zona C (hanya teleskop), dan zona
  tidak mungkin terlihat (< limit Danjon 7°).
- Kriteria internasional lain sebagai referensi: Yallop (1997), Ilyas
  (1984), Bruin (1977), SAAO (1995).

## 6. Sejarah hisab di Nusantara

Ilmu falak berakulturasi sistematis di Nusantara abad ke-19 — awal abad
ke-20, didorong kebutuhan praktis: arah kiblat, waktu salat, awal Ramadan/
Syawal.

- **KH Sholeh Darat** (Muhammad Sholeh bin Umar al-Samarani, lahir Jepara
  1820, mendirikan pesantren Darat Semarang) — mahaguru sentral transmisi
  ilmu falak dari Haramain ke Jawa, berguru falak ke K. Abu 'Abdillah
  Muhammad al-Hadi ibn Ba'uni (mufti Semarang). Murid-muridnya membentuk
  wajah Islam Nusantara: **KH Hasyim Asy'ari** (pendiri NU) dan **KH Ahmad
  Dahlan** (pendiri Muhammadiyah).
- **Sullamun Nayyirain** (1925) — karya **KH Muhammad Mansur bin Abdul
  Hamid** ("Guru Mansur al-Batawi"), tonggak hisab Indonesia. Menghitung
  ijtimak, tinggi hilal, gerhana, berbasis tabel astronomi (zeij) Sultan
  Ulugh Beg as-Samarqandi (abad ke-15). Keunikannya: metode aproksimasi
  (tabel wasat/khassah/markas) tanpa perlu trigonometri bola penuh — tinggi
  hilal dihitung sederhana dari separuh selisih waktu ghurub dan ijtimak
  (asumsi geosentris Ptolemy, Bulan bergerak konstan 12°/hari). Akurasinya
  di bawah standar modern (belum ada koreksi gerak harian tak-beraturan &
  toposentris) tapi jadi pegangan standar pesantren (mis. Lajnah Falakiyah
  PPMH Malang) dan fondasi kemandirian hisab Indonesia.

## 7. Evolusi hierarki metode hisab

1. **Hisab 'Urfi** — kalender aritmatika siklik (30 tahun/windu, 11 tahun
   kabisat 355 hari + 19 tahun basitah 354 hari), umur bulan baku
   berselang-seling, tidak merujuk observasi Bulan riil. Sudah ditinggalkan
   untuk penetapan ibadah, tersisa nilai historis/sosiologis saja.
2. **Hisab Hakiki Taqribi** — menghitung posisi Bulan "sebenarnya" tapi
   pakai rumus pendekatan & tabel sederhana, tanpa trigonometri bola penuh.
   *Sullamun Nayyirain* masuk kategori ini.
3. **Hisab Hakiki Tahqiqi** — trigonometri bola penuh + koefisien gerak
   Matahari/Bulan presisi (inklinasi, equation of time), error sangat kecil.
4. **Hisab Hakiki Kontemporer (Ephemeris/Software)** — basis data ephemeris
   tervalidasi lembaga antariksa, memperhitungkan perturbasi gravitasi
   antar-planet. Algoritma modern: **VSOP87D** (~2.462 suku koreksi posisi
   Matahari) dan **ELP/MPP02** (~35.901 suku koreksi posisi Bulan) — total
   ~38.363 suku koreksi. Contoh software: WinHisab (Kemenag), Accurate
   Times (Odeh/Jordanian Astronomical Society), Stellarium, Hisab Astronomis
   (PP Persis). **Ini kategori yang dipakai app** lewat "Astronomy Engine"
   (`io.github.cosinekitty.astronomy`, lihat [bulan-hijriyah.md §5](features/bulan-hijriyah.md)
   dan [fase-bulan.md](features/fase-bulan.md)) — bukan tabel klasik.

## 8. Kriteria fikih: Wujudul Hilal vs Imkanur Rukyat Neo-MABIMS

Karena software modern sudah menyatukan hasil koordinat astronomis,
perbedaan penetapan awal bulan (mis. 1 Ramadan/Syawal) bukan soal akurasi
hitungan, melainkan **ambang batas hukum** yang dipakai:

**Wujudul Hilal** (Majelis Tarjih Muhammadiyah) — awal bulan sah begitu
hilal *berwujud* secara fisis di atas ufuk saat ghurub, terlepas dari bisa
tidaknya dilihat mata. Tiga syarat kumulatif hari ke-29: (1) ijtimak telah
terjadi, (2) ijtimak sebelum ghurub, (3) piringan Bulan di atas ufuk saat
ghurub (tinggi > 0°) — meski hanya 0,1° yang secara fisika optik (Limit
Danjon) mustahil dirukyat. Dipakai dengan prinsip *Wilayatul Hukmi*
nasional — kalau kriteria terpenuhi di titik ufuk barat terjauh negara,
berlaku untuk seluruh wilayah.

**Imkanur Rukyat** (Pemerintah RI, NU, Persis, forum **MABIMS** — Brunei,
Indonesia, Malaysia, Singapura) — hisab dipakai sebagai *filter* kelayakan
optis, bukan penentu langsung; kesaksian rukyat yang lolos filter hisab baru
sah, yang gagal filter didiskualifikasi otomatis (bulan di-*istikmal* 30
hari). Kriteria lama "2-3-8" (tinggi ≥2°, elongasi ≥3°, umur ≥8 jam) dikritik
karena elongasi 3° jauh di bawah Limit Danjon 7° (mustahil secara fisika).
Sejak awal 2022, diperbarui jadi **Kriteria Neo-MABIMS** (Rekomendasi
Jakarta 2017): **tinggi hilal toposentris ≥ 3°** dan **elongasi geosentris ≥
6,4°** — inilah kriteria yang ditampilkan di kartu kesimpulan layar Awal
Bulan app ini (lihat [bulan-hijriyah.md §5 poin 6](features/bulan-hijriyah.md)).

Konsekuensi: dengan ambang Neo-MABIMS yang lebih ketat, gap potensi beda
tanggal dengan Muhammadiyah (yang tetap pakai Wujudul Hilal, tinggi >0°)
justru bisa melebar dibanding kriteria lama.

## 9. Kalender Hijriah Global Tunggal (KHGT)

Merespons perbedaan hari raya antarnegara, *International Hijri Calendar
Unity Congress* (Istanbul, 28-30 Mei 2016) mendeklarasikan **KHGT**: "Satu
Hari, Satu Tanggal, untuk Seluruh Dunia" — mendobrak konsep *wilayatul
hukmi* lokal lewat *Ittihadul Matali'* (matla global): kalau hilal valid
terlihat di manapun di Bumi, keterlihatan itu ditransfer ke seluruh dunia.

Kriteria Turki 2016: ijtimak harus tuntas sebelum 00:00 GMT; tinggi hilal
minimal **5°**, elongasi minimal **8°**; ada klausul penundaan kalau hilal
baru tertangkap setelah 00:00 GMT (bulan baru ditunda ke lusa untuk kawasan
timur). Muhammadiyah merencanakan transisi bertahap dari Wujudul Hilal
nasional ke KHGT penuh mulai 1447 H/2025 M. Masih ada resistensi akademik
karena menuntut penundaan ibadah puasa/berbuka lokal berdasarkan kesaksian
rukyat di benua lain, tanpa otoritas tunggal (khalifah/ulil amri) yang
mengikat semua pihak.

## 10. Teknologi rukyat modern (image processing)

Rukyat modern (BMKG, observatorium kampus) tidak lagi murni mata telanjang:
teleskop (mis. Vixen ED-103S, VC200L) + star tracking terkomputerisasi
mengunci posisi hilal meski belum terlihat, cahaya diteruskan ke sensor CCD
(peka near-infrared, mengurangi efek hamburan Rayleigh debu senja) alih-alih
ke okuler. Citra RAW diproses software (APT, IRIS) dengan **contrast
stretching** untuk memisahkan piksel hilal dari latar syafaq. Teknik
**CLAHE** (Contrast Limited Adaptive Histogram Equalization) unggul
dibanding Histogram Equalization biasa karena membagi citra jadi blok kecil
— mencegah amplifikasi silau syafaq tapi tetap melambungkan piksel sabit
hilal. Beberapa institusi fikih memvalidasi hasil CCD+software lewat qiyas
sebagai alat bantu (setara kacamata), sepanjang tetap terkonfirmasi hisab
dan parameter fisik (elongasi-ketinggian) sesuai kriteria MABIMS.

## 11. Ringkasan poin kunci

- Perdebatan kriteria awal bulan (Wujudul Hilal vs Imkanur Rukyat
  Neo-MABIMS vs KHGT) adalah beda **ambang hukum penerimaan**, bukan beda
  akurasi hisab — software modern (VSOP87D/ELP-MPP02, dipakai app ini lewat
  Astronomy Engine) sudah menyamakan angka koordinat astronomisnya.
- Angka **6,4°** (elongasi Neo-MABIMS) dan **7°** (Limit Danjon klasik)
  bukan angka sembarang — keduanya turunan langsung batas fisis pembentukan
  sabit yang bisa dideteksi mata/kamera.
- App ini menampilkan kriteria **Neo-MABIMS** saja (bukan Wujudul Hilal atau
  KHGT) sebagai kesimpulan status hilal — keputusan yang sudah dicatat di
  [bulan-hijriyah.md §5 poin 6](features/bulan-hijriyah.md).

## 12. Sumber

Rangkuman ini disusun dari riset naratif (kompilasi materi Ilmu Falak: fase
Bulan, manazil, Limit Danjon/Kriteria Odeh, sejarah hisab Nusantara,
Wujudul Hilal/Imkanur Rukyat Neo-MABIMS, KHGT) yang didiskusikan bersama
user pada 2026-09-05 — dokumen sumber tidak menyertakan daftar pustaka
formal per klaim; kalau ada kebutuhan verifikasi angka/tanggal presisi
(mis. tanggal pasti kongres Istanbul, tokoh sejarah), cross-check ke sumber
primer sebelum dipakai sebagai rujukan hukum/keputusan fitur baru.
