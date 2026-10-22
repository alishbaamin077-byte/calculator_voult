package com.example.calc

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val engine = CalculatorEngine()
    private lateinit var lcd: SevenSegmentView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lcd = findViewById(R.id.lcd)
        bindKeys(findViewById(R.id.keypad))
        render()
    }

    // Every key carries its action name in android:tag
    private fun bindKeys(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) bindKeys(view.getChildAt(i))
        } else {
            val key = view.tag as? String ?: return
            view.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                if (key == "=") {
                    val currentEntry = engine.displayText.removeSuffix(".")
                    val secretPin = VaultActivity.getPin(this)
                    if (currentEntry == secretPin) {
                        engine.press("c")
                        render()
                        val intent = android.content.Intent(this, VaultActivity::class.java)
                        startActivity(intent)
                        return@setOnClickListener
                    }
                }
                engine.press(key)
                render()
            }
        }
    }

    private fun render() {
        lcd.lit = engine.isOn
        lcd.memoryOn = engine.hasMemory
        lcd.text = engine.displayText
    }
}