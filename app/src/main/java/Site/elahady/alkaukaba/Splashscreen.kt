package Site.elahady.alkaukaba

import Site.elahady.alkaukaba.databinding.ActivityMainBinding
import Site.elahady.alkaukaba.databinding.ActivitySplashBinding
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class Splashscreen : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3000L artinya 3000 milidetik (3 detik)
        Handler(Looper.getMainLooper()).postDelayed({

            // Intent untuk pindah ke LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            // finish() digunakan agar pengguna tidak bisa kembali ke Splashscreen
            // saat menekan tombol back di HP
            finish()

        }, 3000L)
    }
}