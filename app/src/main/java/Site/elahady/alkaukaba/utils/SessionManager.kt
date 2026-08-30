package site.elahady.alkaukaba.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("AppSession", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PRAYER_METHOD_ID = "PRAYER_METHOD_ID"
        private const val KEY_PRAYER_CUSTOM_FAJR = "PRAYER_CUSTOM_FAJR"
        private const val KEY_PRAYER_CUSTOM_ISHA = "PRAYER_CUSTOM_ISHA"
        const val DEFAULT_PRAYER_METHOD_ID = PrayerCalculationMethods.EPHEMERIS_ID
        const val DEFAULT_CUSTOM_FAJR_ANGLE = 20.0
        const val DEFAULT_CUSTOM_ISHA_ANGLE = 18.0
    }

    fun setPrayerMethodId(methodId: Int) {
        prefs.edit().putInt(KEY_PRAYER_METHOD_ID, methodId).apply()
    }

    fun getPrayerMethodId(): Int = prefs.getInt(KEY_PRAYER_METHOD_ID, DEFAULT_PRAYER_METHOD_ID)

    fun setCustomPrayerAngles(fajrAngle: Double, ishaAngle: Double) {
        prefs.edit()
            .putFloat(KEY_PRAYER_CUSTOM_FAJR, fajrAngle.toFloat())
            .putFloat(KEY_PRAYER_CUSTOM_ISHA, ishaAngle.toFloat())
            .apply()
    }

    fun getCustomFajrAngle(): Double = prefs.getFloat(KEY_PRAYER_CUSTOM_FAJR, DEFAULT_CUSTOM_FAJR_ANGLE.toFloat()).toDouble()

    fun getCustomIshaAngle(): Double = prefs.getFloat(KEY_PRAYER_CUSTOM_ISHA, DEFAULT_CUSTOM_ISHA_ANGLE.toFloat()).toDouble()

    /** Format query `methodSettings` Aladhan API ("fajrAngle,maghribOffset,ishaAngle"), hanya relevan saat method Custom (99). */
    fun getMethodSettingsQuery(): String? {
        if (getPrayerMethodId() != PrayerCalculationMethods.CUSTOM_ID) return null
        return "${getCustomFajrAngle()},0,${getCustomIshaAngle()}"
    }

    fun setLogin(isLoggedIn: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean("IS_LOGGED_IN", isLoggedIn)
        editor.apply()
    }

    fun setUserName(username : String) {
        val editor = prefs.edit()
        editor.putString("USERNAME",username)
        editor.apply()
    }


    fun setEmail(email : String) {
        val editor = prefs.edit()
        editor.putString("EMAIL",email)
        editor.apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("IS_LOGGED_IN", false)
    }
}