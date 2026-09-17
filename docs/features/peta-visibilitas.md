# Peta Visibilitas Hilal (Dunia)

## 1. Ringkasan

**Fitur**: peta dunia (equirectangular, benua digambar sebagai poligon sangat
disederhanakan) dengan overlay warna per sel grid 15°x15° lintang-bujur,
menunjukkan wilayah mana yang hisabnya memenuhi kriteria Neo-MABIMS
(hijau) vs belum (merah) untuk bulan Hijriyah terdekat.

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
| `utils/WorldVisibilityCalculator.kt` | Generate grid 10 lintang (-60..75, step 15°) x 24 bujur (-180..165, step 15°) = 240 titik, panggil `EphemerisCalculator.calculate()` per titik (`heightMeters=0`), skip (try/catch) titik yang gagal dihitung (mis. kasus tepi ekstrem dekat kutub saat mentari/bulan tidak terbenam) |
| `model/WorldVisibilityModels.kt` | `VisibilityGridPoint` (lat, lng, `memenuhiKriteria`) dan `WorldVisibilityResult` (label bulan + ijtima' + list titik) |
| `ui/widget/WorldMapView.kt` | Custom `View`: gambar poligon benua (tangan-gambar, lihat section 4) + overlay warna per sel grid, proyeksi equirectangular sederhana (`x=(lng+180)/360*width`, `y=(90-lat)/180*height`) |

Alur data: `onCreate` -> `calculatePeta()` -> loop 240 titik ->
`EphemerisCalculator.calculate()` per titik -> `List<VisibilityGridPoint>`
-> LiveData -> Activity render kartu info + `WorldMapView.setData()`.

`bulanHijriyahLabel` dan `ijtimaTime` diambil dari titik grid pertama yang
berhasil dihitung (bukan dihitung terpisah) — aman karena keduanya
independen dari lokasi observer (ijtima' adalah peristiwa global, dan
`EphemerisCalculator.formatLocalTime` memformat pakai timezone device, jadi
sama persis nilainya di titik grid manapun).

## 4. Poligon benua (`WorldMapView.CONTINENTS`)

**Sangat disederhanakan** — dibuat manual dari perkiraan bentuk kasar tiap
benua (segelintir titik lat/lng per benua), **bukan** hasil digitasi data
GeoJSON/batas pantai sungguhan. Cukup untuk memberi konteks visual "kira-kira
di mana", tidak untuk keperluan yang butuh akurasi geografis. Kalau nanti
mau diperhalus, opsinya: cari/bundle data GeoJSON batas negara/benua yang
disederhanakan (perlu sumber data + kemungkinan parsing tambahan), bukan
sekadar nambah titik manual lagi.

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

## 6. Known limitations

- [ ] **Bentuk benua sangat kasar** (section 4) — jangan dipakai sebagai
      referensi geografis, cuma indikasi lokasi kasar.
- [ ] **Bukan reproduksi HilalMap** — grid 15°x15° (240 titik) jauh lebih
      renggang dari referensi (ribuan titik), dan kriteria Neo-MABIMS 2
      kategori (bukan Odeh 2006, 4 kategori: mudah terlihat mata telanjang
      / perlu alat optis / hanya dengan alat optis / tidak terlihat). Kalau
      nanti user minta kriteria Odeh 2006, itu implementasi klasifikasi
      baru dari nol (formula ARCV/lag time), bukan sekadar ubah tampilan.
- [ ] **Performa**: 240 pemanggilan `EphemerisCalculator.calculate()`
      sekuensial, tiap panggilan independen menghitung ulang ijtima' dari
      nol (`findIjtimaAtOffset` di `EphemerisCalculator`) walau hasilnya
      identik di semua titik (ijtima' adalah peristiwa global, tidak
      tergantung observer) — ini redundansi murni, bukan cuma di fitur ini
      (Hisab Nasional juga punya redundansi yang sama, hanya lebih ringan
      karena jumlah titiknya jauh lebih sedikit). Optimasi yang mungkin:
      hitung ijtima' sekali di awal, lalu tambahkan overload
      `EphemerisCalculator.calculate()` yang menerima `ijtima: Time` siap
      pakai — belum dilakukan supaya tidak mengubah perilaku fungsi yang
      sudah dipakai & divalidasi di fitur "Awal Bulan" (production,
      sensitif ke perubahan).
- [ ] **Warna overlay ter-alpha-blend dengan latar** (section 5) — di peta
      terlihat toska/mauve, bukan hijau/merah murni seperti di legenda.
      Kalau dirasa membingungkan, bisa dinaikkan opacity-nya
      (`WorldMapView.memenuhiPaint`/`belumPaint`) atau ganti pendekatan
      (mis. gambar overlay di layer terpisah tanpa blending dengan warna
      benua).
- [ ] **Grid resolusi tetap hardcode** (`WorldVisibilityCalculator.
      LATITUDES`/`LONGITUDES`, 15° step) — gampang diubah kalau mau
      lebih rapat/renggang, tapi ingat trade-off performa (section 5) dan
      harus disinkronkan dengan `WorldMapView.CELL_HALF_LAT`/
      `CELL_HALF_LNG`.
