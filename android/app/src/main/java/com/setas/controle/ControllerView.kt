package com.setas.controle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

/** Tela de controle: bloco de setas + analógico. Com o cadeado aberto, move e redimensiona. */
@SuppressLint("ViewConstructor")
class ControllerView(context: Context, private val store: LayoutStore) : View(context) {

    var onMask: ((Int) -> Unit)? = null
    var hapticsOn = true

    var locked = true
        set(value) {
            if (field == value) return
            field = value
            drag = null
            if (!value) releaseAll() else store.save(landscape, layout)
            invalidate()
        }

    private val dp = resources.displayMetrics.density
    private val minU = 48 * dp
    private val minStick = 120 * dp
    private val handle = 24 * dp

    private val touch = TouchState()
    private var layout = Layout(BlockGeometry(0f, 0f, 1f), StickGeometry(0f, 0f, 1f))
    private var landscape = true
    private var sized = false

    private var stickPointer = -1
    private var knobDx = 0f
    private var knobDy = 0f

    private enum class DragMode { MOVE_BLOCK, RESIZE_BLOCK, MOVE_STICK, RESIZE_STICK }
    private class Drag(val mode: DragMode, val startX: Float, val startY: Float, val start: Layout)
    private var drag: Drag? = null

    private val keyStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2 * dp; color = Color.rgb(170, 170, 170) }
    private val keyFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(40, 40, 40) }
    private val keyOn = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 136, 229) }
    private val dashed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1.5f * dp; color = Color.rgb(110, 110, 110)
        pathEffect = DashPathEffect(floatArrayOf(6 * dp, 6 * dp), 0f)
    }
    private val editOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2 * dp; color = Color.rgb(30, 136, 229)
        pathEffect = DashPathEffect(floatArrayOf(8 * dp, 6 * dp), 0f)
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 136, 229) }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(90, 90, 90) }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER }
    private val hint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(130, 130, 130); textAlign = Paint.Align.CENTER }

    init {
        setBackgroundColor(Color.rgb(18, 18, 18))
        isHapticFeedbackEnabled = true
    }

    /** Solta tudo e avisa o PC na hora. */
    fun releaseAll() {
        touch.releaseAll()
        stickPointer = -1
        knobDx = 0f
        knobDy = 0f
        onMask?.invoke(0)
        invalidate()
    }

    fun resetLayout() {
        store.clear(landscape)
        layout = LayoutModel.default(width.toFloat(), height.toFloat())
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        val newLandscape = w > h
        if (!sized || newLandscape != landscape || w != oldw || h != oldh) {
            if (sized && !locked) store.save(landscape, layout)
            releaseAll()
            drag = null
            landscape = newLandscape
            sized = true
            val saved = store.load(landscape)
            layout = if (saved != null) LayoutModel.clamp(saved, w.toFloat(), h.toFloat(), minU, minStick)
            else LayoutModel.default(w.toFloat(), h.toFloat())
        }
    }

    // ---------- desenho ----------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!sized) return
        val b = layout.block
        val mask = touch.mask
        val r = 10 * dp

        for (c in b.cornerRects()) canvas.drawRoundRect(c, r, r, dashed)
        hint.textSize = b.u * 0.2f
        val corners = b.cornerRects()
        drawLabel(canvas, corners[0], "↖", hint)
        drawLabel(canvas, corners[1], "↗", hint)

        text.textSize = b.u * 0.35f
        for ((arrow, label) in listOf(Arrows.UP to "↑", Arrows.LEFT to "←", Arrows.DOWN to "↓", Arrows.RIGHT to "→")) {
            val rect = b.keyRect(arrow)
            canvas.drawRoundRect(rect, r, r, if (mask and arrow != 0) keyOn else keyFill)
            canvas.drawRoundRect(rect, r, r, keyStroke)
            drawLabel(canvas, rect, label, text)
        }

        val s = layout.stick
        canvas.drawCircle(s.centerX, s.centerY, s.size / 2, keyStroke)
        canvas.drawCircle(s.centerX, s.centerY, s.deadZone, dashed)
        canvas.drawCircle(s.centerX + knobDx, s.centerY + knobDy, s.size * 0.2f, knobPaint)
        canvas.drawCircle(s.centerX + knobDx, s.centerY + knobDy, s.size * 0.2f, keyStroke)

        if (!locked) {
            val pad = 6 * dp
            canvas.drawRoundRect(RectF(b.x - pad, b.y - pad, b.x + b.width + pad, b.y + b.height + pad), r, r, editOutline)
            canvas.drawRoundRect(RectF(s.x - pad, s.y - pad, s.x + s.size + pad, s.y + s.size + pad), r, r, editOutline)
            canvas.drawRoundRect(handleRect(b.x + b.width, b.y + b.height), 4 * dp, 4 * dp, handlePaint)
            canvas.drawRoundRect(handleRect(s.x + s.size, s.y + s.size), 4 * dp, 4 * dp, handlePaint)
        }
    }

    private fun drawLabel(canvas: Canvas, rect: RectF, label: String, paint: Paint) {
        val y = rect.centerY() - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(label, rect.centerX(), y, paint)
    }

    private fun handleRect(cx: Float, cy: Float) = RectF(cx - handle / 2, cy - handle / 2, cx + handle / 2, cy + handle / 2)

    // ---------- toque ----------

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!sized) return true
        if (locked) playTouch(e) else editTouch(e)
        invalidate()
        return true
    }

    private fun playTouch(e: MotionEvent) {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                val id = e.getPointerId(i)
                val x = e.getX(i)
                val y = e.getY(i)
                if (stickPointer == -1 && layout.stick.contains(x, y)) {
                    stickPointer = id
                    updateStick(x, y)
                } else {
                    touch.setPointer(id, layout.block.hit(x, y))
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val id = e.getPointerId(i)
                    if (id == stickPointer) updateStick(e.getX(i), e.getY(i))
                    else touch.setPointer(id, layout.block.hit(e.getX(i), e.getY(i)))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val id = e.getPointerId(e.actionIndex)
                if (id == stickPointer) {
                    stickPointer = -1
                    knobDx = 0f
                    knobDy = 0f
                    touch.setStick(0)
                } else {
                    touch.removePointer(id)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                releaseAll()
                return
            }
        }
        publish()
    }

    private fun updateStick(x: Float, y: Float) {
        val s = layout.stick
        var dx = x - s.centerX
        var dy = y - s.centerY
        touch.setStick(StickMapper.map(dx, dy, s.size))
        val d = hypot(dx, dy)
        if (d > s.maxTravel) {
            dx = dx / d * s.maxTravel
            dy = dy / d * s.maxTravel
        }
        knobDx = dx
        knobDy = dy
    }

    /** Envia ao PC primeiro; só depois vibra, para a vibração nunca atrasar a tecla. */
    private fun publish() {
        onMask?.invoke(touch.mask)
        val added = touch.takeNewArrowBits()
        if (hapticsOn && added != 0) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun editTouch(e: MotionEvent) {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = e.x
                val y = e.y
                val b = layout.block
                val s = layout.stick
                val grab = handle * 0.9f
                val mode = when {
                    hypot(x - (b.x + b.width), y - (b.y + b.height)) <= grab -> DragMode.RESIZE_BLOCK
                    hypot(x - (s.x + s.size), y - (s.y + s.size)) <= grab -> DragMode.RESIZE_STICK
                    x >= b.x && x <= b.x + b.width && y >= b.y && y <= b.y + b.height -> DragMode.MOVE_BLOCK
                    s.contains(x, y) -> DragMode.MOVE_STICK
                    else -> null
                }
                drag = mode?.let { Drag(it, x, y, layout) }
            }
            MotionEvent.ACTION_MOVE -> {
                val d = drag ?: return
                val dx = e.x - d.startX
                val dy = e.y - d.startY
                val moved = when (d.mode) {
                    DragMode.MOVE_BLOCK -> LayoutModel.moveBlock(d.start, dx, dy)
                    DragMode.RESIZE_BLOCK -> LayoutModel.resizeBlock(d.start, dx, dy)
                    DragMode.MOVE_STICK -> LayoutModel.moveStick(d.start, dx, dy)
                    DragMode.RESIZE_STICK -> LayoutModel.resizeStick(d.start, dx, dy)
                }
                layout = LayoutModel.clamp(moved, width.toFloat(), height.toFloat(), minU, minStick)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> drag = null
        }
    }
}
