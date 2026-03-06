package com.example.proportionatecircleview

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.apply
import kotlin.collections.max
import kotlin.collections.min
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.ranges.coerceAtLeast
import kotlin.ranges.coerceIn

/**
 * A custom View that draws 3 adjacent (touching) circles whose sizes
 * are proportional to the given percentages (must sum to 100).
 *
 * Usage in XML:
 *   <com.example.circlelayout.CirclePercentageView
 *       android:id="@+id/circleView"
 *       android:layout_width="match_parent"
 *       android:layout_height="300dp" />
 *
 * Usage in code:
 *   circleView.setPercentages(10f, 50f, 40f)
 */
class CirclePercentageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Data ────────────────────────────────────────────────────────────────
    private var percentages = floatArrayOf(10f, 50f, 40f)
    private val labels      = arrayOf("A", "B", "C")

    // ── Colors ──────────────────────────────────────────────────────────────
    private val circleColors = intArrayOf(
        Color.parseColor("#FF6B35"),   // orange
        Color.parseColor("#1A1A2E"),   // dark navy
        Color.parseColor("#F7C948")    // yellow
    )
    private val lightColors = intArrayOf(
        Color.parseColor("#FF9A6C"),
        Color.parseColor("#2E2E4E"),
        Color.parseColor("#FFE080")
    )
    private val textColors = intArrayOf(
        Color.WHITE,
        Color.WHITE,
        Color.parseColor("#1A1A2E")
    )

    // ── Paints ───────────────────────────────────────────────────────────────
    private val circlePaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#30000000")
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
    }
    private val labelPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val subLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
        alpha = 180
    }

    // ── Computed geometry ────────────────────────────────────────────────────
    // Each circle's radius is proportional to sqrt(percentage) so that
    // AREA is proportional to the percentage.
    private val radii   = FloatArray(3)
    private val centerX = FloatArray(3)
    private val centerY = FloatArray(3)

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Set the three percentages.  They should sum to 100.
     */
    fun setPercentages(p1: Float, p2: Float, p3: Float) {
        require(p1 >= 0 && p2 >= 0 && p3 >= 0) { "All percentages must be >= 0" }
        require(Math.abs(p1 + p2 + p3 - 100f) < 0.5f) { "Percentages must sum to 100" }
        percentages = floatArrayOf(p1, p2, p3)
        // Recompute geometry immediately with the current view dimensions.
        // (onSizeChanged only fires when the view's pixel size changes, not on data updates.)
        if (width > 0 && height > 0) {
            computeGeometry(width.toFloat(), height.toFloat())
        }
        invalidate()
    }

    // ── Layout / measure ─────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeGeometry(w.toFloat(), h.toFloat())
    }

    /**
     * Compute radii and circle centers so all three circles are:
     *  • Sized by area ∝ percentage
     *  • Touching each other (adjacent, no gap, no overlap)
     *  • Centred vertically in the view
     *  • Scaled to fit within the available width & height
     */
    private fun computeGeometry(w: Float, h: Float) {
        val pad = 32f

        // Raw radii proportional to sqrt(percentage) → area ∝ percentage
        // Minimum of 0.08 ensures even a 0% circle stays visible
        val rawR = FloatArray(3) { sqrt(percentages[it] / 100f).coerceAtLeast(0.4f) }

        // Distance between each pair of centres when touching = r[i] + r[j]
        val d01 = rawR[0] + rawR[1]   // A–B
        val d12 = rawR[1] + rawR[2]   // B–C
        val d02 = rawR[0] + rawR[2]   // A–C  ← this makes all 3 mutually adjacent

        // Place A at origin, B to its right
        val ax = 0f;  val ay = 0f
        val bx = d01; val by = 0f

        // Find C using the intersection of two circles:
        //   |C - A| = d02,  |C - B| = d12
        // Angle at A in triangle A-B-C via cosine rule
        val cosA = (d01 * d01 + d02 * d02 - d12 * d12) / (2 * d01 * d02)
        val angA = acos(cosA.coerceIn(-1f, 1f))
        val cx0 = ax + d02 * cos(angA)
        val cy0 = ay + d02 * sin(angA)   // positive Y = downward in screen coords

        // Bounding box of the three circle edges
        val xs = floatArrayOf(
            ax - rawR[0], bx - rawR[1], cx0 - rawR[2],
            ax + rawR[0], bx + rawR[1], cx0 + rawR[2]
        )
        val ys = floatArrayOf(
            ay - rawR[0], by - rawR[1], cy0 - rawR[2],
            ay + rawR[0], by + rawR[1], cy0 + rawR[2]
        )
        val minX = xs.min(); val maxX = xs.max()
        val minY = ys.min(); val maxY = ys.max()
        val groupW = maxX - minX
        val groupH = maxY - minY

        // Scale to fit inside padded view
        val scale = min((w - 2 * pad) / groupW, (h - 2 * pad) / groupH)

        for (i in 0..2) radii[i] = rawR[i] * scale

        // Offset so the group is centred in the view
        val offX = (w - groupW * scale) / 2f - minX * scale
        val offY = (h - groupH * scale) / 2f - minY * scale

        val centres = floatArrayOf(ax, bx, cx0)
        val centresY = floatArrayOf(ay, by, cy0)
        for (i in 0..2) {
            centerX[i] = centres[i]  * scale + offX
            centerY[i] = centresY[i] * scale + offY
        }
    }

    // ── Draw ─────────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (radii[0] == 0f) return   // not laid out yet

        for (i in 0..2) {
            drawCircle(canvas, i)
        }
    }

    private fun drawCircle(canvas: Canvas, i: Int) {
        val r  = radii[i]
        val cx = centerX[i]
        val cy = centerY[i]

        // Shadow
        canvas.drawCircle(cx + 6f, cy + 10f, r, shadowPaint)

        // Gradient fill
        circlePaint.shader = RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.2f,
            lightColors[i], circleColors[i],
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, circlePaint)

        // Percentage label  (e.g. "50%")
        val pctText = "${percentages[i].toInt()}%"
        labelPaint.color    = textColors[i]
        labelPaint.textSize = r * 0.40f
        canvas.drawText(pctText, cx, cy + labelPaint.textSize * 0.35f, labelPaint)

        // Sub-label  (e.g. "Circle A")
        subLabelPaint.color    = textColors[i]
        subLabelPaint.textSize = r * 0.20f
        canvas.drawText(
            "Circle ${labels[i]}",
            cx,
            cy + r * 0.62f,
            subLabelPaint
        )
    }
}
