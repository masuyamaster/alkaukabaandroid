package site.elahady.alkaukaba

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import site.elahady.alkaukaba.databinding.ActivitySplashBinding
import site.elahady.alkaukaba.utils.SessionManager

class Splashscreen : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        // Latar splash gelap (nuansa malam) -> ikon status bar harus terang.
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        pulseDot(binding.dot1, 0L)
        pulseDot(binding.dot2, 150L)
        pulseDot(binding.dot3, 300L)

        // Inisialisasi SessionManager
        sessionManager = SessionManager(this)

        Handler(Looper.getMainLooper()).postDelayed({

            // Cek status session di sini
            val intent = if (sessionManager.isLoggedIn()) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }

            startActivity(intent)
            finish()

        }, 3000L)
    }

    private fun pulseDot(dot: View, startDelay: Long) {
        dot.alpha = 0.25f
        ObjectAnimator.ofFloat(dot, View.ALPHA, 0.25f, 1f, 0.25f).apply {
            duration = 1200L
            this.startDelay = startDelay
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }
}