# Al-Kaukaba Android

## Menjalankan & build via CLI (tanpa Android Studio)

Project ini dikerjakan sebagian dengan emulator yang dijalankan langsung dari VSCode, bukan dari Android Studio. Jangan sarankan langkah "buka/jalankan di Android Studio" — gunakan `gradlew` dari command line, dan jalankan/hubungkan emulator via `adb`.

Jika shell tidak punya `JAVA_HOME`/`ANDROID_HOME` (umum di Windows), set dulu sebelum memanggil `gradlew` — path berikut contoh di salah satu mesin dev, sesuaikan dengan instalasi JDK 17 & Android SDK di mesin masing-masing:

```
JAVA_HOME="C:/Program Files/Microsoft/jdk-17.0.20.101-hotspot" ANDROID_HOME="C:/Android/Sdk" ./gradlew.bat compileDebugKotlin --console=plain
```

## Setelah mengerjakan task (fix/fitur)

Setelah mengubah kode, jangan hanya compile check — build & install APK debug ke emulator yang sedang jalan supaya perubahan bisa langsung diverifikasi:

```
"$ANDROID_HOME/platform-tools/adb.exe" devices   # pastikan emulator terdeteksi
JAVA_HOME=... ANDROID_HOME=... ./gradlew.bat installDebug --console=plain
```
