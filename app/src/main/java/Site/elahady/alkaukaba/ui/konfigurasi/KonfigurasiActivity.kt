package site.elahady.alkaukaba.ui.konfigurasi

import site.elahady.alkaukaba.LoginActivity
import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.databinding.ActivityKonfigurasiBinding
import site.elahady.alkaukaba.utils.PrayerCalculationMethods
import site.elahady.alkaukaba.utils.SessionManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.WindowCompat
import com.google.android.material.bottomsheet.BottomSheetDialog

class KonfigurasiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKonfigurasiBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityKonfigurasiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        sessionManager = SessionManager(this)

        binding.btnBack.setOnClickListener { finish() }
        binding.rowPrayerMethod.setOnClickListener { showPrayerMethodSheet() }
        binding.btnLogout.setOnClickListener { showLogoutConfirmation() }

        updateCurrentMethodLabel()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
            .setPositiveButton("Ya") { dialog, _ ->
                dialog.dismiss()
                performLogout()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun performLogout() {
        sessionManager.setLogin(false)

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun updateCurrentMethodLabel() {
        val method = PrayerCalculationMethods.findById(sessionManager.getPrayerMethodId())
        binding.tvCurrentMethod.text = method.displayName
    }

    private fun showPrayerMethodSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_prayer_method, null)
        bottomSheetDialog.setContentView(view)
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupMethods)
        val layoutCustomAngle = view.findViewById<View>(R.id.layoutCustomAngle)
        val etCustomFajr = view.findViewById<EditText>(R.id.etCustomFajr)
        val etCustomIsha = view.findViewById<EditText>(R.id.etCustomIsha)
        val btnSave = view.findViewById<AppCompatButton>(R.id.btnSavePrayerMethod)

        val currentMethodId = sessionManager.getPrayerMethodId()

        PrayerCalculationMethods.PRESETS.forEach { method ->
            val radioButton = RadioButton(this).apply {
                id = method.id
                text = "${method.displayName}\n${method.subtitle}"
                textSize = 14f
                setPadding(0, 16, 0, 16)
                isChecked = method.id == currentMethodId
            }
            radioGroup.addView(radioButton)
        }

        etCustomFajr.setText(sessionManager.getCustomFajrAngle().toString())
        etCustomIsha.setText(sessionManager.getCustomIshaAngle().toString())
        layoutCustomAngle.visibility = if (currentMethodId == PrayerCalculationMethods.CUSTOM_ID) View.VISIBLE else View.GONE

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            layoutCustomAngle.visibility =
                if (checkedId == PrayerCalculationMethods.CUSTOM_ID) View.VISIBLE else View.GONE
        }

        btnSave.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            if (selectedId == -1) {
                Toast.makeText(this, "Pilih salah satu metode terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedId == PrayerCalculationMethods.CUSTOM_ID) {
                val fajrAngle = etCustomFajr.text.toString().toDoubleOrNull()
                val ishaAngle = etCustomIsha.text.toString().toDoubleOrNull()
                if (fajrAngle == null || ishaAngle == null) {
                    Toast.makeText(this, "Isi sudut Fajr & Isya dengan angka yang valid", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                sessionManager.setCustomPrayerAngles(fajrAngle, ishaAngle)
            }

            sessionManager.setPrayerMethodId(selectedId)
            updateCurrentMethodLabel()
            Toast.makeText(this, "Metode perhitungan disimpan", Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }
}
