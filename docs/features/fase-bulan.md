# Fase Bulan

## 1. Ringkasan

**Fitur**: Fase Bulan — kartu di home screen yang menampilkan ilustrasi
fase bulan saat ini (real-time, berdasarkan waktu sistem device, dirender
pakai tekstur foto bulan asli — lihat §4) beserta nama fase dan persentase
permukaan yang tersinari. Tap kartu membuka layar detail dengan ilustrasi
lebih besar, kartu "Detail Astronomis" (magnitude, jarak, radius, RA/Dec,
Az/Alt, jam terbit/terbenam), dan jadwal 4 fase bulan mendatang (bulan
baru/kuartal pertama/purnama/kuartal akhir).

Sengaja ditaruh sebagai section/kartu terpisah di home, **bukan** ikon ke-5 di
baris 4-menu utama (Arah Kiblat/Waktu Sholat/Awal Bulan/Gerhana) — baris itu
`weightSum="4"` dan didesain sebagai 4 aksi cepat, bukan tempat untuk fitur
informasional/dekoratif seperti ini.

Semua perhitungan pakai "Astronomy Engine" (`utils/Astronomy.kt`,
`io.github.cosinekitty.astronomy`) yang sudah dipakai fitur Bulan Hijriyah &
Gerhana — tidak ada perhitungan astronomi baru yang ditambahkan.

## 2. Entry point & prasyarat

- Kartu "Fase Bulan Malam Ini" di `activity_main.xml` (`cardMoonPhase` /
  `btnMoonPhase`), antara baris 4-menu dan section "Kalender".
- Tap kartu → `FaseBulanActivity` (layout `activity_fase_bulan.xml`).
- Prasyarat inti (ilustrasi, nama fase, %, magnitude, jarak, radius, RA/Dec,
  fase mendatang): tidak ada — murni perhitungan waktu (tanggal & jam device)
  tanpa panggilan network.
- Prasyarat tambahan khusus kartu "Detail Astronomis" di layar detail: izin
  lokasi (`ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`), dibutuhkan untuk
  Az/Alt dan jam terbit/terbenam Bulan (toposentris, beda per lokasi
  pengamat) — lihat §4. Kalau izin ditolak atau lokasi belum siap, dua field
  ini tampil "-"; field lain tidak terpengaruh.

## 3. Titik masuk logika & navigasi

- `MainActivity.setupMoonPhaseCard()` — dipanggil dari `onCreate`, hitung fase
  saat ini dan isi kartu home, pasang `OnClickListener` ke `FaseBulanActivity`.
- `FaseBulanActivity.showCurrentPhase()` — versi lebih besar dari logika yang
  sama di kartu home, ditambah magnitude/jarak/radius (geosentris, tidak
  butuh lokasi).
- `FaseBulanActivity.checkLocationPermission()` → `onLocationReady()` — alur
  izin & fetch lokasi (pola sama dengan `KiblatActivity`: cek
  `SessionManager.isManualLocationMode()` dulu, baru fallback ke
  `FusedLocationProviderClient`), lalu hitung RA/Dec topocentric, Az/Alt, dan
  jam terbit/terbenam Bulan buat kartu "Detail Astronomis".
- `FaseBulanActivity.loadUpcomingQuarters()` — cari 4 fase mendatang lewat
  `searchMoonQuarter`/`nextMoonQuarter`, dijalankan di `Dispatchers.Default`
  (iteratif/pencarian akar, dipindah dari main thread) lalu bind ke UI di
  `Dispatchers.Main`.
- `MoonPhaseView` (`ui/widget/MoonPhaseView.kt`) — custom `View` yang reusable,
  dipakai di kartu home (56dp), layar detail (160dp), dan ilustrasi hilal di
  `AwalBulanActivity`, lewat method publik
  `setPhase(phaseAngleDegrees, illuminatedFraction)`.
- `MoonPhaseLabel.forAngle()` (`utils/MoonPhaseLabel.kt`) — mapping sudut fase
  sinodik ke 8 nama fase. Per 2026-09-05, dua iterasi sebelum settle ke bentuk
  final: sempat diganti ke istilah falakiyah dari tabel "Al-Azhillah
  Al-Qamariyah" (Ijtimak, Hilal/Sabit Muda, Tarbi' Awwal, dst), lalu sempat ke
  terjemahan Indonesia (Bulan Baru, Sabit Awal, Kuartal Pertama, dst) — versi
  final memakai istilah **Fase Astronomis** apa adanya dalam Bahasa Inggris:
  New Moon, Waxing Crescent, First Quarter, Waxing Gibbous, Full Moon, Waning
  Gibbous, Last Quarter, Waning Crescent — persis kolom "Fase Astronomis" di
  [referensi-ilmu-falak.md §2](../referensi-ilmu-falak.md) (istilah falakiyah
  di kolom sebelahnya tetap didokumentasikan di sana sebagai referensi, tapi
  tidak dipakai di UI). Label statis di kartu "Fase Mendatang"
  (`activity_fase_bulan.xml`, 4 baris) mengikuti keputusan yang sama.

## 4. Struktur & alur data

| File | Peran |
|---|---|
| `ui/widget/MoonPhaseView.kt` | Custom `View`: bentuk sabit/cembung dari dua-arc `Path` (lebar elips terminator = `r * |1 - 2k|`, `k` = fraksi tersinari). Di-fill pakai `BitmapShader` dari `res/drawable-nodpi/moon_texture.jpg` (bukan warna flat) supaya terlihat foto asli — bisa dimatikan per-instance lewat `setRealisticTexture(false)` (fallback ke `flatBrightPaint` putih flat), dipakai `AwalBulanActivity` untuk ilustrasi hilal. Per 2026-09-05 (lihat §4 lanjutan di bawah untuk detail lengkap termasuk 2 iterasi salah yang sempat ter-commit): sisi malam tetap piringan solid opak (`nightBasePaint`, blok apa pun di belakangnya) tapi tekstur terangnya "dihapus" di sisi malam dengan tepi melembut alami (bukan lagi garis tajam) — bisa dimatikan lewat `setSoftNightSide(false)` (kembali ke tepi tajam, dipakai `AwalBulanActivity` untuk kejelasan bentuk hilal saat rukyah). |
| `res/drawable-nodpi/moon_texture.jpg` | Foto purnama Bulan asli, di-crop dari `File:FullMoon2010.jpg` di Wikimedia Commons — © Gregory H. Revera, lisensi CC BY-SA 3.0 (**atribusi wajib** kalau file/versi turunannya didistribusikan lagi di luar app ini; ditampilkan sebagai UI di dalam app tidak butuh watermark, tapi kredit ini harus tetap ada di sini & tidak boleh dihapus). `drawable-nodpi` supaya didekode di resolusi piksel aslinya di semua densitas device, bukan di-scale otomatis. Per 2026-09-05: sumber awal (`File:Full moon.jpeg`, NASA, domain publik) diganti karena piringan bulannya tidak simetris dalam frame (margin kiri/atas ada, kanan/bawah nol — disc-nya kepotong tepi foto), bikin celah kelihatan antara tepi foto & lingkaran gold yang digambar `MoonPhaseView`. `FullMoon2010.jpg` di-crop presisi (deteksi bounding box piksel non-hitam via kode Java sekali pakai, radius = setengah sisi terpanjang bbox, crop persegi center pas di situ) supaya piringannya nyaris pas isi seluruh frame simetris (margin ~4-5px di semua sisi pada file 900×900) — align rapi dengan lingkaran yang digambar, tanpa celah. |
| `utils/MoonPhaseLabel.kt` | 8 bucket nama fase (masing-masing 45°) dari sudut sinodik 0-360°. |
| `ui/fasebulan/FaseBulanActivity.kt` + `activity_fase_bulan.xml` | Layar detail: kartu hitam besar (ilustrasi + nama fase + %), kartu putih "Detail Astronomis" (magnitude/jarak/radius/RA-Dec/Az-Alt/terbit-terbenam), lalu kartu putih daftar 4 fase mendatang. |
| `MainActivity.kt` | `setupMoonPhaseCard()` mengisi kartu home + wiring klik ke `FaseBulanActivity`. |
| `activity_main.xml` | Kartu `cardMoonPhase`/`btnMoonPhase` (background `@color/black`, per 2026-09-06 - lihat catatan di bawah). |
| `ui/widget/ZoomableImageView.kt` + `res/layout/dialog_moon_zoom.xml` | Modal zoom (per 2026-09-05): tap ilustrasi 160dp di layar detail → `FaseBulanActivity.showMoonZoomDialog()` render `moonPhaseView.renderToBitmap(1024)` ke `Dialog` fullscreen custom (`android.R.style.Theme_Black_NoTitleBar_Fullscreen`, bukan `BottomSheetDialog` seperti dialog lain di app ini — drag-to-dismiss bottom sheet akan bentrok dengan gesture pan saat zoom). `ZoomableImageView` (extends `AppCompatImageView`, `scaleType=MATRIX`) implementasi pinch-zoom/pan/double-tap manual pakai `ScaleGestureDetector`+`GestureDetector`+`Matrix` — bukan library (`PhotoView` dkk) karena repo belum punya dependency image-zoom apa pun & kebutuhannya sederhana (satu bitmap persegi). `StarfieldView` (lihat baris di bawah) juga dipasang di belakang `ZoomableImageView` di layout ini, supaya background hitam modal ikut penuh bintang seperti kartu Fase Bulan. Known gap: ada strip tipis warna cream di ujung atas modal (area status bar) yang belum berhasil dihilangkan meski sudah dicoba beberapa pendekatan (`WindowCompat.setDecorFitsSystemWindows`, `FLAG_LAYOUT_NO_LIMITS`, `WindowInsetsControllerCompat.hide()`, `window.setLayout(MATCH_PARENT,...)`) — dugaan sementara terkait `targetSdk 36` yang meng-enforce edge-to-edge dengan cara yang belum cocok dengan tema fullscreen dialog lama ini; murni kosmetik, tidak mengganggu fungsi zoom. |
| `ui/widget/StarfieldView.kt` | Custom `View` (per 2026-09-05): gambar ~70 bintang (posisi/ukuran/alpha random tapi seed tetap, jadi tidak "berkedip" tiap `invalidate()`) sebagai layer paling belakang di kartu Fase Bulan (`activity_fase_bulan.xml` & `activity_main.xml`, sebelum `ic_sparkle`/`dot_star` yang sudah ada) — supaya kartu terasa seperti langit malam sungguhan, bukan cuma gradient + 1-2 titik dekoratif. Tidak dipakai di kartu hilal `AwalBulanActivity` (tidak diminta). |

**Sisi malam opak + terminator lembut (per 2026-09-05, revisi final):**
sebelumnya sisi malam digambar warna flat navy (`shadowPaint`) dan batas
terminator selalu berupa garis tajam (langsung dari geometri `Path`) —
dibanding aplikasi astronomi lain yang batas terminatornya melembut alami,
hasil kita terlihat "kaku". Perbaikan ini melalui 3 iterasi sebelum benar;
dua iterasi awal SALAH dan sempat ter-commit — dicatat di sini supaya tidak
diulang kalau ada yang menyentuh area ini lagi.

**Iterasi 1 (salah): sisi malam transparan penuh.** `drawMoonDisc()` gambar
seluruh piringan seakan 100% tersinari, lalu "menghapus" sisi malam ke
alpha=0 (`PorterDuffXfermode(PorterDuff.Mode.CLEAR)` + `BlurMaskFilter`)
supaya tembus ke background View apa pun warnanya. **Bug**: Bulan jadi
benar-benar tembus pandang — `StarfieldView` di baliknya (lihat baris
tabel di atas) kelihatan MENEMBUS piringan Bulan, padahal Bulan itu benda
padat yang seharusnya menghalangi apa pun di belakangnya (foto real: sisi
malam tidak reflektif, tapi tetap opak). User baru sadar bug ini setelah
efeknya jelas kelihatan di layar hilal `AwalBulanActivity` (background
putih polos di situ, bukan navy — bikin "tembus pandang"-nya sangat
mencolok, tidak seperti di kartu Fase Bulan yang background navy-nya
kebetulan mirip warna sisi malam sehingga bug-nya tersamar).

**Iterasi 2 (masih salah): tambah `nightBasePaint` (piringan navy solid)
digambar SEBELUM tekstur, dengan asumsi "erase" akan menyingkap warna itu
alih-alih background View.** Ternyata `PorterDuff.Mode.CLEAR` tidak peduli
"lapisan" gambar sebelumnya — dia menghapus TUNTAS ke transparan apa pun
yang sudah ada di buffer canvas saat itu, termasuk `nightBasePaint` yang
baru saja digambar di canvas yang sama. Hasilnya sisi malam tetap
transparan (bug iterasi 1 tidak benar-benar hilang, cuma di kartu Fase
Bulan kebetulan sulit dibedakan dari background-nya yang senada) —
dikonfirmasi dengan crop-zoom manual ke screenshot, ketemu bintang
`StarfieldView` yang jelas kelihatan tepat di dalam siluet Bulan.

**Iterasi 3 (final, benar):** kuncinya isolasi lewat `canvas.saveLayer()`.
`nightBasePaint` (piringan solid navy) digambar ke canvas UTAMA seperti
biasa, TAPI tekstur + operasi hapus (`nightErasePaint`) dibungkus di dalam
`saveLayer()`/`restoreToCount()` tersendiri. Karena CLEAR di dalam
`saveLayer` cuma menghapus isi LAYER itu (bukan buffer canvas utama di
baliknya), begitu layer di-restore, sisa tekstur (bentuk sabit/cembung)
dikomposit normal (SRC_OVER) di atas `nightBasePaint` yang sudah aman —
sisi malam akhirnya benar-benar opak (blok apa pun di belakangnya) sekaligus
terminatornya tetap lembut karena blur pada erase di dalam layer tidak
berubah. Alur lengkap di `drawMoonDisc()`:
1. `canvas.drawCircle(cx, cy, r, nightBasePaint)` ke canvas utama — SELALU,
   bahkan untuk bulan baru (k=0, `return` di titik ini, tanpa bagian terang).
2. `canvas.saveLayer(...)` — buka layer terpisah.
3. Di dalam layer: gambar seluruh piringan seakan 100% tersinari
   (`canvas.drawCircle` radius `r`, texture atau `flatBrightPaint`) — tanpa
   clip apa pun, limb luarnya otomatis tajam karena itu memang radius
   gambar lingkarannya.
4. Masih di dalam layer: "hapus" sisi malam pakai `nightErasePaint`
   (`PorterDuffXfermode(PorterDuff.Mode.CLEAR)` + `BlurMaskFilter` kalau
   [useSoftNightSide]) di atas shape dari `buildNightPath()`.
5. `canvas.restoreToCount(layer)` — komposit layer (dengan lubang
   transparan di sisi malam) ke canvas utama, menyingkap `nightBasePaint`.
6. View di-paksa `LAYER_TYPE_SOFTWARE` (lihat `init{}`) karena
   `BlurMaskFilter` tidak konsisten didukung di hardware-accelerated canvas
   di banyak versi Android.

Dua jebakan geometri tambahan yang sempat bikin hasil salah (dicoba &
dibuang sebelum solusi final di atas, murni soal bentuk shape-nya, terpisah
dari bug transparansi di atas):
- **Percobaan A**: men-scale seluruh shape penghapus 5% dari titik pusat
  (uniform) — masih menyisakan cincin tipis semi-terhapus tepat di limb,
  karena mem-blur+geser SELURUH shape (termasuk sisi terminator) tidak
  menjamin sisi limb-nya sendiri terdorong cukup jauh melewati lebar blur.
- **Percobaan B**: menambah `canvas.clipPath(lingkaran r)` sebelum semua
  gambar/hapus (dengan asumsi klasik "clip supaya limb tajam") — ternyata
  interaksi clip vs anti-alias/blur malah SELALU menyisakan cincin gelap
  tipis tepat di batas clip, walau shape penghapusnya sudah diperbesar
  berapa pun. **Clip akhirnya dilepas total** — ternyata tidak diperlukan
  sama sekali: `drawCircle(r)` sendiri memang berhenti presisi di radius
  `r`, jadi limb tetap tajam murni tanpa bantuan clip apa pun.
- Solusi geometri final: `buildNightPath()` (terpisah dari `buildLitPath`,
  bukan reuse) memakai radius `r + expand` (`expand = feather * 2.2`,
  `feather = r * TERMINATOR_FEATHER_RATIO = r * 0.10`, atau `0` kalau
  `useSoftNightSide` tidak aktif) khusus untuk arc/rentang vertikal yang
  berimpit dengan limb, sementara lebar terminator (`rx`) tetap dihitung
  dari `r` asli (tidak ikut membesar) supaya posisi/proporsi sabitnya akurat.

**`setSoftNightSide(enabled)`** (per 2026-09-05): toggle tambahan di
`MoonPhaseView`, pola sama seperti `setRealisticTexture`. Kalau `false` —
dipakai `AwalBulanActivity` untuk layar hilal — `feather`/`expand` di atas
jadi 0 dan `maskFilter` dilepas (`null`), sehingga `nightErasePaint`
menghapus dengan tepi TAJAM (bukan blur): hasilnya identik gaya lama
(piringan solid + sabit tepi tajam). Alasan: orang yang rukyah butuh bentuk
hilal yang jelas & tidak ambigu (hilal sudah setipis 0.1-0.3% tersinari),
bukan realisme foto — blur/soft edge di sini justru mengaburkan bentuk yang
krusial untuk diidentifikasi. Kartu Fase Bulan (home/detail) dan modal zoom
tetap pakai versi lembut (default `true`).

**Penting — dua requirement "sisi malam" ini scope-nya beda, jangan
dicampur** (klarifikasi user setelah iterasi opak di atas sempat dikira
"membatalkan" requirement natural-blend):
1. **Opak/blok apa pun di belakangnya** (`nightBasePaint` + `saveLayer`
   di atas) — berlaku di kartu home, layar detail, DAN modal zoom. Tidak
   relevan untuk `AwalBulanActivity` (hilal) karena di situ
   `setSoftNightSide(false)` sudah bikin piringan solid dari awal, bukan
   soal transparansi.
2. **Warna sisi malam menyatu (natural) dengan background lokasinya** —
   JUGA cuma berlaku di 3 tempat yang sama (home/detail/modal zoom), TIDAK
   berlaku di `AwalBulanActivity`. Modal zoom background-nya hitam solid
   (`#000000`) — diperbaiki lewat parameter baru
   `MoonPhaseView.renderToBitmap(sizePx, nightColorOverride)`:
   `FaseBulanActivity.showMoonZoomDialog()` panggil dengan
   `nightColorOverride = Color.BLACK` supaya sisi malam di render modal ini
   ikut hitam solid (opak dari requirement 1 + menyatu dari requirement 2
   sekaligus, karena warnanya sama persis dengan background). Override ini
   cuma untuk satu kali render (swap-restore `nightBasePaint.color` di
   dalam `renderToBitmap`), tidak mengubah warna default View.

   Kartu home & detail tadinya pakai `bg_card_gradient_navy` (gradient
   diagonal `navy_gradient_top` #1D2A45 → `login_bg_deep` #10192A), BUKAN
   warna flat. Percobaan pertama: asumsi `nightBasePaint` default
   `#10192A` (salah satu ujung gradient) "sudah cukup dekat" — user
   melaporkan masih kelihatan jelas beda (piringan navy vs background di
   sekitarnya), karena Bulan biasanya tidak persis di ujung gradient yang
   paling gelap. Percobaan kedua: aproksimasi manual - proyeksikan posisi
   Bulan ke fraksi `(fx + fy) / 2` di sepanjang diagonal card, lalu
   interpolasi RGB linear antara dua warna gradient - lebih baik tapi
   masih menyisakan garis siluet tipis (rumus Android untuk
   `android:angle` gradient tidak benar-benar diagonal sudut-ke-sudut
   sederhana, jadi aproksimasi manapun akan sedikit meleset). Percobaan
   ketiga (`utils/CardGradientColor.kt`, sekarang sudah dihapus): render
   `cardView.background` ke `Bitmap` seukuran card lewat `Canvas`, lalu
   baca piksel sungguhan (`Bitmap.getPixel()`) tepat di posisi tengah
   `MoonPhaseView` — akurat, tapi jadi tidak relevan lagi begitu user
   memutuskan (2026-09-06) kartu home & detail diganti ke background hitam
   solid (`@color/black`), bukan gradient — requirement "sisi malam
   menyatu dengan background lokal" TIDAK berubah, cuma nilai background
   lokalnya sekarang konstan hitam, jadi cukup
   `MoonPhaseView.setNightBaseColor(Color.BLACK)` langsung di
   `MainActivity.setupMoonPhaseCard()` & `FaseBulanActivity.onCreate()`,
   tanpa perlu sampling gradient/`doOnLayout` lagi. Modal zoom tidak
   berubah (sudah hitam solid dari awal, lihat baris `renderToBitmap` di
   atas).

Alur data (fase & info geosentris saat ini, tanpa lokasi):
`Time.fromMillisecondsSince1970(now)` → `moonPhase(time)` (sudut sinodik
0-360°, untuk `MoonPhaseView` + label), `illumination(Body.Moon,
time)` (`phaseFraction` untuk ilustrasi + teks persentase, `mag` untuk
magnitude), dan `geoVector(Body.Moon, time, Aberration.Corrected).length() *
KM_PER_AU` (jarak geosentris km) → langsung bind ke UI (sinkron, tidak perlu
coroutine — perhitungan single-point sangat cepat). Radius Bulan
(`moonMeanRadiusKm = 1737.4`) konstanta fisik (IAU mean radius), tidak
dihitung ulang.

Alur data (Detail Astronomis yang butuh lokasi, dijalankan di
`Dispatchers.Default` lalu bind di `Dispatchers.Main`): dapat `lat/lon` (GPS
atau lokasi manual dari `SessionManager`) → `Observer(lat, lon, 0.0)` →
`equator(Body.Moon, now, observer, EquatorEpoch.OfDate, Aberration.Corrected)`
(RA/Dec topocentric) → `horizon(now, observer, eq.ra, eq.dec,
Refraction.Normal)` (Az/Alt) → `searchRiseSet(Body.Moon, observer,
Direction.Rise/Set, now, 1.2)` (jam terbit/terbenam terdekat ke depan). RA
diformat jam sideris (`HHhMMmSS.Ss`), Dec/Az/Alt diformat derajat-menit-detik
(`DD°MM'SS.S"`) lewat helper lokal `formatRaHours`/`formatDegreesDms` di
`FaseBulanActivity`.

Per 2026-09-05 — kemiringan sungguhan ilustrasi: `showCurrentPhase()` (tanpa
lokasi) pakai `MoonPhaseView.setPhase()`, yang cuma pilih limb terang
kiri/kanan generik tanpa rotasi — ilustrasi awal karenanya selalu tampak
"lurus" (garis terminator vertikal), padahal kemiringan sungguhan di langit
tergantung posisi Bulan & Matahari relatif observer (dibanding, mis., app
astronomi lain yang render kemiringan asli). Begitu lokasi tersedia,
`onLocationReady()` menghitung ulang posisi Matahari (`equator`+`horizon`
untuk `Body.Sun`, pola sama seperti Bulan) lalu memanggil
`MoonTilt.brightLimbAngleDegrees()` (util yang sama dipakai ilustrasi hilal
di `AwalBulanActivity`) dan `MoonPhaseView.setPhaseWithTrueTilt()` — method
baru yang set fraksi tersinari dengan shape kanonik "bright-on-right" (sama
seperti `setWaxingCrescent`) lalu rotasi ke sudut sungguhan, karena begitu
rotasi dipakai, sisi kiri/kanan generik tidak lagi relevan (rotasi mencakup
kasus itu). Efeknya ilustrasi "loncat" dari lurus ke miring begitu lokasi
selesai di-resolve — belum ada state transisi/animasi untuk itu.

Rumus matematis di balik `MoonTilt.brightLimbAngleDegrees()` (turunan
vektor 3D-nya direduksi ke rumus tertutup 2 suku, setara rumus initial
bearing/parallactic angle trigonometri bola) didokumentasikan sebagai KDoc
di `utils/MoonTilt.kt` langsung — bukan di sini, supaya rumus tetap
bersebelahan dengan kode yang mengimplementasikannya.

Per 2026-09-05 (lanjutan) — kartu home ikut diperbaiki: `MainActivity` juga
sudah minta lokasi sejak awal (buat data sholat/kalender), jadi
`fetchDataByCoordinate(lat, lon)` (dipanggil dari `getUserLocation()`/
`useManualLocation()` begitu lokasi resolve) sekarang juga panggil
`updateMoonPhaseCardTilt(lat, lon)` — logika identik dengan
`FaseBulanActivity.onLocationReady()` (hitung Az/Alt Bulan & Matahari via
`equator`+`horizon`, lalu `MoonTilt.brightLimbAngleDegrees()` +
`setPhaseWithTrueTilt()`), tapi dijalankan sinkron di main thread (bukan
`Dispatchers.Default`) karena di sini tidak ada `searchRiseSet` — cuma dua
pasang `equator`+`horizon`, sama ringannya dengan perhitungan magnitude/jarak
yang juga sinkron. `setupMoonPhaseCard()` (dipanggil duluan di `onCreate`,
sebelum lokasi resolve) tetap pakai `setPhase()` biasa sebagai tampilan awal
sebelum lokasi siap — sama seperti perilaku `FaseBulanActivity`.

Bug terkait yang ikut diperbaiki di perubahan yang sama:
`MoonPhaseView.renderToBitmap()` (dipakai modal zoom, lihat baris
`ZoomableImageView.kt` di tabel atas) sebelumnya gambar langsung ke `Canvas`
baru tanpa lewat `View.draw()`,
jadi rotasi `View.rotation` (dari `setBrightLimbAngle`/`setPhaseWithTrueTilt`)
tidak ikut ke bitmap — hasil render selalu lurus walau tampilan aslinya sudah
miring. Sekarang `renderToBitmap()` menerapkan `canvas.rotate(rotation, ...)`
manual sebelum `drawMoonDisc()`.

Alur data (4 fase mendatang): `searchMoonQuarter(now)` → `nextMoonQuarter()`
dipanggil 3x berantai → 4 `MoonQuarterInfo` (quarter 0=baru, 1=kuartal
pertama, 2=purnama, 3=kuartal akhir, berurutan kronologis) → format tanggal
Indonesia → bind ke 4 `TextView` di kartu "Fase Mendatang".

**Catatan penting — dua "phase angle" yang berbeda di Astronomy Engine, jangan
tertukar:**
- `IlluminationInfo.phaseAngle` (dari `illumination()`) = sudut Matahari-Bumi
  dilihat dari Bulan, **0°=purnama, 180°=baru, rentang 0-180, simetris** — TIDAK
  bisa dipakai untuk membedakan waxing vs waning (nilainya sama persis di kedua
  sisi siklus).
- `moonPhase(time)` = sudut sinodik geosentris Bulan relatif Matahari,
  **0°=baru, 90°=kuartal pertama, 180°=purnama, 270°=kuartal akhir, rentang
  0-360** — inilah yang dipakai `MoonPhaseView`/`MoonPhaseLabel` untuk arah
  limb terang & nama fase.

Sempat salah pakai `IlluminationInfo.phaseAngle` di iterasi pertama (2026-09-05)
— hasilnya nama fase & sisi terang salah/tidak konsisten dengan persentase
tersinari. Sudah diperbaiki ke `moonPhase()`.

## 5. Dependencies & tech stack khusus

- Tidak ada tambahan library baru. `BitmapShader`/`Matrix` (tekstur foto
  bulan) dan `Path.arcTo` (bentuk sabit/cembung) adalah API
  `android.graphics` bawaan platform — bukan dependency eksternal.
- Satu aset raster baru: `res/drawable-nodpi/moon_texture.jpg` (~245KB, foto
  CC BY-SA 3.0 — wajib atribusi, lihat §4). Ini raster pertama di luar
  logo/mipmap-xxxhdpi; sebelumnya semua drawable di repo ini vector.
- Lokasi (§2) pakai `com.google.android.gms.location` (`FusedLocationProviderClient`),
  sudah jadi dependency existing lewat fitur Arah Kiblat — tidak ada
  penambahan library baru untuk itu juga.

## 6. Testing

Tidak ada test otomatis. Verifikasi manual dilakukan build debug APK →
install ke emulator `Pixel6_API34` (bukan device fisik, sesuai preferensi
testing project ini) → cek kartu home render ilustrasi (tekstur foto,
bukan flat color) + label + persentase konsisten, tap kartu → cek layar
detail + kartu "Detail Astronomis" terisi angka yang masuk akal (Az/Alt &
terbit/terbenam butuh izin lokasi granted) + jadwal 4 fase mendatang terisi
tanggal yang masuk akal, tap tombol back → kembali ke `MainActivity`. Sudah
diverifikasi 2026-09-05: ilustrasi sabit render tekstur kawah dengan benar
di kartu home (56dp) & layar detail (160dp), field Detail Astronomis terisi
(Magnitude -9.65, Jarak 369,094 km, dst.), tidak ada exception di logcat.

## 7. Known issues & TODOs

- [x] ~~Orientasi kiri/kanan limb terang pakai konvensi sederhana...~~ —
      Diperbaiki 2026-09-05, di kartu home (`MainActivity`) & layar detail
      (`FaseBulanActivity`): begitu lokasi observer tersedia, keduanya pakai
      `MoonTilt.brightLimbAngleDegrees()` + `MoonPhaseView.setPhaseWithTrueTilt()`
      untuk kemiringan sungguhan (lihat §4). Sisa keterbatasan: sebelum lokasi
      resolve (atau kalau izin ditolak), ilustrasi tetap fallback ke
      `setPhase()` generik (kiri/kanan tanpa rotasi) — bukan lagi masalah akurasi tapi
      transisi/state saat lokasi belum siap.
- [x] ~~`MoonTilt.brightLimbAngleDegrees()` mencerminkan kiri-kanan~~ — Per
      2026-09-07: `screenRight` di `MoonTilt.kt` (`cross(screenUp, moon)`)
      ternyata menghasilkan arah kiri sejati, bukan kanan (lolos dari
      validasi sebelumnya karena kombinasi rumus Meeus χ+q yang dipakai
      untuk cross-check saat itu ikut tercermin juga). Diperbaiki jadi
      `cross(moon, screenUp)`, dikonfirmasi lewat fakta kompas dasar
      (menghadap Utara → kanan = Timur) dan verifikasi visual di emulator
      (`FaseBulanActivity`, limb terang kanan-bawah cocok posisi Matahari
      Az lebih besar & Alt lebih rendah dari Bulan). `MoonTiltTest.kt`
      diperbarui mengikuti arah yang benar.
- [ ] Belum ada test otomatis untuk `MoonPhaseLabel`/logika `MoonPhaseView`.
- [ ] Tekstur `moon_texture.jpg` selalu piringan purnama tanpa libration —
      dipotong ke bentuk sabit/cembung yang benar, tapi corak kawah yang
      kelihatan di tepi limb tidak berubah sesuai libration sungguhan
      tanggal tsb (efek minor, tidak kasat mata pada ukuran tampil 56-160dp).
- [ ] Modal zoom (`ZoomableImageView`) menyisakan strip cream tipis di ujung
      atas (area status bar) yang belum berhasil dihilangkan — lihat catatan
      di baris `ZoomableImageView.kt`, tabel §4. Kosmetik, tidak mengganggu
      fungsi pinch-zoom/pan/double-tap.
