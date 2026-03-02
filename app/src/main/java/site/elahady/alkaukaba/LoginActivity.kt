package site.elahady.alkaukaba

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import site.elahady.alkaukaba.databinding.ActivityLoginBinding
import site.elahady.alkaukaba.model.GoogleLoginRequest
import site.elahady.alkaukaba.model.LoginRequest
import site.elahady.alkaukaba.model.RegisterRequest
import site.elahady.alkaukaba.utils.AuthClient

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken

                if (idToken != null) {
                    // Berhasil! Lanjut ke API kita
                    performGoogleLogin(idToken)
                } else {
                    Log.e("GOOGLE_AUTH", "Token null, tapi login sukses.")
                    Toast.makeText(this, "Gagal mendapatkan ID Token", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                // INI YANG PALING PENTING: Menangkap kode error dari Google
                Log.e("GOOGLE_AUTH", "Google Sign-In failed. Error Code: ${e.statusCode}")
                Toast.makeText(this, "Error Code: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.e("GOOGLE_AUTH", "Result Code bukan RESULT_OK. User mungkin membatalkan popup.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // GANTI DENGAN WEB CLIENT ID DARI GOOGLE CLOUD CONSOLE
            .requestIdToken("604243092609-94nm2tlm46e3slr0vboe9n41inouvlj7.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

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
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
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

    private fun performGoogleLogin(idToken: String) {
        // Tampilkan indikator loading (opsional, sesuaikan dengan UI Anda)
        Toast.makeText(this, "Memverifikasi akun Google...", Toast.LENGTH_SHORT).show()
        println("idtoken :: $idToken")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Bungkus token dalam model request
                val request = GoogleLoginRequest(idToken)
                println("idtoken :: " + idToken)

                // Panggil endpoint khusus Google Login di Retrofit Anda
                val response = AuthClient.instance.googleLogin(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!

                        if (apiResponse.status == "success") {
                            Toast.makeText(this@LoginActivity, "Login Google Berhasil!", Toast.LENGTH_SHORT).show()

                            // Lanjut ke MainActivity
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, apiResponse.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Verifikasi gagal di server", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Error koneksi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}