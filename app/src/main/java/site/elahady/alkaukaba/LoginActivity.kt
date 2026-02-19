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
import site.elahady.alkaukaba.model.RegisterRequest
import site.elahady.alkaukaba.utils.AuthClient

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // ==========================================
        // LOGIC FORM LOGIN
        // ==========================================
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

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

            performLogin(email, password)
        }

        // ==========================================
        // TOGGLE ANIMATION (LOGIN <-> REGISTER)
        // ==========================================

        // Saat 'REGISTER' di halaman login ditekan
        binding.tvRegister.setOnClickListener {
            binding.groupLogin.visibility = View.GONE
            binding.groupRegister.visibility = View.VISIBLE
        }

        // Saat 'SIGN IN' di halaman register ditekan
        binding.tvBackToLogin.setOnClickListener {
            binding.groupRegister.visibility = View.GONE
            binding.groupLogin.visibility = View.VISIBLE
        }

        // ==========================================
        // LOGIC FORM REGISTER
        // ==========================================
        binding.btnRegisterSubmit.setOnClickListener {
            val username = binding.etRegUsername.text.toString().trim()
            val email = binding.etRegEmail.text.toString().trim()
            val password = binding.etRegPassword.text.toString().trim()

            if (username.isEmpty()) {
                binding.etRegUsername.error = "Username tidak boleh kosong"
                binding.etRegUsername.requestFocus()
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                binding.etRegEmail.error = "Email tidak boleh kosong"
                binding.etRegEmail.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etRegPassword.error = "Password tidak boleh kosong"
                binding.etRegPassword.requestFocus()
                return@setOnClickListener
            }

            performRegister(username, email, password)
        }

        // --- TOMBOL GOOGLE ---
        binding.cvGoogle.setOnClickListener {
            Toast.makeText(this, "Fitur Google Sign-In menyusul", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performRegister(username: String, email: String, pass: String) {
        binding.btnRegisterSubmit.text = "Loading..."
        binding.btnRegisterSubmit.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Gunakan model RegisterRequest yang sudah Anda buat
                val request = RegisterRequest(username, email, pass)
                val response = AuthClient.instance.register(request)

                withContext(Dispatchers.Main) {
                    binding.btnRegisterSubmit.text = "REGISTER"
                    binding.btnRegisterSubmit.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!

                        if (apiResponse.status == "success") {
                            Toast.makeText(this@LoginActivity, "Register Berhasil! Sedang mengalihkan...", Toast.LENGTH_SHORT).show()

                            // LANGSUNG LOGIN OTOMATIS JIKA REGISTER SUKSES
                            performLogin(email, pass)
                        } else {
                            Toast.makeText(this@LoginActivity, apiResponse.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Register gagal, periksa jaringan/server", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnRegisterSubmit.text = "REGISTER"
                    binding.btnRegisterSubmit.isEnabled = true
                    Toast.makeText(this@LoginActivity, "Error koneksi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun performLogin(email: String, pass: String) {
        binding.btnSignIn.text = "Loading..."
        binding.btnSignIn.isEnabled = false

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
                        Toast.makeText(this@LoginActivity, "Login gagal, periksa email dan password", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSignIn.text = "SIGN IN"
                    binding.btnSignIn.isEnabled = true
                    Toast.makeText(this@LoginActivity, "Error koneksi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}