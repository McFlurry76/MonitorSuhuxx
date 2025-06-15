package com.example.monitorsuhu // PASTIKAN SESUAI DENGAN PACKAGE NAME ANDA

import android.content.Intent
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog

class HubActivity : BaseActivity(), TextureView.SurfaceTextureListener {

    private lateinit var textureView: TextureView
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var btnAddDevice: Button
    private lateinit var iconSettings: ImageView
    private lateinit var iconMainMenu: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hub)

        textureView = findViewById(R.id.texture_view_hub)
        btnAddDevice = findViewById(R.id.button_add_device)
        iconSettings = findViewById(R.id.icon_settings)
        iconMainMenu = findViewById(R.id.icon_main_menu)

        textureView.surfaceTextureListener = this
        setupClickListeners()
        setupOnBackPressed()
    }

    private fun setupClickListeners() {
        val buttons = mapOf(
            btnAddDevice to AddDeviceActivity::class.java,
            iconMainMenu to ControlActivity::class.java,
            iconSettings to SettingsActivity::class.java
        )

        buttons.forEach { (button, activityClass) ->
            button.setOnClickListener { view ->
                val intent = Intent(this, activityClass)
                val revealX = (view.x + view.width / 2).toInt()
                val revealY = (view.y + view.height / 2).toInt()
                intent.putExtra("REVEAL_X", revealX)
                intent.putExtra("REVEAL_Y", revealY)
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
        }
    }

    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@HubActivity)
                    .setTitle(getString(R.string.dialog_exit_title))
                    .setMessage(getString(R.string.dialog_exit_message))
                    .setPositiveButton(getString(R.string.dialog_button_exit)) { _, _ -> finish() }
                    .setNeutralButton(getString(R.string.dialog_button_minimize)) { _, _ -> moveTaskToBack(true) }
                    .setNegativeButton(getString(R.string.dialog_button_cancel), null)
                    .show()
            }
        })
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        try {
            val videoPath = "android.resource://" + packageName + "/" + R.raw.hub_video
            val uri = Uri.parse(videoPath)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                setSurface(Surface(surface))
                isLooping = true
                setOnVideoSizeChangedListener { _, videoWidth, videoHeight ->
                    updateTextureViewSize(videoWidth, videoHeight)
                }
                prepareAsync()
                setOnPreparedListener { it.start() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // LOGIKA BARU & FINAL UNTUK VIDEO SCALING
    private fun updateTextureViewSize(videoWidth: Int, videoHeight: Int) {
        val viewWidth = textureView.width
        val viewHeight = textureView.height
        val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()
        val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()

        val matrix = Matrix()
        if (videoRatio != viewRatio) {
            if (viewRatio > videoRatio) {
                // Tampilan lebih lebar dari video, sesuaikan lebar video
                val scale = viewHeight.toFloat() / videoHeight.toFloat()
                matrix.setScale(scale, scale)
            } else {
                // Tampilan lebih tinggi dari video, sesuaikan tinggi video
                val scale = viewWidth.toFloat() / videoWidth.toFloat()
                matrix.setScale(scale, scale)
            }
        }
        // Atur posisi video ke tengah
        matrix.postTranslate(
            (viewWidth - videoWidth * matrix.mapRadius(1f)) / 2,
            (viewHeight - videoHeight * matrix.mapRadius(1f)) / 2
        )
        textureView.setTransform(matrix)
    }


    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        mediaPlayer?.let { if (it.isPlaying) { updateTextureViewSize(it.videoWidth, it.videoHeight) } }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null; return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    override fun onPause() {
        super.onPause()
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }
}
