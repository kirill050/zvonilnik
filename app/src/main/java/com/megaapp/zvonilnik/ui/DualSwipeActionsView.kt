package com.megaapp.zvonilnik.ui

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.megaapp.zvonilnik.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class DualSwipeActionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val declineHandle: View
    private val answerHandle: View
    private val hint: View

    var onAnswer: (() -> Unit)? = null
    var onDecline: (() -> Unit)? = null

    private val maxUpPx = dp(72f)        // насколько можно тянуть вверх
    private val triggerUpPx = dp(52f)    // порог срабатывания

    init {
        LayoutInflater.from(context).inflate(R.layout.view_dual_swipe_actions, this, true)
        declineHandle = findViewById(R.id.handleDecline)
        answerHandle = findViewById(R.id.handleAnswer)
        hint = findViewById(R.id.tvSwipeHint)

        bindSwipe(declineHandle) { onDecline?.invoke() }
        bindSwipe(answerHandle) { onAnswer?.invoke() }
    }

    private fun bindSwipe(handle: View, fire: () -> Unit) {
        var downY = 0f
        var startTy = 0f
        var locked = false

        handle.setOnTouchListener { v, ev ->
            if (locked) return@setOnTouchListener true

            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downY = ev.rawY
                    startTy = v.translationY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = ev.rawY - downY
                    // тянем только вверх => translationY отрицательный
                    val ty = (startTy + dy).coerceIn(-maxUpPx, 0f)
                    v.translationY = ty

                    // подсказку слегка прячем при движении
                    val p = min(1f, abs(ty) / maxUpPx)
                    hint.alpha = 1f - p * 0.7f
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val ty = v.translationY
                    if (ty <= -triggerUpPx) {
                        locked = true
                        animateTo(v, -maxUpPx) {
                            fire()
                            // вернуть обратно (на случай быстрого повторного теста)
                            postDelayed({
                                animateTo(v, 0f) { locked = false }
                                hint.alpha = 1f
                            }, 180)
                        }
                    } else {
                        animateTo(v, 0f) { hint.alpha = 1f }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun animateTo(v: View, target: Float, end: () -> Unit) {
        val from = v.translationY
        ValueAnimator.ofFloat(from, target).apply {
            duration = 170
            interpolator = DecelerateInterpolator()
            addUpdateListener { v.translationY = it.animatedValue as Float }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
                override fun onAnimationCancel(animation: android.animation.Animator) { end() }
                override fun onAnimationEnd(animation: android.animation.Animator) { end() }
            })
            start()
        }
    }

    private fun dp(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)
}
