package com.setas.controle

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val link = PhoneLink()
    private lateinit var controller: ControllerView
    private lateinit var status: TextView
    private lateinit var lockButton: Button
    private lateinit var resetButton: Button
    private lateinit var hapticButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val prefs = getSharedPreferences("setas", MODE_PRIVATE)
        controller = ControllerView(this, LayoutStore(prefs))
        controller.hapticsOn = prefs.getBoolean("haptics", true)
        controller.onMask = { link.send(it) }

        val dp = resources.displayMetrics.density
        fun button(label: String, onClick: () -> Unit) = Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 13f
            minHeight = 0
            minimumHeight = 0
            setPadding((10 * dp).toInt(), (4 * dp).toInt(), (10 * dp).toInt(), (4 * dp).toInt())
            setOnClickListener { onClick() }
        }

        status = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.rgb(239, 154, 154))
            text = "Desconectado"
        }
        hapticButton = button("") {
            controller.hapticsOn = !controller.hapticsOn
            prefs.edit().putBoolean("haptics", controller.hapticsOn).apply()
            refreshButtons()
        }
        resetButton = button("Voltar ao padrão") { controller.resetLayout() }
        lockButton = button("") {
            controller.locked = !controller.locked
            refreshButtons()
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * dp).toInt(), (6 * dp).toInt(), (12 * dp).toInt(), 0)
            addView(status, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(resetButton)
            addView(hapticButton)
            addView(lockButton)
        }

        setContentView(FrameLayout(this).apply {
            addView(controller, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(bar, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.TOP))
        })
        refreshButtons()

        link.onConnectionChanged = { connected ->
            runOnUiThread {
                status.text = if (connected) "Conectado ao PC" else "Desconectado"
                status.setTextColor(if (connected) Color.rgb(129, 199, 132) else Color.rgb(239, 154, 154))
            }
        }
    }

    private fun refreshButtons() {
        lockButton.text = if (controller.locked) "🔒 Travado" else "🔓 Ajustando"
        hapticButton.text = if (controller.hapticsOn) "Vibração: ligada" else "Vibração: desligada"
        resetButton.visibility = if (controller.locked) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        link.start()
    }

    override fun onPause() {
        controller.releaseAll()
        link.stop()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars() else controller.releaseAll()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }
}
