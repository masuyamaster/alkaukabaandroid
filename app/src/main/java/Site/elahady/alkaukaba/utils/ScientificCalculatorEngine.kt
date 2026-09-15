package site.elahady.alkaukaba.utils

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Parser ekspresi kalkulator scientific: recursive-descent tangan-sendiri (bukan
 * exp4j/mXparser) karena tidak ada dependency evaluator ekspresi matematika di
 * project ini, dan operator/fungsi yang dibutuhkan cukup kecil untuk tetap
 * mudah di-maintain inline tanpa dependency baru.
 *
 * Precedence (dari rendah ke tinggi): + - , * / , unary +/-, ^ (kanan-asosiatif),
 * postfix % (dibagi 100, bukan modulo infix).
 */
object ScientificCalculatorEngine {

    enum class AngleMode { DEGREE, RADIAN }

    class ExpressionError(message: String) : Exception(message)

    private val FUNCTIONS = setOf(
        "asin", "acos", "atan", "sin", "cos", "tan", "ln", "log", "sqrt", "cbrt", "abs"
    )

    fun evaluate(input: String, angleMode: AngleMode): Double {
        val tokens = tokenize(input)
        if (tokens.isEmpty()) throw ExpressionError("Ekspresi kosong")
        val parser = Parser(tokens, angleMode)
        val result = parser.parseExpression()
        if (!parser.isAtEnd()) throw ExpressionError("Ekspresi tidak valid")
        if (result.isNaN() || result.isInfinite()) throw ExpressionError("Hasil tidak terdefinisi")
        return result
    }

    private sealed class Token {
        data class Num(val value: Double) : Token()
        data class Sym(val ch: Char) : Token()
        data class Func(val name: String) : Token()
        object Pi : Token()
        object Euler : Token()
    }

    private fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        val s = input.replace("×", "*").replace("÷", "/").replace("−", "-")
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    val numText = s.substring(start, i)
                    tokens += Token.Num(numText.toDoubleOrNull() ?: throw ExpressionError("Angka tidak valid: $numText"))
                }
                c == 'π' -> { tokens += Token.Pi; i++ }
                c == '√' -> { tokens += Token.Func("sqrt"); i++ }
                c in "+-*/^%()" -> { tokens += Token.Sym(c); i++ }
                c.isLetter() -> {
                    val start = i
                    while (i < s.length && s[i].isLetter()) i++
                    val word = s.substring(start, i)
                    when {
                        word == "e" -> tokens += Token.Euler
                        word == "pi" -> tokens += Token.Pi
                        FUNCTIONS.contains(word) -> tokens += Token.Func(word)
                        else -> throw ExpressionError("Token tidak dikenal: $word")
                    }
                }
                else -> throw ExpressionError("Karakter tidak dikenal: $c")
            }
        }
        return tokens
    }

    private class Parser(private val tokens: List<Token>, private val angleMode: AngleMode) {
        private var pos = 0

        fun isAtEnd() = pos >= tokens.size
        private fun peek(): Token? = tokens.getOrNull(pos)
        private fun advance(): Token = tokens[pos++]

        fun parseExpression(): Double = parseAddSub()

        private fun parseAddSub(): Double {
            var value = parseMulDiv()
            while (true) {
                val t = peek()
                if (t is Token.Sym && (t.ch == '+' || t.ch == '-')) {
                    advance()
                    val rhs = parseMulDiv()
                    value = if (t.ch == '+') value + rhs else value - rhs
                } else break
            }
            return value
        }

        private fun parseMulDiv(): Double {
            var value = parsePower()
            while (true) {
                val t = peek()
                if (t is Token.Sym && (t.ch == '*' || t.ch == '/')) {
                    advance()
                    val rhs = parsePower()
                    value = if (t.ch == '*') {
                        value * rhs
                    } else {
                        if (rhs == 0.0) throw ExpressionError("Tidak bisa dibagi nol")
                        value / rhs
                    }
                } else break
            }
            return value
        }

        private fun parsePower(): Double {
            val base = parseUnary()
            val t = peek()
            return if (t is Token.Sym && t.ch == '^') {
                advance()
                base.pow(parsePower())
            } else base
        }

        private fun parseUnary(): Double {
            val t = peek()
            if (t is Token.Sym && (t.ch == '+' || t.ch == '-')) {
                advance()
                val value = parseUnary()
                return if (t.ch == '-') -value else value
            }
            return parsePostfix()
        }

        private fun parsePostfix(): Double {
            var value = parsePrimary()
            while (true) {
                val t = peek()
                if (t is Token.Sym && t.ch == '%') {
                    advance()
                    value /= 100.0
                } else break
            }
            return value
        }

        private fun parsePrimary(): Double {
            val t = peek() ?: throw ExpressionError("Ekspresi tidak lengkap")
            return when (t) {
                is Token.Num -> { advance(); t.value }
                Token.Pi -> { advance(); PI }
                Token.Euler -> { advance(); E }
                is Token.Func -> {
                    advance()
                    expectSym('(')
                    val arg = parseExpression()
                    expectSym(')')
                    applyFunction(t.name, arg)
                }
                is Token.Sym -> {
                    if (t.ch == '(') {
                        advance()
                        val value = parseExpression()
                        expectSym(')')
                        value
                    } else {
                        throw ExpressionError("Ekspresi tidak valid dekat '${t.ch}'")
                    }
                }
            }
        }

        private fun expectSym(ch: Char) {
            val t = peek()
            if (t is Token.Sym && t.ch == ch) advance()
            else throw ExpressionError("Diharapkan '$ch'")
        }

        private fun applyFunction(name: String, arg: Double): Double {
            return when (name) {
                "sin" -> sin(toRadiansIfDegree(arg))
                "cos" -> cos(toRadiansIfDegree(arg))
                "tan" -> tan(toRadiansIfDegree(arg))
                "asin" -> fromRadiansIfDegree(asin(arg))
                "acos" -> fromRadiansIfDegree(acos(arg))
                "atan" -> fromRadiansIfDegree(atan(arg))
                "ln" -> ln(arg)
                "log" -> log10(arg)
                "sqrt" -> {
                    if (arg < 0) throw ExpressionError("Akar dari bilangan negatif")
                    sqrt(arg)
                }
                "cbrt" -> Math.cbrt(arg)
                "abs" -> abs(arg)
                else -> throw ExpressionError("Fungsi tidak dikenal: $name")
            }
        }

        private fun toRadiansIfDegree(value: Double) =
            if (angleMode == AngleMode.DEGREE) Math.toRadians(value) else value

        private fun fromRadiansIfDegree(value: Double) =
            if (angleMode == AngleMode.DEGREE) Math.toDegrees(value) else value
    }
}
