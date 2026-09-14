# Kalkulator Scientific

### 1. Ringkasan (Overview)
- **Nama fitur**: Kalkulator Scientific
- **Deskripsi singkat**: Kalkulator ilmiah umum (operator dasar, trigonometri,
  logaritma, akar, pangkat, konstanta π/e) di dalam app. Bukan bagian dari
  perhitungan ilmu falak (waktu sholat/hisab/dsb) — murni utilitas umum untuk
  pengguna yang butuh kalkulator cepat tanpa keluar app.

### 2. Entry point & prasyarat
- Dari layar Beranda (`MainActivity`), tombol menu **Kalkulator** — posisinya
  di grid menu antara **Okultasi** dan **Al-Qur'an**
  ([activity_main.xml](../app/src/main/res/layout/activity_main.xml), id
  `bt_kalkulator`).
- Tidak ada prasyarat permission/API key/config khusus — kalkulator murni
  logic lokal, tidak butuh lokasi/internet/API.

### 3. Titik masuk logika & navigasi
- `MainActivity.setupNavigation()` — `binding.btKalkulator.setOnClickListener`
  membuka `KalkulatorActivity` lewat `Intent` biasa (tanpa extra).
- [`KalkulatorActivity`](../app/src/main/java/site/elahady/alkaukaba/ui/kalkulator/KalkulatorActivity.kt)
  — satu-satunya layar fitur ini, tidak ada navigasi lanjutan ke Activity lain.
- [`ScientificCalculatorEngine`](../app/src/main/java/site/elahady/alkaukaba/utils/ScientificCalculatorEngine.kt)
  — object berisi parser & evaluator ekspresi; titik "colok" kalau mau
  menambah fungsi/operator baru (tambahkan di `FUNCTIONS`, tokenizer, dan
  `Parser.applyFunction`).

### 4. Struktur & alur data
- `activity_kalkulator.xml` — layout: toolbar default (`view_toolbar_default`,
  reuse), area display (`tvExpression` untuk ekspresi mentah, `tvResult` untuk
  hasil), grid tombol 4 kolom x 8 baris (nested `LinearLayout` + `weightSum`,
  konsisten dengan pola grid menu di `activity_main.xml` — bukan
  `androidx.gridlayout`).
- Style tombol (`CalcKeyNumber`, `CalcKeyOp`, `CalcKeyFunction`,
  `CalcKeyConst`, `CalcKeyAction`, `CalcKeyParen`, `CalcKeyMode`) ada di
  [`styles.xml`](../app/src/main/res/values/styles.xml), masing-masing pakai
  drawable shape `bg_calc_key_*.xml` + warna dari `colors.xml` yang di-reuse
  dari palet menu Beranda yang sudah ada (mis. `bg_teal_light`/`icon_teal`
  untuk tombol fungsi), kecuali `bg_indigo_light`/`icon_indigo` yang baru
  ditambah khusus untuk ikon menu Kalkulator di Beranda.
- Alur data: `KalkulatorActivity` (tap tombol) → append ke `StringBuilder`
  ekspresi lokal (in-memory, tidak persist) → `ScientificCalculatorEngine.evaluate()`
  (tokenize → recursive-descent parse) → `Double` → `formatResult()` (trim
  desimal berlebih, tampilkan sebagai integer kalau hasilnya bulat) →
  `tvResult`. Evaluasi dipanggil live setiap tap (preview), bukan hanya saat
  `=` — kalau ekspresi belum lengkap (mis. `"sin("` atau `"2+"`), `evaluate()`
  melempar `ExpressionError`/exception dan preview lama dibiarkan tampil
  (lihat `updateDisplay()`), bukan dikosongkan/nge-crash.

### 5. Dependencies & tech stack khusus
- Tidak ada dependency evaluator ekspresi eksternal (mis. exp4j/mXparser) di
  `build.gradle` project ini, jadi `ScientificCalculatorEngine` adalah parser
  recursive-descent tulisan sendiri (tokenizer + parser precedence standar:
  `+ -` < `* /` < unary `+/-` < `^` kanan-asosiatif < postfix `%`). Kalau
  suatu saat butuh fitur ekspresi yang jauh lebih kompleks (variabel,
  fungsi custom pengguna, dsb), pertimbangkan swap ke library matang
  daripada extend parser ini terus-menerus.
- Fungsi trigonometri (`sin/cos/tan/asin/acos/atan`) menghormati mode sudut
  (`DEGREE`/`RADIAN`) yang bisa di-toggle lewat tombol `DEG`/`RAD` di pojok
  kiri atas grid tombol — state ini cuma in-memory (`angleMode` di Activity),
  reset ke `DEGREE` tiap kali layar dibuka ulang.

### 6. Testing
- Unit test murni logic (tanpa Android) di
  [`ScientificCalculatorEngineTest`](../app/src/test/java/site/elahady/alkaukaba/utils/ScientificCalculatorEngineTest.kt)
  — precedence operator, pangkat kanan-asosiatif, trig di kedua angle mode,
  log/ln/sqrt/konstanta, ekspresi bersarang, dan error handling (bagi nol,
  akar negatif, parentheses tidak seimbang, token tidak dikenal). Semua pass
  (`./gradlew.bat testDebugUnitTest --tests "*ScientificCalculatorEngineTest"`).
- Tidak ada instrumentation/UI test otomatis untuk `KalkulatorActivity` itu
  sendiri (klik tombol, render grid) — gap yang sama dengan mayoritas layar
  lain di app ini. Verifikasi manual yang sudah dilakukan: build +
  `installDebug` ke emulator, buka Beranda → cek posisi menu Kalkulator
  (antara Okultasi & Al-Qur'an) lewat screenshot, buka `KalkulatorActivity` →
  cek grid tombol render lengkap, coba `sin(78)` → hasil `0.9781476007`
  (cocok dengan nilai sebenarnya sin 78°).

### 7. Known issues & TODOs
- Tombol `⌫` (backspace) menghapus satu karakter mentah, bukan satu token —
  menghapus `sin(` perlu 4x tap. Trade-off sengaja untuk simplicity, bukan
  bug.
- Belum ada implicit multiplication (mis. `"2π"` atau `"sin(78)sin(30)"` tanpa
  operator `×` di antaranya) — expression seperti itu gagal di-parse
  (`ExpressionError`, ditangkap dengan preview lama tetap tampil, tidak
  crash). Kalau mau ditambah, perlu logic tambahan di `Parser.parseMulDiv()`
  untuk mendeteksi juxtaposition.
- State kalkulator (ekspresi & hasil) tidak disimpan lewat rotasi layar
  (`onSaveInstanceState` belum diimplementasikan) — rotate akan reset ke
  kosong. Belum jadi prioritas karena app ini dikunci portrait di sebagian
  besar layar lain.
