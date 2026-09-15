package site.elahady.alkaukaba.ui.tasbih

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.databinding.ActivityTasbihBinding
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

/**
 * Tasbih digital - penghitung dzikir sederhana. Ketuk lingkaran untuk menambah hitungan,
 * target bisa diganti (33 / 99 / 100 / tanpa target) dan "putaran" dihitung otomatis begitu
 * hitungan mencapai kelipatan target. Hitungan & target disimpan di SharedPreferences supaya
 * tidak hilang saat user keluar-masuk halaman ini (dipakai berulang kali dalam satu sesi dzikir).
 */
class TasbihActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTasbihBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var vibrator: Vibrator

    private var count = 0
    private var targetIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTasbihBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        applyStatusBarIconsForTheme()
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        vibrator = getVibrator()

        count = prefs.getInt(KEY_COUNT, 0)
        targetIndex = prefs.getInt(KEY_TARGET_INDEX, 0)

        setupToolbar()
        setupInteractions()
        renderCount()
        renderTarget()
    }

    private fun setupToolbar() {
        binding.includeToolbar.tvToolbarTitle.text = "Tasbih Digital"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.includeToolbar.btnToolbarAction.apply {
            setImageResource(R.drawable.ic_reset)
            setColorFilter(getColor(R.color.text_secondary))
            visibility = android.view.View.VISIBLE
            contentDescription = "Reset hitungan"
            setOnClickListener { confirmReset() }
        }
    }

    private fun setupInteractions() {
        binding.circleTapArea.setOnClickListener {
            count++
            saveCount()
            renderCount()
            val target = TARGETS[targetIndex]
            if (target > 0 && count % target == 0) {
                vibrate(longArrayOf(0, 60, 80, 60))
            } else {
                vibrate(longArrayOf(0, 25))
            }
        }
        binding.btnUndo.setOnClickListener {
            if (count > 0) {
                count--
                saveCount()
                renderCount()
            }
        }
        binding.tvTarget.setOnClickListener {
            targetIndex = (targetIndex + 1) % TARGETS.size
            prefs.edit().putInt(KEY_TARGET_INDEX, targetIndex).apply()
            renderTarget()
        }
    }

    private fun renderCount() {
        binding.tvCount.text = count.toString()
        val target = TARGETS[targetIndex]
        binding.tvRounds.text = if (target > 0) {
            "Putaran ke-${count / target}"
        } else {
            "Hitungan Bebas"
        }
    }

    private fun renderTarget() {
        val target = TARGETS[targetIndex]
        binding.tvTarget.text = if (target > 0) "Target: ${target}x" else "Tanpa Target"
        renderCount()
    }

    private fun saveCount() {
        prefs.edit().putInt(KEY_COUNT, count).apply()
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("Reset Hitungan?")
            .setMessage("Hitungan tasbih akan kembali ke 0. Lanjutkan?")
            .setPositiveButton("Reset") { dialog, _ ->
                dialog.dismiss()
                count = 0
                saveCount()
                renderCount()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun vibrate(pattern: LongArray) {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    companion object {
        private const val PREFS_NAME = "TasbihPrefs"
        private const val KEY_COUNT = "COUNT"
        private const val KEY_TARGET_INDEX = "TARGET_INDEX"

        // 0 = tanpa target (hitungan bebas)
        private val TARGETS = intArrayOf(33, 99, 100, 0)
    }
}
