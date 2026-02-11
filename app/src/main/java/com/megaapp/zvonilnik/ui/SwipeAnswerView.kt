package com.megaapp.zvonilnik.ui

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.megaapp.zvonilnik.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SwipeAnswerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val pill: View
    private val targetDecline: View
    private val targetAnswer: View
    private val handle: View
    private val hint: TextView

    var onAnswer: (() -> Unit)? = null
    var onDecline: (() -> Unit)? = null

    private var downX = 0f
    private var startTx = 0f
    private var centerTx = 0f
    private var minTx = 0f
    private var maxTx = 0f
    private var locked = false

    init {
        LayoutInflater.from(context).inflate(R.layout.view_swipe_answer, this, true)
        pill = findViewById(R.id.swipePill)
        targetDecline = findViewById(R.id.targetDecline)
        targetAnswer = findViewById(R.id.targetAnswer)
        handle = findViewById(R.id.handle)
        hint = findViewById(R.id.hint)

        post {
            // Двигаем handle по translationX внутри pill
            val available = pill.width - handle.width
            centerTx = 0f
            minTx = -available / 2f
            maxTx = available / 2f
            handle.translationX = centerTx
            updateVisuals(0f)
        }

        handle.setOnTouchListener { _, ev ->
            if (locked) return@setOnTouchListener true

            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX
                    startTx = handle.translationX
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - downX
                    val tx = (startTx + dx).coerceIn(minTx, maxTx)
                    handle.translationX = tx
                    updateVisuals(tx)
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val tx = handle.translationX
                    val threshold = (maxTx * 0.55f)

                    when {
                        tx >= threshold -> animateTo(maxTx) { fireAnswer() }
                        tx <= -threshold -> animateTo(minTx) { fireDecline() }
                        else -> animateTo(centerTx, reset = true) { }
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun updateVisuals(tx: Float) {
        val progress = if (maxTx == 0f) 0f else (tx / maxTx).coerceIn(-1f, 1f)

        // hint плавно исчезает при свайпе
        hint.alpha = 1f - min(1f, abs(progress) * 1.2f)

        // подсветка таргетов
        targetAnswer.alpha = 0.35f + max(0f, progress) * 0.65f
        targetDecline.alpha = 0.35f + max(0f, -progress) * 0.65f

        // чуть “выпячиваем” handle на свайпе
        val s = 1f + min(0.06f, abs(progress) * 0.06f)
        handle.scaleX = s
        handle.scaleY = s
    }

    private fun animateTo(targetTx: Float, reset: Boolean = false, end: () -> Unit) {
        locked = !reset
        val from = handle.translationX
        ValueAnimator.ofFloat(from, targetTx).apply {
            duration = 180
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val tx = it.animatedValue as Float
                handle.translationX = tx
                updateVisuals(tx)
            }
            doOnEnd {
                if (reset) locked = false
                end()
                if (!reset) {
                    // после срабатывания возвращаем handle в центр (чтобы повторный тест выглядел нормально)
                    postDelayed({
                        locked = false
                        animateTo(centerTx, reset = true) { }
                    }, 250)
                }
            }
            start()
        }
    }

    private fun ValueAnimator.doOnEnd(block: () -> Unit) {
        addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationCancel(animation: android.animation.Animator) { block() }
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
            override fun onAnimationEnd(animation: android.animation.Animator) { block() }
        })
    }

    private fun fireAnswer() {
        onAnswer?.invoke()
    }

    private fun fireDecline() {
        onDecline?.invoke()
    }
}
