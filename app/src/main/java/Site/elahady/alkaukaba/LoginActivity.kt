package Site.elahady.alkaukaba

import Site.elahady.alkaukaba.databinding.ActivityLoginBinding
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Setup View Binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- TOMBOL SIGN IN ---
        binding.btnSignIn.setOnClickListener {
            // 1. Buat Intent untuk menuju MainActivity
            val intent = Intent(this, MainActivity::class.java)

            // 2. Jalankan Activity
            startActivity(intent)

            // 3. (Opsional) Tutup LoginActivity agar user tidak bisa kembali
            // ke halaman login saat menekan tombol back
            finish()
        }
    }
}