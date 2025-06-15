package com.example.monitorsuhu // GANTI DENGAN PACKAGE NAME ANDA

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import kotlin.math.hypot

class SettingsActivity : BaseActivity() {

    // Kebutuhan untuk Animasi
    companion object {
        const val EXTRA_REVEAL_X = "REVEAL_X"
        const val EXTRA_REVEAL_Y = "REVEAL_Y"
    }
    private lateinit var rootLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        rootLayout = findViewById(R.id.root_layout_settings)

        if (savedInstanceState == null) {
            rootLayout.visibility = View.INVISIBLE
            rootLayout.post { revealActivity() }
        }

        val languageSettingButton: TextView = findViewById(R.id.language_setting)
        languageSettingButton.setOnClickListener {
            showLanguageDialog()
        }

        setupOnBackPressed()
    }

    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                unRevealActivity()
            }
        })
    }

    private fun revealActivity() {
        val revealX = intent.getIntExtra(EXTRA_REVEAL_X, 0)
        val revealY = intent.getIntExtra(EXTRA_REVEAL_Y, 0)
        val finalRadius = hypot(rootLayout.width.toDouble(), rootLayout.height.toDouble()).toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(rootLayout, revealX, revealY, 0f, finalRadius)
        rootLayout.visibility = View.VISIBLE
        anim.duration = 400
        anim.start()
    }

    private fun unRevealActivity() {
        val revealX = intent.getIntExtra(EXTRA_REVEAL_X, 0)
        val revealY = intent.getIntExtra(EXTRA_REVEAL_Y, 0)
        val initialRadius = hypot(rootLayout.width.toDouble(), rootLayout.height.toDouble()).toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(rootLayout, revealX, revealY, initialRadius, 0f)
        anim.duration = 400
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                rootLayout.visibility = View.INVISIBLE
                finish()
                overridePendingTransition(0, 0)
            }
        })
        anim.start()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Bahasa Indonesia")
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_language_title))
            .setItems(languages) { dialog, which ->
                val languageCode = if (which == 0) "en" else "in"
                setLocale(languageCode)
                dialog.dismiss()
            }
            .show()
    }

    private fun setLocale(languageCode: String) {
        val editor = getSharedPreferences("Settings", MODE_PRIVATE).edit()
        editor.putString("My_Lang", languageCode)
        editor.apply()

        // Restart aplikasi untuk menerapkan bahasa baru secara konsisten
        val intent = Intent(this, HubActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}