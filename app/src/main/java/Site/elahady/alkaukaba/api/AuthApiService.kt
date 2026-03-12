package site.elahady.alkaukaba.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import site.elahady.alkaukaba.model.ApiResponse
import site.elahady.alkaukaba.model.GoogleLoginRequest
import site.elahady.alkaukaba.model.LoginRequest
import site.elahady.alkaukaba.model.RegisterRequest

interface AuthApiService {
    @POST("api.php?action=login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse>

    @POST("api.php?action=register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse>

    @POST("api.php?action=google_login")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<ApiResponse>
}