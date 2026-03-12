package Site.elahady.alkaukaba.model


data class HilalInput(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double, // Meter
    val refraction: Double,
    val date: java.util.Calendar
)

data class HilalResult(
    val ijtimaTime: String,
    val ghurubTime: String,
    val moonAltitude: String, // Tinggi Hilal
    val moonElongation: String,
    val calculationLog: String // String panjang untuk isi PDF
)
