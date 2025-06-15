package com.example.monitorsuhu // GANTI DENGAN PACKAGE NAME ANDA

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs
import kotlin.math.hypot

class AddDeviceActivity : BaseActivity() {

    companion object {
        const val EXTRA_REVEAL_X = "REVEAL_X"
        const val EXTRA_REVEAL_Y = "REVEAL_Y"
        var lastPosition = 0
    }

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var deviceAdapter: DeviceAdapter
    private val deviceList = mutableListOf<DeviceModel>()

    private val handler = Handler(Looper.getMainLooper())
    private var scanningPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)

        rootLayout = findViewById(R.id.root_layout_add_device)
        viewPager = findViewById(R.id.view_pager_devices)

        if (savedInstanceState == null) {
            rootLayout.visibility = View.INVISIBLE
            rootLayout.post { revealActivity() }
        }

        setupViewPager()
        rootLayout.setOnClickListener { unRevealActivity() }
        viewPager.setOnClickListener(null)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { unRevealActivity() }
        })
    }

    private fun setupViewPager() {
        if (deviceList.isEmpty()) {
            deviceList.addAll(listOf(
                DeviceModel("B1", R.drawable.cooler_b1),
                DeviceModel("B1 Elite", R.drawable.cooler_b1_elite),
                DeviceModel("B1s", R.drawable.cooler_b1s),
                DeviceModel("G1 Elite", R.drawable.cooler_g1_elite)
            ))
        }

        deviceAdapter = DeviceAdapter(deviceList) { selectedDevice, position ->
            if (scanningPosition != -1) return@DeviceAdapter

            scanningPosition = position
            selectedDevice.isScanning = true
            deviceAdapter.notifyItemChanged(position)

            startScan(selectedDevice)
        }
        viewPager.adapter = deviceAdapter

        viewPager.offscreenPageLimit = 3
        val compositeTransformer = CompositePageTransformer()
        compositeTransformer.addTransformer(MarginPageTransformer(40))
        compositeTransformer.addTransformer { page, position ->
            val r = 1 - abs(position)
            page.scaleY = 0.85f + r * 0.15f
        }
        viewPager.setPageTransformer(compositeTransformer)

        val startPosition = if (lastPosition != 0) lastPosition else Int.MAX_VALUE / 2
        viewPager.setCurrentItem(startPosition, false)
    }

    private fun startScan(device: DeviceModel) {
        val timeoutRunnable = Runnable {
            if (scanningPosition != -1) {
                Toast.makeText(this, getString(R.string.toast_scan_timeout), Toast.LENGTH_LONG).show()
                resetScanningState()
            }
        }
        handler.postDelayed(timeoutRunnable, 15000)
    }

    private fun resetScanningState() {
        if (scanningPosition != -1) {
            val realIndex = scanningPosition % deviceList.size
            if(realIndex >= 0 && realIndex < deviceList.size) {
                deviceList[realIndex].isScanning = false
                deviceAdapter.notifyItemChanged(scanningPosition)
            }
            scanningPosition = -1
        }
    }

    private fun unRevealActivity() {
        if (scanningPosition != -1) {
            handler.removeCallbacksAndMessages(null)
            resetScanningState()
        }

        lastPosition = viewPager.currentItem
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

    private fun revealActivity() {
        val revealX = intent.getIntExtra(EXTRA_REVEAL_X, 0)
        val revealY = intent.getIntExtra(EXTRA_REVEAL_Y, 0)
        val finalRadius = hypot(rootLayout.width.toDouble(), rootLayout.height.toDouble()).toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(rootLayout, revealX, revealY, 0f, finalRadius)
        rootLayout.visibility = View.VISIBLE
        anim.duration = 400
        anim.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
