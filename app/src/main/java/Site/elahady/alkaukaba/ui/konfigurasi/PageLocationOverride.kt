package site.elahady.alkaukaba.ui.konfigurasi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import site.elahady.alkaukaba.R

/**
 * Override lokasi khusus satu halaman (Gerhana, Awal Bulan, Okultasi): sheet "Lokasi untuk Halaman
 * Ini" dengan pilihan "Ikuti pengaturan global" atau "Manual khusus halaman ini" (isi lat/lon,
 * GPS, atau pin di peta). Pilihan manual disimpan di SharedPreferences [prefsName] milik halaman
 * itu sendiri - SEPARATE dari [site.elahady.alkaukaba.utils.SessionManager] - sehingga tidak
 * pernah mengubah pengaturan lokasi global yang dipakai fitur lain.
 *
 * Harus dibuat sebagai property/di onCreate Activity (mendaftarkan ActivityResult launcher,
 * yang tidak boleh dilakukan setelah Activity STARTED).
 *
 * @param fallbackLocation titik awal peta kalau field kosong/tidak valid: lokasi yang sedang
 * dipakai halaman (bukan titik acak) supaya pin mulai dari konteks yang user kenal.
 * @param onSaved dipanggil setelah Simpan (override dipasang/dilepas) - halaman menghitung ulang.
 */
class PageLocationOverride(
    private val activity: AppCompatActivity,
    prefsName: String,
    private val fallbackLocation: () -> Pair<Double, Double>,
    private val onSaved: () -> Unit
) {

    // lazy: kelas ini dibuat sebagai property initializer Activity (di constructor, sebelum
    // attachBaseContext), jadi apa pun yang butuh Context - getSharedPreferences, klien lokasi -
    // TIDAK boleh disentuh di sini atau langsung NPE. registerForActivityResult di bawah aman
    // karena tidak butuh Context.
    private val prefs by lazy { activity.getSharedPreferences(prefsName, Context.MODE_PRIVATE) }
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(activity) }

    // Field aktif selagi sheet terbuka, dipakai callback GPS/permission/peta.
    private var etLatRef: EditText? = null
    private var etLngRef: EditText? = null

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchGpsIntoFields()
        } else {
            Toast.makeText(activity, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickFromMapLauncher = activity.registerForActivityResult(PilihLokasiPetaContract()) { picked ->
        picked?.let { (lat, lng) ->
            etLatRef?.setText(lat.toString())
            etLngRef?.setText(lng.toString())
        }
    }

    fun hasOverride(): Boolean = prefs.contains(KEY_LAT) && prefs.contains(KEY_LNG)

    fun lat(): Double = prefs.getFloat(KEY_LAT, 0f).toDouble()

    fun lng(): Double = prefs.getFloat(KEY_LNG, 0f).toDouble()

    fun showSheet() {
        val bottomSheetDialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(R.layout.dialog_lokasi_halaman, null)
        bottomSheetDialog.setContentView(view)

        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupPageLocationMode)
        val radioGlobal = view.findViewById<RadioButton>(R.id.radioPageLocationGlobal)
        val radioManual = view.findViewById<RadioButton>(R.id.radioPageLocationManual)
        val layoutManual = view.findViewById<View>(R.id.layoutPageManualLocation)
        val etLat = view.findViewById<EditText>(R.id.etPageManualLat)
        val etLng = view.findViewById<EditText>(R.id.etPageManualLng)
        val btnUseGps = view.findViewById<AppCompatButton>(R.id.btnPageUseCurrentGps)
        val btnPickFromMap = view.findViewById<AppCompatButton>(R.id.btnPagePickFromMap)
        val btnSave = view.findViewById<AppCompatButton>(R.id.btnSavePageLocation)

        val isOverride = hasOverride()
        radioManual.isChecked = isOverride
        radioGlobal.isChecked = !isOverride
        layoutManual.visibility = if (isOverride) View.VISIBLE else View.GONE
        if (isOverride) {
            etLat.setText(prefs.getFloat(KEY_LAT, 0f).toString())
            etLng.setText(prefs.getFloat(KEY_LNG, 0f).toString())
        }

        etLatRef = etLat
        etLngRef = etLng

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            layoutManual.visibility = if (checkedId == R.id.radioPageLocationManual) View.VISIBLE else View.GONE
        }

        btnUseGps.setOnClickListener { fetchGpsIntoFields() }

        // Titik awal peta: angka valid di field, kalau tidak lokasi yang sedang dipakai halaman.
        btnPickFromMap.setOnClickListener {
            val lat = etLat.text.toString().toDoubleOrNull()?.takeIf { it in -90.0..90.0 }
            val lng = etLng.text.toString().toDoubleOrNull()?.takeIf { it in -180.0..180.0 }
            pickFromMapLauncher.launch(if (lat != null && lng != null) lat to lng else fallbackLocation())
        }

        btnSave.setOnClickListener {
            if (radioGroup.checkedRadioButtonId == R.id.radioPageLocationManual) {
                val lat = etLat.text.toString().toDoubleOrNull()
                val lng = etLng.text.toString().toDoubleOrNull()
                if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
                    Toast.makeText(activity, "Isi lintang/bujur dengan angka yang valid", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                prefs.edit()
                    .putFloat(KEY_LAT, lat.toFloat())
                    .putFloat(KEY_LNG, lng.toFloat())
                    .apply()
            } else {
                prefs.edit().remove(KEY_LAT).remove(KEY_LNG).apply()
            }
            Toast.makeText(activity, "Lokasi disimpan", Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss()
            onSaved()
        }

        bottomSheetDialog.setOnDismissListener {
            etLatRef = null
            etLngRef = null
        }

        bottomSheetDialog.show()
    }

    @SuppressLint("MissingPermission")
    private fun fetchGpsIntoFields() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                etLatRef?.setText(location.latitude.toString())
                etLngRef?.setText(location.longitude.toString())
            } else {
                Toast.makeText(activity, "Lokasi GPS tidak ditemukan, coba lagi", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(activity, "Gagal mengambil lokasi GPS", Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        // Key sama dengan implementasi awal di GerhanaActivity - jangan diubah, override yang
        // sudah tersimpan di device pengguna akan hilang.
        const val KEY_LAT = "PAGE_MANUAL_LAT"
        const val KEY_LNG = "PAGE_MANUAL_LNG"
    }
}
