package site.elahady.alkaukaba.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("AppSession", Context.MODE_PRIVATE)

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