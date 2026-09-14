package site.elahady.alkaukaba.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Pure logic test untuk parser [ScientificCalculatorEngine] (tanpa Android/API/DB) -
 * mengecek precedence operator, fungsi trigonometri di kedua [ScientificCalculatorEngine.AngleMode],
 * dan penanganan error (bagi nol, ekspresi tidak valid).
 */
class ScientificCalculatorEngineTest {

    private val deg = ScientificCalculatorEngine.AngleMode.DEGREE
    private val rad = ScientificCalculatorEngine.AngleMode.RADIAN

    @Test
    fun `operator dasar mengikuti precedence matematika standar`() {
        assertEquals(14.0, ScientificCalculatorEngine.evaluate("2+3*4", deg), 1e-9)
        assertEquals(20.0, ScientificCalculatorEngine.evaluate("(2+3)*4", deg), 1e-9)
        assertEquals(8.0, ScientificCalculatorEngine.evaluate("2^3", deg), 1e-9)
        assertEquals(-8.0, ScientificCalculatorEngine.evaluate("-2^3+0", deg), 1e-9)
        assertEquals(0.5, ScientificCalculatorEngine.evaluate("50%", deg), 1e-9)
    }

    @Test
    fun `pangkat kanan-asosiatif`() {
        // 2^(3^2) = 2^9 = 512, bukan (2^3)^2 = 64
        assertEquals(512.0, ScientificCalculatorEngine.evaluate("2^3^2", deg), 1e-9)
    }

    @Test
    fun `fungsi trigonometri menghormati mode derajat vs radian`() {
        assertEquals(1.0, ScientificCalculatorEngine.evaluate("sin(90)", deg), 1e-9)
        assertEquals(0.0, ScientificCalculatorEngine.evaluate("sin(0)", rad), 1e-9)
        assertEquals(1.0, ScientificCalculatorEngine.evaluate("sin(pi/2)", rad), 1e-9)
        assertEquals(90.0, ScientificCalculatorEngine.evaluate("asin(1)", deg), 1e-9)
    }

    @Test
    fun `fungsi log, ln, sqrt, dan konstanta`() {
        assertEquals(2.0, ScientificCalculatorEngine.evaluate("log(100)", deg), 1e-9)
        assertEquals(1.0, ScientificCalculatorEngine.evaluate("ln(e)", deg), 1e-9)
        assertEquals(3.0, ScientificCalculatorEngine.evaluate("sqrt(9)", deg), 1e-9)
        assertEquals(Math.PI, ScientificCalculatorEngine.evaluate("pi", deg), 1e-9)
        assertEquals(Math.PI, ScientificCalculatorEngine.evaluate("π", deg), 1e-9)
        assertEquals(3.0, ScientificCalculatorEngine.evaluate("√(9)", deg), 1e-9)
    }

    @Test
    fun `ekspresi kompleks dengan fungsi dan parentheses bersarang`() {
        // sin(30) + 2*(3-1)^2 = 0.5 + 8 = 8.5
        assertEquals(8.5, ScientificCalculatorEngine.evaluate("sin(30)+2*(3-1)^2", deg), 1e-9)
    }

    @Test
    fun `pembagian dengan nol melempar ExpressionError`() {
        assertThrows(ScientificCalculatorEngine.ExpressionError::class.java) {
            ScientificCalculatorEngine.evaluate("5/0", deg)
        }
    }

    @Test
    fun `akar dari bilangan negatif melempar ExpressionError`() {
        assertThrows(ScientificCalculatorEngine.ExpressionError::class.java) {
            ScientificCalculatorEngine.evaluate("sqrt(-4)", deg)
        }
    }

    @Test
    fun `ekspresi dengan parentheses tidak seimbang melempar ExpressionError`() {
        assertThrows(ScientificCalculatorEngine.ExpressionError::class.java) {
            ScientificCalculatorEngine.evaluate("(2+3", deg)
        }
    }

    @Test
    fun `token tidak dikenal melempar ExpressionError`() {
        assertThrows(ScientificCalculatorEngine.ExpressionError::class.java) {
            ScientificCalculatorEngine.evaluate("2+xyz", deg)
        }
    }
}
