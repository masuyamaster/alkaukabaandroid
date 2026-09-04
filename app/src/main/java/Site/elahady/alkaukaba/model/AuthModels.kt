package site.elahady.alkaukaba.model

import com.google.gson.annotations.SerializedName

// Data class untuk Request API
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val username: String, val email: String, val password: String)
data class UpdateProfileRequest(val username: String)
data class ChangePasswordRequest(val current_password: String, val new_password: String)
data class DeleteAccountRequest(val password: String)

// Data class untuk Response API
data class ApiResponse(
    val status: String,
    val message: String,
    val data: UserData? = null
)

data class UserData(
    val id: Int,
    val username: String,
    val email: String,
    val avatar_url: String? = null,
    // Hanya terisi dari login/register/google_login/change_password (endpoint yang
    // menerbitkan token baru) - update_profile sengaja tidak menerbitkan token baru.
    val token: String? = null
)
