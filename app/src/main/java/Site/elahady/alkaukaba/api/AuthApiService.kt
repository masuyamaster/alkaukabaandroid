package site.elahady.alkaukaba.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import site.elahady.alkaukaba.model.ApiResponse
import site.elahady.alkaukaba.model.ChangePasswordRequest
import site.elahady.alkaukaba.model.DeleteAccountRequest
import site.elahady.alkaukaba.model.GoogleLoginRequest
import site.elahady.alkaukaba.model.LoginRequest
import site.elahady.alkaukaba.model.RegisterRequest
import site.elahady.alkaukaba.model.UpdateProfileRequest

interface AuthApiService {
    @POST("api.php?action=login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse>

    @POST("api.php?action=register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse>

    @POST("api.php?action=google_login")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<ApiResponse>

    @POST("api.php?action=update_profile")
    suspend fun updateProfile(@Header("Authorization") bearerToken: String, @Body request: UpdateProfileRequest): Response<ApiResponse>

    @POST("api.php?action=change_password")
    suspend fun changePassword(@Header("Authorization") bearerToken: String, @Body request: ChangePasswordRequest): Response<ApiResponse>

    @POST("api.php?action=delete_account")
    suspend fun deleteAccount(@Header("Authorization") bearerToken: String, @Body request: DeleteAccountRequest): Response<ApiResponse>

    @Multipart
    @POST("api.php?action=upload_avatar")
    suspend fun uploadAvatar(@Header("Authorization") bearerToken: String, @Part photo: MultipartBody.Part): Response<ApiResponse>

    @POST("api.php?action=delete_avatar")
    suspend fun deleteAvatar(@Header("Authorization") bearerToken: String): Response<ApiResponse>
}