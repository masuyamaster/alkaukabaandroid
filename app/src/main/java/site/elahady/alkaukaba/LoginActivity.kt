package site.elahady.alkaukaba

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import site.elahady.alkaukaba.databinding.ActivityLoginBinding
import site.elahady.alkaukaba.model.LoginRequest
import site.elahady.alkaukaba.utils.AuthClient

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // --- TOMBOL SIGN IN ---
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Validasi Input
            if (email.isEmpty()) {
                binding.etEmail.error = "Email tidak boleh kosong"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Password tidak boleh kosong"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            // Jalankan proses login
            performLogin(email, password)
        }

        // --- TOMBOL REGISTER ---
        binding.tvRegister.setOnClickListener {
            // Nanti arahkan ke RegisterActivity
            // val intent = Intent(this, RegisterActivity::class.java)
            // startActivity(intent)
            Toast.makeText(this, "Arahkan ke halaman Register", Toast.LENGTH_SHORT).show()
        }

        // --- TOMBOL GOOGLE (Disiapkan untuk nanti) ---
        binding.cvGoogle.setOnClickListener {
            Toast.makeText(this, "Fitur Google Sign-In menyusul", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogin(email: String, pass: String) {
        // Tampilkan loading (opsional, bisa diganti dengan ProgressBar jika ada di XML)
        binding.btnSignIn.text = "Loading..."
        binding.btnSignIn.isEnabled = false

        // Menjalankan API call di background thread
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = LoginRequest(email, pass)
                val response = AuthClient.instance.login(request)

                withContext(Dispatchers.Main) {
                    binding.btnSignIn.text = "SIGN IN"
                    binding.btnSignIn.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!

                        if (apiResponse.status == "success") {
                            Toast.makeText(this@LoginActivity, "Selamat Datang, ${apiResponse.data?.username}!", Toast.LENGTH_SHORT).show()

                            // Lanjut ke MainActivity
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, apiResponse.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Response code bukan 200 (misal 401 Password Salah / 404 Email Tidak Terdaftar)
                        Toast.makeText(this@LoginActivity, "Login gagal, periksa email dan password", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // Error jaringan atau server down
                withContext(Dispatchers.Main) {
                    binding.btnSignIn.text = "SIGN IN"
                    binding.btnSignIn.isEnabled = true
                    Toast.makeText(this@LoginActivity, "Error koneksi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}