package site.elahady.alkaukaba.ui.kalkulator

import site.elahady.alkaukaba.databinding.ActivityKalkulatorBinding
import site.elahady.alkaukaba.utils.ScientificCalculatorEngine
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import java.util.Locale
import kotlin.math.abs

/**
 * Kalkulator scientific sederhana - bukan bagian dari perhitungan ilmu falak
 * app (waktu sholat/hisab dsb), murni utilitas umum untuk pengguna. Parsing &
 * evaluasi ekspresi dilakukan oleh [ScientificCalculatorEngine] (recursive-
 * descent tangan-sendiri, tidak ada dependency evaluator ekspresi eksternal
 * di project ini).
 */
class KalkulatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKalkulatorBinding
    private val expression = StringBuilder()
    private var angleMode = ScientificCalculatorEngine.AngleMode.DEGREE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKalkulatorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        binding.includeToolbar.tvToolbarTitle.text = "Kalkulator"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupKeys()
        updateDisplay()
    }

    private fun setupKeys() {
        val literalKeys = listOf(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2",
            binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5",
            binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8",
            binding.btn9 to "9", binding.btnDot to ".",
            binding.btnPlus to "+", binding.btnMinus to "−",
            binding.btnMultiply to "×", binding.btnDivide to "÷",
            binding.btnPower to "^", binding.btnPercent to "%",
            binding.btnOpenParen to "(", binding.btnCloseParen to ")",
            binding.btnPi to "π", binding.btnEuler to "e"
        )
        literalKeys.forEach { (view, text) ->
            view.setOnClickListener { appendToExpression(text) }
        }

        val functionKeys = listOf(
            binding.btnSin to "sin(", binding.btnCos to "cos(",
            binding.btnTan to "tan(", binding.btnLn to "ln(",
            binding.btnLog to "log(", binding.btnSqrt to "√("
        )
        functionKeys.forEach { (view, text) ->
            view.setOnClickListener { appendToExpression(text) }
        }

        binding.btnClear.setOnClickListener {
            expression.clear()
            updateDisplay()
        }
        binding.btnBackspace.setOnClickListener {
            if (expression.isNotEmpty()) {
                expression.deleteCharAt(expression.length - 1)
                updateDisplay()
            }
        }
        binding.btnModeAngle.setOnClickListener {
            angleMode = if (angleMode == ScientificCalculatorEngine.AngleMode.DEGREE) {
                ScientificCalculatorEngine.AngleMode.RADIAN
            } else {
                ScientificCalculatorEngine.AngleMode.DEGREE
            }
            binding.btnModeAngle.text = if (angleMode == ScientificCalculatorEngine.AngleMode.DEGREE) "DEG" else "RAD"
            updateDisplay()
        }
        binding.btnEquals.setOnClickListener { evaluateExpression() }
    }

    private fun appendToExpression(text: String) {
        expression.append(text)
        updateDisplay()
    }

    /** Live-preview hasil sambil mengetik; ekspresi belum lengkap (mis. "2+" atau "sin(")
     * wajar gagal di-evaluate, jadi preview lama dibiarkan tampil, bukan dikosongkan. */
    private fun updateDisplay() {
        binding.tvExpression.text = expression.toString()
        if (expression.isEmpty()) {
            binding.tvResult.text = "0"
            return
        }
        try {
            binding.tvResult.text = formatResult(ScientificCalculatorEngine.evaluate(expression.toString(), angleMode))
        } catch (e: Exception) {
            // biarkan hasil preview sebelumnya
        }
    }

    private fun evaluateExpression() {
        if (expression.isEmpty()) return
        try {
            val formatted = formatResult(ScientificCalculatorEngine.evaluate(expression.toString(), angleMode))
            binding.tvResult.text = formatted
            expression.clear()
            expression.append(formatted)
        } catch (e: Exception) {
            binding.tvResult.text = "Error"
        }
    }

    private fun formatResult(value: Double): String {
        if (abs(value) < 1e15 && value == Math.floor(value)) {
            return value.toLong().toString()
        }
        val formatted = String.format(Locale.US, "%.10f", value)
        return formatted.trimEnd('0').trimEnd('.')
    }
}
