package com.example.autoclick

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {

    private val names = listOf(
        "1 - Alodia", "2 - Anglo-Saxon", "3 - Anuradhapura",
        "4 - Ba Lan", "5 - Barcelona", "6 - Bột Hải",
        "7 - Bungari", "8 - Burgundy", "9 - Castile"
    )
    private val checks = ArrayList<CheckBox>()
    private lateinit var etRepeat: EditText
    private lateinit var etDelay: EditText
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        val saved = prefs.getString("positions", "1")!!
            .split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        root.addView(TextView(this).apply {
            text = "Auto Ủng Hộ Chủ Quyền"
            textSize = 22f
        })

        tvStatus = TextView(this).apply { setPadding(0, 24, 0, 24) }
        root.addView(tvStatus)

        root.addView(Button(this).apply {
            text = "1. Bật quyền Trợ năng (Accessibility)"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        root.addView(TextView(this).apply {
            text = "\n2. Chọn vị trí quốc gia cần nhấp:"
        })
        names.forEachIndexed { i, n ->
            val cb = CheckBox(this).apply {
                text = n
                isChecked = (i + 1) in saved
            }
            checks.add(cb)
            root.addView(cb)
        }

        root.addView(TextView(this).apply { text = "\nSố lần lặp mỗi vị trí:" })
        etRepeat = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("repeat", 1).toString())
        }
        root.addView(etRepeat)

        root.addView(TextView(this).apply { text = "Thời gian chờ giữa mỗi lần nhấp (ms):" })
        etDelay = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("delay", 1200).toString())
        }
        root.addView(etDelay)

        root.addView(Button(this).apply {
            text = "Lưu cài đặt"
            setOnClickListener {
                save()
                Toast.makeText(this@MainActivity, "Đã lưu", Toast.LENGTH_SHORT).show()
            }
        })

        root.addView(TextView(this).apply {
            setPadding(0, 32, 0, 0)
            text = "3. Mở game, vào màn hình danh sách quốc gia (cuộn lên đầu), " +
                    "rồi bấm nút nổi ▶ START ở mép trái màn hình. Bấm ■ STOP để dừng."
        })

        setContentView(ScrollView(this).apply {
            addView(root, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        })
    }

    private fun save() {
        val pos = checks.withIndex().filter { it.value.isChecked }
            .joinToString(",") { (it.index + 1).toString() }
        getSharedPreferences("cfg", MODE_PRIVATE).edit()
            .putString("positions", pos)
            .putInt("repeat", etRepeat.text.toString().toIntOrNull() ?: 1)
            .putInt("delay", etDelay.text.toString().toIntOrNull() ?: 1200)
            .apply()
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    override fun onResume() {
        super.onResume()
        tvStatus.text = if (ClickService.instance != null)
            "Trạng thái: ĐÃ BẬT trợ năng ✔ (nút nổi đang hiện)"
        else
            "Trạng thái: CHƯA bật trợ năng ✘"
    }
}
