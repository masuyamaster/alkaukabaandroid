package site.elahady.alkaukaba.model

data class NearbyMosque(
    val id: Long,
    val name: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Float
)
