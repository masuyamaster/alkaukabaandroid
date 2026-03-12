package site.elahady.alkaukaba.model

import com.google.gson.annotations.SerializedName

// Data class untuk Request API
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val username: String, val email: String, val password: String)

// Data class untuk Response API
data class ApiResponse(
    val status: String,
    val message: String,
    val data: UserData? = null
)

data class UserData(
    val id: Int,
    val username: String,
    val email: String
)
