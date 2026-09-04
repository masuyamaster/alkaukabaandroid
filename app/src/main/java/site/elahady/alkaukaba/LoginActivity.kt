package site.elahady.alkaukaba

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import site.elahady.alkaukaba.model.UserData
import site.elahady.alkaukaba.utils.AuthClient
import site.elahady.alkaukaba.utils.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Tidak mengecek resultCode di sini: saat gagal, GMS sering mengembalikan
        // RESULT_CANCELED walau penyebabnya bukan pembatalan user (mis. DEVELOPER_ERROR
        // karena SHA-1/package mismatch). Kode error asli hanya bisa didapat lewat
        // ApiException dari getSignedInAccountFromIntent, jadi selalu coba proses intent-nya.
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
            Log.e("GOOGLE_AUTH", "Google Sign-In failed. resultCode=${result.resultCode} statusCode=${e.statusCode} message=${e.message}")
            Toast.makeText(this, "Error Code: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        // Latar layar login gelap (nuansa malam) -> ikon status bar harus terang.
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

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

        // Segmented tab di atas form (Masuk / Daftar)
        binding.tabMasuk.setOnClickListener { showLoginForm() }
        binding.tabDaftar.setOnClickListener { showRegisterForm() }

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

        // --- TOGGLE LIHAT PASSWORD ---
        binding.ivTogglePassword.setOnClickListener {
            togglePasswordVisibility(binding.etPassword, binding.ivTogglePassword)
        }
        binding.ivToggleRegPassword.setOnClickListener {
            togglePasswordVisibility(binding.etRegPassword, binding.ivToggleRegPassword)
        }
    }

    private fun togglePasswordVisibility(editText: EditText, toggleIcon: ImageView) {
        val isHidden = editText.inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD != 0
        if (isHidden) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleIcon.alpha = 1.0f
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleIcon.alpha = 0.55f
        }
        editText.setSelection(editText.text.length)
    }

    private fun showLoginForm() {
        binding.groupRegister.visibility = View.GONE
        binding.groupLogin.visibility = View.VISIBLE
        binding.tabMasuk.setBackgroundResource(R.drawable.bg_tab_gold_active)
        binding.tabMasuk.setTextColor(ContextCompat.getColor(this, R.color.login_bg_deep))
        binding.tabDaftar.background = null
        binding.tabDaftar.setTextColor(ContextCompat.getColor(this, R.color.color_secondary))
    }

    private fun showRegisterForm() {
        binding.groupLogin.visibility = View.GONE
        binding.groupRegister.visibility = View.VISIBLE
        binding.tabDaftar.setBackgroundResource(R.drawable.bg_tab_gold_active)
        binding.tabDaftar.setTextColor(ContextCompat.getColor(this, R.color.login_bg_deep))
        binding.tabMasuk.background = null
        binding.tabMasuk.setTextColor(ContextCompat.getColor(this, R.color.color_secondary))
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
                    binding.btnRegisterSubmit.text = "DAFTAR"
                    binding.btnRegisterSubmit.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!

                        if (apiResponse.status == "success") {
                            Toast.makeText(this@LoginActivity, "Register Berhasil! Sedang mengalihkan...", Toast.LENGTH_SHORT).show()

                            val sessionManager = SessionManager(this@LoginActivity)
                            sessionManager.setLogin(true)
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
                    binding.btnRegisterSubmit.text = "DAFTAR"
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
                    binding.btnSignIn.text = "MASUK"
                    binding.btnSignIn.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!

                        if (apiResponse.status == "success") {
                            Toast.makeText(this@LoginActivity, "Selamat Datang, ${apiResponse.data?.username}!", Toast.LENGTH_SHORT).show()

                            val sessionManager = SessionManager(this@LoginActivity)
                            sessionManager.setLogin(true)
                            persistUserData(sessionManager, apiResponse.data)
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
                    binding.btnSignIn.text = "MASUK"
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

                            val sessionManager = SessionManager(this@LoginActivity)
                            sessionManager.setLogin(true)
                            persistUserData(sessionManager, apiResponse.data)
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

    private fun persistUserData(sessionManager: SessionManager, data: UserData?) {
        if (data == null) return
        sessionManager.setUserId(data.id)
        sessionManager.setUserName(data.username)
        sessionManager.setEmail(data.email)
        data.token?.let { sessionManager.setAuthToken(it) }
    }
}