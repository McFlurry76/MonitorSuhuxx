package com.example.monitorsuhu // PASTIKAN SESUAI DENGAN PACKAGE NAME ANDA

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.io.File
import java.io.IOException
import kotlin.math.hypot

class ControlActivity : BaseActivity() {

    // Kebutuhan untuk Animasi
    companion object {
        const val EXTRA_REVEAL_X = "REVEAL_X"
        const val EXTRA_REVEAL_Y = "REVEAL_Y"
    }
    private lateinit var rootLayout: ConstraintLayout

    // Kebutuhan untuk UI & Logika Suhu
    private lateinit var tvCpuTemp: TextView
    private lateinit var tvBatteryTemp: TextView
    private lateinit var lineChart: LineChart
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval: Long = 3000
    private var chartEntryCount = 0f
    private var latestBatteryTemp: Float? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempInCelsius = temperature / 10.0
            tvBatteryTemp.text = String.format("%.1f°C", tempInCelsius)
            if (tempInCelsius > 0) latestBatteryTemp = tempInCelsius.toFloat()
        }
    }

    private val updateDataRunnable = object : Runnable {
        override fun run() {
            getCpuTemperature()?.let {
                tvCpuTemp.text = String.format("%.1f°C", it)
                addEntryToChart(it, "cpu")
            } ?: run { tvCpuTemp.text = "--°C" }

            latestBatteryTemp?.let { addEntryToChart(it, "battery") }

            chartEntryCount++
            handler.postDelayed(this, updateInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        // Hubungkan UI untuk Animasi & Konten
        rootLayout = findViewById(R.id.root_layout_control)
        tvCpuTemp = findViewById(R.id.tv_cpu_temp)
        tvBatteryTemp = findViewById(R.id.tv_battery_temp)
        lineChart = findViewById(R.id.line_chart)

        // Logika untuk memulai animasi
        if (savedInstanceState == null) {
            rootLayout.visibility = View.INVISIBLE
            rootLayout.post { revealActivity() }
        }

        setupLineChart()
        setupOnBackPressed() // Panggil fungsi untuk tombol kembali
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        handler.post(updateDataRunnable)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
        handler.removeCallbacks(updateDataRunnable)
    }

    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                unRevealActivity() // Panggil animasi keluar
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

    // --- Semua fungsi untuk suhu & grafik tetap sama ---
    private fun getCpuTemperature(): Float? {
        val thermalPaths = listOf(
            "/sys/class/thermal/thermal_zone0/temp", "/sys/class/thermal/thermal_zone1/temp",
            "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp", "/sys/devices/virtual/thermal/thermal_zone0/temp"
        )
        for (path in thermalPaths) {
            try {
                val tempStr = File(path).readText().trim()
                val tempValue = tempStr.toFloatOrNull()
                if (tempValue != null) return if (tempValue > 1000) tempValue / 1000.0f else tempValue
            } catch (e: IOException) {}
        }
        return null
    }

    private fun setupLineChart() {
        lineChart.description.isEnabled = false
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setBackgroundColor(Color.TRANSPARENT)
        lineChart.data = LineData()
        lineChart.legend.isEnabled = true
        lineChart.xAxis.isEnabled = false
        lineChart.axisLeft.textColor = ContextCompat.getColor(this, R.color.frosty_blue_dark)
        lineChart.axisLeft.gridColor = Color.parseColor("#330D253F")
        lineChart.axisRight.isEnabled = false
    }

    private fun addEntryToChart(temperature: Float, type: String) {
        val data = lineChart.data ?: return
        var set: ILineDataSet? = data.getDataSetByLabel(type, true)
        if (set == null) {
            set = createSet(type)
            data.addDataSet(set)
        }
        data.addEntry(Entry(chartEntryCount, temperature), data.getIndexOfDataSet(set))
        if (set.entryCount > 300) set.removeFirst()
        data.notifyDataChanged()
        lineChart.notifyDataSetChanged()
        lineChart.moveViewToX(data.entryCount.toFloat())
    }

    private fun createSet(type: String): LineDataSet {
        val set = LineDataSet(null, type)
        set.lineWidth = 2.5f
        set.setDrawValues(false)
        set.setDrawCircles(false)
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.isHighlightEnabled = false
        set.color = if (type == "cpu") ContextCompat.getColor(this, R.color.frosty_accent_red) else ContextCompat.getColor(this, R.color.frosty_blue_dark)
        return set
    }
}
