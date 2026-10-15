package com.example.autoclick

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.Toast

class ClickService : AccessibilityService() {

    companion object {
        var instance: ClickService? = null

        // Tọa độ gốc theo màn hình 1080x2340 (tự scale theo máy)
        const val REF_W = 1080f
        const val REF_H = 2340f

        val COUNTRY_Y = listOf(1271, 1379, 1487, 1595, 1703, 1811, 1919, 2027, 2135)
        const val COUNTRY_X = 881

        val TAB_GIFT = Pair(755, 2280)
        val BTN_SOVEREIGN = Pair(144, 1935)
        val BTN_CONFIRM = Pair(762, 1486)
        val BTN_CLOSE_MSG = Pair(540, 1494)
        val BTN_CLOSE_PANEL = Pair(1010, 138)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var floatBtn: Button? = null
    private var queue: List<Pair<Int, Int>> = emptyList()
    private var delayMs = 1200L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        showFloatingButton()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        stopRun()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        stopRun()
        removeFloatingButton()
        instance = null
        return super.onUnbind(intent)
    }

    // ---------- Nút nổi ----------
    private fun showFloatingButton() {
        if (floatBtn != null) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val btn = Button(this).apply {
            text = "▶ START"
            setOnClickListener { if (running) stopRun() else startRun() }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 600 // vị trí không đè lên các nút cần nhấp
        }
        wm.addView(btn, lp)
        floatBtn = btn
    }

    private fun removeFloatingButton() {
        floatBtn?.let {
            try {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
            } catch (_: Exception) {
            }
        }
        floatBtn = null
    }

    // ---------- Điều khiển chạy ----------
    private fun startRun() {
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        val positions = prefs.getString("positions", "1")!!
            .split(",").mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..9 }
        val repeat = prefs.getInt("repeat", 1).coerceAtLeast(1)
        delayMs = prefs.getInt("delay", 1200).toLong().coerceAtLeast(200)

        if (positions.isEmpty()) {
            Toast.makeText(this, "Chưa chọn vị trí nào trong app", Toast.LENGTH_SHORT).show()
            return
        }

        val q = ArrayList<Pair<Int, Int>>()
        for (pos in positions) {
            repeat(repeat) {
                q.add(Pair(COUNTRY_X, COUNTRY_Y[pos - 1]))
                q.add(TAB_GIFT)
                q.add(BTN_SOVEREIGN)
                q.add(BTN_CONFIRM)
                q.add(BTN_CLOSE_MSG)
                q.add(BTN_CLOSE_PANEL)
            }
        }
        queue = q
        running = true
        floatBtn?.text = "■ STOP"
        handler.postDelayed({ runStep(0) }, 800)
    }

    private fun stopRun() {
        running = false
        handler.removeCallbacksAndMessages(null)
        floatBtn?.text = "▶ START"
    }

    private fun runStep(i: Int) {
        if (!running) return
        if (i >= queue.size) {
            stopRun()
            Toast.makeText(this, "Xong!", Toast.LENGTH_SHORT).show()
            return
        }
        val (rx, ry) = queue[i]
        val (x, y) = scale(rx, ry)
        tap(x, y) {
            handler.postDelayed({ runStep(i + 1) }, delayMs)
        }
    }

    // ---------- Nhấp ----------
    @Suppress("DEPRECATION")
    private fun scale(rx: Int, ry: Int): Pair<Float, Float> {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(dm)
        return Pair(rx * dm.widthPixels / REF_W, ry * dm.heightPixels / REF_H)
    }

    private fun tap(x: Float, y: Float, done: () -> Unit) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
            .build()
        val ok = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) = done()
            override fun onCancelled(g: GestureDescription?) = done()
        }, null)
        if (!ok) done()
    }
}
