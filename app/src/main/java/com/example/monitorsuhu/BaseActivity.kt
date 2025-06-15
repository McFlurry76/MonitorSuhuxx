package com.example.monitorsuhu // GANTI DENGAN PACKAGE NAME ANDA

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val sharedPreferences = newBase.getSharedPreferences("Settings", MODE_PRIVATE)
        val language = sharedPreferences.getString("My_Lang", "en") ?: "en"
        val locale = Locale(language)
        val context = newBase.createConfigurationContext(newBase.resources.configuration.apply { setLocale(locale) })
        super.attachBaseContext(context)
    }
}
