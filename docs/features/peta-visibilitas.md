# Peta Visibilitas Hilal (Dunia)

## 1. Ringkasan

**Fitur**: peta dunia (equirectangular, benua digambar sebagai poligon sangat
disederhanakan) dengan overlay warna per sel grid 5°x5° lintang-bujur
(dinaikkan dari 15°x15° semula, lihat section 6a), menunjukkan wilayah mana
yang hisabnya memenuhi kriteria Neo-MABIMS (hijau) vs belum (merah) untuk
bulan Hijriyah terdekat.

Dibuat 2026-09-16 setelah user share referensi visual "HilalMap"
(al-habib.info, kriteria Odeh 2006, grid rapat + batas negara akurat) dan
minta didiskusikan penempatan menunya. Setelah diskusi (lihat riwayat
percakapan), diputuskan:

- **Menu terpisah** dari "Hisab Awal Bulan Nasional" (bukan digabung) —
  scope beda (dunia vs Indonesia), kriteria beda, cara render beda total
  (peta vs daftar kartu).
- **Grid kasar tanpa peta geografis detail** (bukan reproduksi HilalMap) —
  menghitung tiap sel pakai `EphemerisCalculator.calculate()` penuh (sama
  dengan Hisab Nasional), jadi grid rapat ala referensi (ribuan titik) akan
  terlalu lambat tanpa optimasi mesin hisab lebih lanjut.
- **Kriteria Neo-MABIMS** (2 kategori: memenuhi/belum, reuse
  `HilalResult.hilalMemenuhiKriteria`), BUKAN Odeh 2006 (4 kategori) yang
  dipakai peta referensi — supaya reuse mesin hisab yang sudah divalidasi,
  bukan implementasi kriteria klasifikasi baru dari nol.

## 2. Entry point & navigasi

- Layar: `PetaVisibilitasActivity` (layout `activity_peta_visibilitas.xml`,
  judul UI "Peta Visibilitas Hilal").
- **Navigasi bulan** (ditambahkan 2026-09-17): dua `Spinner`
  (`spinnerBulanHijriyah`, `spinnerTahunHijriyah`) di atas kartu info,
  default = bulan Hijriyah terdekat ke depan (offset 0). Pola implementasi
  identik dengan selector di `AwalBulanActivity` — baseline dari
  `HijriDateUtil.nextMonthYearMonth()`, `currentMonthOffset` dihitung dari
  selisih (tahun*12+bulan) pilihan spinner terhadap baseline, lalu
  `viewModel.calculatePeta(currentMonthOffset)` dipanggil ulang tiap ganti
  pilihan.
- Terdaftar di `AndroidManifest.xml` (`exported="false"`).
- Menu: ditaruh **tepat setelah "Hisab Awal Bulan Nasional"** di kedua
  tempat (permintaan eksplisit user):
  - `SemuaMenuActivity` — warna baru `bg_lime_light`/`icon_lime`
    (ditambahkan khusus fitur ini, semua pasangan warna kartu menu yang ada
    sudah terpakai tepat sekali masing-masing).
  - Home quick-access (`activity_main.xml`/`MainActivity.kt`,
    `bt_peta_visibilitas`) — slotnya didapat dengan **mengeluarkan
    "Al-Qur'an" dari home** (tetap ada di Semua Menu) atas permintaan user,
    supaya grid home tetap 2 baris x 4 kolom.
- Icon custom `ic_menu_peta_visibilitas.xml` — wireframe globe (lingkaran +
  garis lintang/bujur), belum ada icon "peta/globe" lain di project.
- Tidak butuh permission lokasi — semua titik grid fixed by-design (bukan
  lokasi user).

## 3. Struktur & alur data

| File | Peran |
|---|---|
| `ui/petavisibilitas/PetaVisibilitasActivity.kt` + `activity_peta_visibilitas.xml` | UI: kartu info (bulan Hijriyah + ijtima') + `WorldMapView` + legenda + disclaimer |
| `viewmodel/petavisibilitas/PetaVisibilitasViewModel.kt` | `LiveData<WorldVisibilityResult>`, jembatan ke `WorldVisibilityCalculator` |
| `utils/WorldVisibilityCalculator.kt` | Generate grid 28 lintang (-60..75, step 5°) x 72 bujur (-180..175, step 5°) = 2016 titik, hitung ijtima' sekali (`EphemerisCalculator.findIjtima()`) lalu panggil `EphemerisCalculator.calculate(input, ijtima)` per titik (`heightMeters=0`), skip (try/catch) titik yang gagal dihitung (mis. kasus tepi ekstrem dekat kutub saat mentari/bulan tidak terbenam) |
| `model/WorldVisibilityModels.kt` | `VisibilityGridPoint` (lat, lng, `memenuhiKriteria`) dan `WorldVisibilityResult` (label bulan + ijtima' + list titik) |
| `ui/widget/WorldMapView.kt` | Custom `View`: gambar poligon benua (tangan-gambar, lihat section 4) + overlay warna per sel grid, proyeksi equirectangular sederhana (`x=(lng+180)/360*width`, `y=(90-lat)/180*height`) |

Alur data: `onCreate` -> `calculatePeta()` -> hitung ijtima' sekali -> loop
2016 titik -> `EphemerisCalculator.calculate(input, ijtima)` per titik ->
`List<VisibilityGridPoint>` -> LiveData -> Activity render kartu info +
`WorldMapView.setData()`.

`bulanHijriyahLabel` dan `ijtimaTime` diambil dari titik grid pertama yang
berhasil dihitung (bukan dihitung terpisah) — aman karena keduanya
independen dari lokasi observer (ijtima' adalah peristiwa global, dan
`EphemerisCalculator.formatLocalTime` memformat pakai timezone device, jadi
sama persis nilainya di titik grid manapun).

## 4. Garis pantai dunia (`WorldMapView.loadLandPolygons()`)

**Per 2026-09-17**: diganti dari poligon tangan-gambar (v1) ke data garis
pantai sungguhan — Natural Earth "ne_110m_land" (domain publik, resolusi
110m, https://github.com/nvkelso/natural-earth-vector), diunduh lalu
diminifikasi (script sekali-pakai, tidak disimpan di repo):

- Strip semua field selain `geometry.coordinates` (`properties`, `bbox`,
  `crs`, `name` dibuang).
- Koordinat dibulatkan 2 desimal (~1km, lebih dari cukup untuk peta seukuran
  layar HP) + dedup titik berurutan yang jadi sama setelah dibulatkan.
- Hasil: array polygon → array ring (ring pertama = outer, sisanya = lubang,
  cuma 1 dari 127 polygon yang punya lubang) → array flat `[lng,lat,lng,lat,...]`.
- Ukuran turun dari 138KB (GeoJSON asli) jadi 66KB
  (`app/src/main/assets/world_land_110m.json`), 127 polygon, ~5100 titik
  total.

Di-parse sekali (lazy, `org.json.JSONArray`) di `WorldMapView`, digambar
sebagai satu `Path` per polygon dengan `FillType.EVEN_ODD` (subpath per ring
— otomatis benar untuk lubang, tidak perlu logic winding-order manual).
Satu-satunya polygon yang datanya melintasi bujur ±180 (Antartika, garis
lintang -90 di kedua ujung) aman digambar apa adanya karena titik
sambungannya sama-sama di baris piksel paling bawah peta — tidak ada
polygon lain yang melintasi meridian ±180 di data 110m ini (sudah dicek
manual sebelum implementasi).

**Sebelum 2026-09-17 (v1, sudah diganti)**: poligon dibuat manual dari
perkiraan bentuk kasar tiap benua (segelintir titik lat/lng per benua),
bukan hasil digitasi data sungguhan — makanya bentuknya terlihat kacau
(feedback user setelah lihat hasil B.1/B.2 tanggal yang sama).

## 5. Testing

Belum ada test otomatis (murni komposisi berulang dari
`EphemerisCalculator` yang sudah ada + custom `View` visual, tidak ada
rumus baru yang perlu divalidasi terpisah). Verifikasi manual (emulator,
2026-09-16, screenshot langsung karena ini fitur visual — beda dari fitur
list-based lain yang cukup `uiautomator dump`):

- Home -> "Peta Visibilitas" & Semua Menu -> "Peta Visibilitas Hilal" ->
  `PetaVisibilitasActivity` terbuka tanpa crash.
- Kartu info terisi benar: "Menjelang Jumadil Awal 1448 H", "Ijtima': 10
  Oktober 2026, 22:50:36".
- Peta tergambar: 6 blob benua (Amerika Utara/Selatan, Eropa, Afrika, Asia,
  Australia) kebaca posisinya kira-kira benar, overlay grid hijau/merah
  tampil ter-blend dengan warna benua/laut di bawahnya (bukan warna solid
  seperti di legenda — legenda pakai warna solid tanpa alpha supaya gampang
  dibaca, sementara di peta di-alpha-blend supaya bentuk benua di
  bawahnya tetap kelihatan; efeknya warna "hijau" di peta terlihat lebih
  ke arah teal/toska dan "merah" ke arah mauve/pink, bukan hijau/merah
  murni — lihat known limitation).
- Ada 1 sel kosong (celah putih kecil) di dekat kutub utara — titik yang
  gagal dihitung (`searchRiseSet` return null, kemungkinan matahari tidak
  terbenam di lokasi & tanggal itu) dan di-skip sesuai desain
  `WorldVisibilityCalculator`, bukan crash.
- Waktu hitung total (240 titik, sekuensial) terasa beberapa detik (dengan
  `ProgressBar` selama proses) — jauh lebih lama dari Hisab Nasional (maks
  38 titik) tapi masih dalam batas wajar untuk aksi "generate peta" yang
  jarang dipicu ulang.

**Per 2026-09-17** (setelah optimasi ijtima' + naik resolusi ke 5°, lihat
section 6a): benchmark JVM (unit test sementara, dihapus lagi setelah
dipakai, bukan bagian dari test suite permanen) memanggil
`EphemerisCalculator.calculate(input, ijtima)` sebanyak grid 5° (1960-2016
titik) hanya makan ~0.4 detik total, dan 5336 titik (setara step 3°) ~0.7
detik — jauh di bawah versi lama (240 titik "beberapa detik") karena
pencarian rantai ijtima' (bagian termahal) sekarang cuma sekali per
`calculatePeta()`, bukan per titik. Instalasi ke emulator & buka layar
sukses tanpa crash; pengukuran presisi waktu-nyata di device tidak
diverifikasi otomatis (uiautomator tap gagal konsisten navigasi kedua kali,
dihentikan sesuai aturan anti-spam-screenshot) — user diminta cek manual.

## 6. Known limitations

- [x] ~~Bentuk benua sangat kasar~~ — **selesai 2026-09-17**, lihat section 4.
      Bentuk sekarang dari data garis pantai sungguhan (Natural Earth 110m),
      bukan lagi poligon tangan-gambar.
- [ ] **Bukan reproduksi HilalMap** — grid 5°x5° (2016 titik, dinaikkan dari
      15° per 2026-09-17, lihat 6a) masih lebih renggang dari referensi
      (ribuan titik), dan kriteria Neo-MABIMS 2 kategori (bukan Odeh 2006,
      4 kategori: mudah terlihat mata telanjang / perlu alat optis / hanya
      dengan alat optis / tidak terlihat). Kalau nanti user minta kriteria
      Odeh 2006, itu implementasi klasifikasi baru dari nol (formula
      ARCV/lag time), bukan sekadar ubah tampilan.
- [ ] **Warna overlay ter-alpha-blend dengan latar** (section 5) — di peta
      terlihat toska/mauve, bukan hijau/merah murni seperti di legenda.
      Kalau dirasa membingungkan, bisa dinaikkan opacity-nya
      (`WorldMapView.memenuhiPaint`/`belumPaint`) atau ganti pendekatan
      (mis. gambar overlay di layer terpisah tanpa blending dengan warna
      benua).
- [ ] **Grid resolusi tetap hardcode** (`WorldVisibilityCalculator.
      LATITUDES`/`LONGITUDES`, 5° step per 2026-09-17) — gampang diubah
      kalau mau lebih rapat/renggang (benchmark JVM: 5336 titik/step 3°
      masih ~0.7 detik, lihat 6a), tapi harus disinkronkan dengan
      `WorldMapView.CELL_HALF_LAT`/`CELL_HALF_LNG`, dan garis grid akan
      selalu terlihat blocky (bukan gradasi halus) tanpa interpolasi antar
      titik (belum diimplementasikan, lihat 6a).

### 6a. Riwayat optimasi performa (2026-09-17)

Rencana perbaikan yang disepakati 2026-09-16 (poin B di riwayat percakapan)
sudah dikerjakan sebagian:

1. **Optimasi cache ijtima'** (selesai) — `EphemerisCalculator.calculate()`
   lama menghitung ulang rantai new-moon (`findIjtimaAtOffset`, bagian
   termahal) di tiap dari 240 panggilan, padahal ijtima' adalah peristiwa
   global yang identik di semua titik grid. Sekarang dipisah: fungsi publik
   baru `EphemerisCalculator.findIjtima(monthOffset)` menghitungnya sekali,
   dan overload baru `EphemerisCalculator.calculate(input, ijtima)`
   menerima hasil itu langsung (skip pencarian ulang). Fungsi lama
   `calculate(input)` (dipakai fitur "Awal Bulan"/Hisab Nasional, production
   & sensitif) TIDAK diubah perilakunya — cuma jadi wrapper tipis yang
   memanggil `findIjtima()` lalu overload baru, diverifikasi lewat
   `EphemerisCalculatorTest` yang sudah ada (masih pass, tanpa perubahan
   assertion).
2. **Naikkan resolusi grid 15° → 5°** (selesai) — `WorldVisibilityCalculator.
   LATITUDES`/`LONGITUDES` dan `WorldMapView.CELL_HALF_LAT`/`CELL_HALF_LNG`
   disinkronkan ke 5° (2016 titik, naik dari 240). Batas bujur atas juga
   dikoreksi dari 165° ke 175° (pola `180° - step`) supaya tidak ada celah
   antara kolom terakhir dan meridian ±180° saat step diperkecil.
   Kelayakannya dicek lewat benchmark JVM sementara (unit test yang dihapus
   lagi setelah dipakai): grid 5° (1960-2016 titik) ~0.4 detik, grid step 3°
   (5336 titik) ~0.7 detik — jauh di bawah versi lama (240 titik tanpa
   optimasi #1, "beberapa detik").
3. **Interpolasi antar titik grid** (opsional, belum dikerjakan) — supaya
   transisi warna terlihat gradasi halus, bukan kotak-kotak tegas. Belum
   ada rencana teknis konkret.
4. **Ganti poligon benua ke data GeoJSON asli** (selesai, sama hari) — lihat
   section 4 untuk detail sumber data & proses minifikasi.

Verifikasi: `EphemerisCalculatorTest` full suite tetap pass; full unit test
suite project (`testDebugUnitTest`) punya 4 failure pre-existing & tidak
terkait (`OccultationCalculatorTest` — golden test waktu-nyata Venus-Bulan,
`KiblatViewModelTest` x3 — `NotImplementedError`), dikonfirmasi lewat
`git stash` (gagal sama persis di kode sebelum perubahan ini). Build &
install debug APK ke emulator sukses tanpa error tiap iterasi (termasuk
setelah ganti poligon benua — logcat dicek, tidak ada crash/FATAL). Asset
`world_land_110m.json` dikonfirmasi ikut ter-bundle di APK
(`unzip -l app-debug.apk`). Verifikasi visual langsung di device **tidak
berhasil diotomasi** — `uiautomator`/`input tap` gagal konsisten
menavigasi ke layar ini pada beberapa percobaan (state Activity di
belakang tidak seperti yang diharapkan dari dump sebelumnya), dihentikan
sesuai aturan anti-spam-screenshot di CLAUDE.md — **user perlu cek manual**
apakah bentuk benua & grid 5° sudah terlihat benar di layar.
