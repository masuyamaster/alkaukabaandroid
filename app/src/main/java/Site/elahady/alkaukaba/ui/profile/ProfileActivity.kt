package site.elahady.alkaukaba.ui.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import site.elahady.alkaukaba.LoginActivity
import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.databinding.ActivityProfileBinding
import site.elahady.alkaukaba.model.ChangePasswordRequest
import site.elahady.alkaukaba.model.DeleteAccountRequest
import site.elahady.alkaukaba.model.UpdateProfileRequest
import site.elahady.alkaukaba.utils.AuthClient
import site.elahady.alkaukaba.utils.ImageUtils
import site.elahady.alkaukaba.utils.SessionManager
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager

    private var pendingCameraUri: Uri? = null

    // Avatar & scrim dari sheet Edit Profil, kalau sedang terbuka - dilacak supaya status
    // upload/hasil bisa direfleksikan di sana juga tanpa menutup sheet-nya.
    private var editSheetAvatarView: ImageView? = null
    private var editSheetScrim: View? = null

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else Toast.makeText(this, "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show()
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCameraUri?.let { handlePickedImage(it) }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { handlePickedImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.scrollContent.applySystemBarInsetsPadding(applyBottom = true)

        sessionManager = SessionManager(this)

        binding.includeToolbar.tvToolbarTitle.text = "Profil"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        renderAccountInfo()

        binding.btnEditProfile.setOnClickListener { showEditProfileSheet() }
        binding.ivAvatar.setOnClickListener { showAvatarActionSheet() }
        binding.ivAvatarBadge.setOnClickListener { showAvatarActionSheet() }
        binding.rowHelp.setOnClickListener { showHelpDialog() }
        binding.rowPrivacy.setOnClickListener { showPrivacyDialog() }
        binding.rowChangePassword.setOnClickListener { showChangePasswordSheet() }
        binding.rowLogout.setOnClickListener { showLogoutConfirmation() }
        binding.rowDeleteAccount.setOnClickListener { showDeleteAccountConfirmation() }
    }

    private fun renderAccountInfo() {
        binding.tvUserName.text = sessionManager.getUserName() ?: "Pengguna"
        binding.tvUserEmail.text = sessionManager.getEmail() ?: "-"
        loadAvatarInto(binding.ivAvatar, sessionManager.getAvatarUrl())
    }

    /** Tampilkan foto profil (Glide, dibulatkan) kalau ada, atau kembalikan placeholder ikon
     * gold+navy default kalau belum/tidak ada foto - imageTintList WAJIB dibersihkan sebelum
     * load foto asli, kalau tidak foto ikut ke-tint navy seperti ikon placeholder-nya. */
    private fun loadAvatarInto(imageView: ImageView, avatarUrl: String?) {
        val paddingPx = (18 * resources.displayMetrics.density).toInt()
        if (avatarUrl.isNullOrBlank()) {
            Glide.with(this).clear(imageView)
            imageView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            imageView.setImageResource(R.drawable.ic_person)
            imageView.background = ContextCompat.getDrawable(this, R.drawable.bg_circle_button)
            imageView.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gold_accent)
            imageView.imageTintList = ContextCompat.getColorStateList(this, R.color.login_bg_deep)
        } else {
            imageView.background = null
            imageView.imageTintList = null
            imageView.setPadding(0, 0, 0, 0)
            Glide.with(this)
                .load(avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .into(imageView)
        }
    }

    // --- Foto Profil ---

    private fun showAvatarActionSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_avatar_action, null)
        bottomSheetDialog.setContentView(view)

        val hasPhoto = !sessionManager.getAvatarUrl().isNullOrBlank()
        view.findViewById<View>(R.id.rowRemovePhoto).visibility = if (hasPhoto) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.dividerRemovePhoto).visibility = if (hasPhoto) View.VISIBLE else View.GONE

        view.findViewById<View>(R.id.rowTakePhoto).setOnClickListener {
            bottomSheetDialog.dismiss()
            requestCameraAndCapture()
        }
        view.findViewById<View>(R.id.rowPickGallery).setOnClickListener {
            bottomSheetDialog.dismiss()
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        view.findViewById<View>(R.id.rowRemovePhoto).setOnClickListener {
            bottomSheetDialog.dismiss()
            confirmRemoveAvatar()
        }

        bottomSheetDialog.show()
    }

    private fun requestCameraAndCapture() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        val imagesDir = File(cacheDir, "images").apply { mkdirs() }
        val photoFile = File(imagesDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun handlePickedImage(uri: Uri) {
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) { ImageUtils.prepareAvatarFile(this@ProfileActivity, uri) }
            if (file == null) {
                Toast.makeText(this@ProfileActivity, "Gagal memproses foto", Toast.LENGTH_SHORT).show()
                return@launch
            }
            uploadAvatarFile(file)
        }
    }

    private fun uploadAvatarFile(file: File) {
        val bearer = requireBearerToken() ?: run { file.delete(); return }

        setAvatarUploading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestBody = RequestBody.create(MediaType.parse("image/jpeg"), file)
                val part = MultipartBody.Part.createFormData("photo", file.name, requestBody)
                val response = AuthClient.instance.uploadAvatar(bearer, part)
                withContext(Dispatchers.Main) {
                    setAvatarUploading(false)
                    val body = response.body()
                    if (response.isSuccessful && body?.status == "success") {
                        sessionManager.setAvatarUrl(body.data?.avatar_url)
                        renderAccountInfo()
                        editSheetAvatarView?.let { loadAvatarInto(it, sessionManager.getAvatarUrl()) }
                        Toast.makeText(this@ProfileActivity, "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileActivity, body?.message ?: "Gagal mengunggah foto", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setAvatarUploading(false)
                    Toast.makeText(this@ProfileActivity, "Error koneksi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                file.delete()
            }
        }
    }

    private fun confirmRemoveAvatar() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Foto Profil?")
            .setMessage("Foto profil akan dihapus dan avatar kembali ke default.")
            .setPositiveButton("Hapus") { dialog, _ ->
                dialog.dismiss()
                removeAvatar()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun removeAvatar() {
        val bearer = requireBearerToken() ?: return

        setAvatarUploading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = AuthClient.instance.deleteAvatar(bearer)
                withContext(Dispatchers.Main) {
                    setAvatarUploading(false)
                    val body = response.body()
                    if (response.isSuccessful && body?.status == "success") {
                        sessionManager.setAvatarUrl(null)
                        renderAccountInfo()
                        editSheetAvatarView?.let { loadAvatarInto(it, null) }
                        Toast.makeText(this@ProfileActivity, "Foto profil dihapus", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileActivity, body?.message ?: "Gagal menghapus foto", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setAvatarUploading(false)
                    Toast.makeText(this@ProfileActivity, "Error koneksi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setAvatarUploading(uploading: Boolean) {
        binding.avatarUploadScrim.visibility = if (uploading) View.VISIBLE else View.GONE
        editSheetScrim?.visibility = if (uploading) View.VISIBLE else View.GONE
    }

    /** Bearer token untuk update_profile/change_password/delete_account - null kalau belum ada
     * (mis. akun login sebelum fitur token ini ada, butuh login ulang sekali). */
    private fun requireBearerToken(): String? {
        val token = sessionManager.getAuthToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Sesi belum lengkap, silakan logout lalu login ulang dulu", Toast.LENGTH_LONG).show()
            return null
        }
        return "Bearer $token"
    }

    // --- A. Edit Profil (nama) ---

    private fun showEditProfileSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        bottomSheetDialog.setContentView(view)

        val etUsername = view.findViewById<EditText>(R.id.etProfileUsername)
        val btnSave = view.findViewById<AppCompatButton>(R.id.btnSaveProfile)
        etUsername.setText(sessionManager.getUserName())

        val ivAvatarSheet = view.findViewById<ImageView>(R.id.ivAvatarSheet)
        loadAvatarInto(ivAvatarSheet, sessionManager.getAvatarUrl())
        ivAvatarSheet.setOnClickListener { showAvatarActionSheet() }
        view.findViewById<ImageView>(R.id.ivAvatarBadgeSheet).setOnClickListener { showAvatarActionSheet() }
        editSheetAvatarView = ivAvatarSheet
        editSheetScrim = view.findViewById(R.id.avatarUploadScrimSheet)
        bottomSheetDialog.setOnDismissListener {
            editSheetAvatarView = null
            editSheetScrim = null
        }

        btnSave.setOnClickListener {
            val newUsername = etUsername.text.toString().trim()
            if (newUsername.isEmpty()) {
                etUsername.error = "Nama tidak boleh kosong"
                return@setOnClickListener
            }

            val bearer = requireBearerToken() ?: return@setOnClickListener

            btnSave.isEnabled = false
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = AuthClient.instance.updateProfile(bearer, UpdateProfileRequest(newUsername))
                    withContext(Dispatchers.Main) {
                        btnSave.isEnabled = true
                        val body = response.body()
                        if (response.isSuccessful && body?.status == "success") {
                            sessionManager.setUserName(body.data?.username ?: newUsername)
                            renderAccountInfo()
                            Toast.makeText(this@ProfileActivity, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            bottomSheetDialog.dismiss()
                        } else {
                            Toast.makeText(this@ProfileActivity, body?.message ?: "Gagal memperbarui profil", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnSave.isEnabled = true
                        Toast.makeText(this@ProfileActivity, "Error koneksi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        bottomSheetDialog.show()
    }

    // --- C. Bantuan & Informasi (placeholder - belum ada kontak/kebijakan resmi) ---

    private fun showHelpDialog() {
        showInfoDialog(
            title = "Hubungi Kami / Bantuan",
            body = "Menemukan bug, atau jadwal sholat/arah kiblat terasa tidak akurat? " +
                "Kontak resmi tim Al-Kaukaba akan ditampilkan di sini (email/WhatsApp) - " +
                "placeholder ini akan diganti setelah kanal bantuan resmi ditentukan."
        )
    }

    private fun showPrivacyDialog() {
        showInfoDialog(
            title = "Kebijakan Privasi & Syarat Ketentuan",
            body = "Aplikasi Al-Kaukaba mengumpulkan data akun (nama pengguna, email) dan data " +
                "lokasi (GPS atau koordinat manual) semata-mata untuk menghitung waktu sholat, " +
                "arah kiblat, dan kalender Hijriyah secara akurat sesuai lokasi Anda. Data lokasi " +
                "tidak dibagikan ke pihak ketiga di luar penyedia perhitungan falak yang dipakai " +
                "app ini.\n\nIni masih draft placeholder - teks kebijakan privasi & syarat " +
                "ketentuan final akan menggantikan ini setelah disusun."
        )
    }

    private fun showInfoDialog(title: String, body: String) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_info_simple, null)
        bottomSheetDialog.setContentView(view)
        view.findViewById<TextView>(R.id.tvInfoTitle).text = title
        view.findViewById<TextView>(R.id.tvInfoBody).text = body
        bottomSheetDialog.show()
    }

    // --- D. Keamanan & Tindakan Akun ---

    private fun showChangePasswordSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_change_password, null)
        bottomSheetDialog.setContentView(view)

        val etCurrent = view.findViewById<EditText>(R.id.etCurrentPassword)
        val etNew = view.findViewById<EditText>(R.id.etNewPassword)
        val etConfirm = view.findViewById<EditText>(R.id.etConfirmNewPassword)
        val btnSave = view.findViewById<AppCompatButton>(R.id.btnSaveNewPassword)

        btnSave.setOnClickListener {
            val current = etCurrent.text.toString()
            val newPass = etNew.text.toString()
            val confirm = etConfirm.text.toString()

            if (current.isEmpty()) {
                etCurrent.error = "Wajib diisi"
                return@setOnClickListener
            }
            if (newPass.length < 6) {
                etNew.error = "Minimal 6 karakter"
                return@setOnClickListener
            }
            if (newPass != confirm) {
                etConfirm.error = "Tidak sama dengan kata sandi baru"
                return@setOnClickListener
            }

            val bearer = requireBearerToken() ?: return@setOnClickListener

            btnSave.isEnabled = false
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = AuthClient.instance.changePassword(
                        bearer, ChangePasswordRequest(current, newPass)
                    )
                    withContext(Dispatchers.Main) {
                        btnSave.isEnabled = true
                        val body = response.body()
                        if (response.isSuccessful && body?.status == "success") {
                            // Password berubah -> server menerbitkan token baru, token lama dicabut.
                            body.data?.token?.let { sessionManager.setAuthToken(it) }
                            Toast.makeText(this@ProfileActivity, "Kata sandi berhasil diubah", Toast.LENGTH_SHORT).show()
                            bottomSheetDialog.dismiss()
                        } else {
                            Toast.makeText(this@ProfileActivity, body?.message ?: "Gagal mengubah kata sandi", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnSave.isEnabled = true
                        Toast.makeText(this@ProfileActivity, "Error koneksi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        bottomSheetDialog.show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Keluar")
            .setMessage("Apakah Anda yakin ingin keluar dari akun ini?")
            .setPositiveButton("Ya") { dialog, _ ->
                dialog.dismiss()
                performLogout()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun performLogout() {
        sessionManager.clearUserData()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Akun?")
            .setMessage("Semua data akun Anda akan dihapus permanen dan tidak bisa dikembalikan. Lanjutkan?")
            .setPositiveButton("Lanjutkan") { dialog, _ ->
                dialog.dismiss()
                showDeleteAccountSheet()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showDeleteAccountSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_delete_account, null)
        bottomSheetDialog.setContentView(view)

        val etPassword = view.findViewById<EditText>(R.id.etDeleteConfirmPassword)
        val btnConfirm = view.findViewById<AppCompatButton>(R.id.btnConfirmDeleteAccount)

        btnConfirm.setOnClickListener {
            val password = etPassword.text.toString()
            if (password.isEmpty()) {
                etPassword.error = "Wajib diisi"
                return@setOnClickListener
            }

            val bearer = requireBearerToken() ?: return@setOnClickListener

            btnConfirm.isEnabled = false
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = AuthClient.instance.deleteAccount(bearer, DeleteAccountRequest(password))
                    withContext(Dispatchers.Main) {
                        btnConfirm.isEnabled = true
                        val body = response.body()
                        if (response.isSuccessful && body?.status == "success") {
                            Toast.makeText(this@ProfileActivity, "Akun berhasil dihapus", Toast.LENGTH_SHORT).show()
                            bottomSheetDialog.dismiss()
                            performLogout()
                        } else {
                            Toast.makeText(this@ProfileActivity, body?.message ?: "Gagal menghapus akun", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnConfirm.isEnabled = true
                        Toast.makeText(this@ProfileActivity, "Error koneksi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        bottomSheetDialog.show()
    }
}
