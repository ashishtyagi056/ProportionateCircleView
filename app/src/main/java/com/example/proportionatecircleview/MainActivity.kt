package com.example.proportionatecircleview

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var circleView: CirclePercentageView

    private lateinit var seekA: SeekBar
    private lateinit var seekB: SeekBar
    private lateinit var seekC: SeekBar
    private lateinit var tvA: TextView
    private lateinit var tvB: TextView
    private lateinit var tvC: TextView

    // Flag to prevent recursive listener calls when we set progress programmatically
    private var isUpdating = false

    private var pA = 10
    private var pB = 50
    private var pC = 40

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        circleView = findViewById(R.id.circleView)
        seekA = findViewById(R.id.seekA)
        seekB = findViewById(R.id.seekB)
        seekC = findViewById(R.id.seekC)
        tvA   = findViewById(R.id.tvA)
        tvB   = findViewById(R.id.tvB)
        tvC   = findViewById(R.id.tvC)

        seekA.max = 100
        seekB.max = 100
        seekC.max = 100

        // Set initial positions
        seekA.progress = pA
        seekB.progress = pB
        seekC.progress = pC

        updateAll()

        // When A changes → split the remainder evenly between B and C
        seekA.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser || isUpdating) return
                pA = progress.coerceIn(0, 100)
                val remaining = 100 - pA
                // Keep B:C ratio, or split 50/50 if both are 0
                val total = pB + pC
                if (total == 0) {
                    pB = remaining / 2
                    pC = remaining - pB
                } else {
                    pB = (remaining * pB.toFloat() / total).toInt().coerceIn(0, remaining)
                    pC = remaining - pB
                }
                syncAndDraw()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // When B changes → adjust C to keep total = 100
        seekB.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser || isUpdating) return
                pB = progress.coerceIn(0, 100 - pA)
                pC = 100 - pA - pB
                syncAndDraw()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // When C changes → adjust B to keep total = 100
        seekC.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser || isUpdating) return
                pC = progress.coerceIn(0, 100 - pA)
                pB = 100 - pA - pC
                syncAndDraw()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
    }

    /** Push current pA/pB/pC back to the seekbars (without re-triggering listeners) and redraw. */
    private fun syncAndDraw() {
        isUpdating = true
        seekA.progress = pA
        seekB.progress = pB
        seekC.progress = pC
        isUpdating = false
        updateAll()
    }

    private fun updateAll() {
        tvA.text = "Circle A: $pA%"
        tvB.text = "Circle B: $pB%"
        tvC.text = "Circle C: $pC%"
        circleView.setPercentages(pA.toFloat(), pB.toFloat(), pC.toFloat())
    }
}