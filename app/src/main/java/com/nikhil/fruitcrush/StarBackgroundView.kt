package com.nikhil.fruitcrush

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import java.util.Random

class StarBackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply { 
        color = Color.WHITE 
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val stars = ArrayList<Star>()
    private val random = Random()
    private var animator: ValueAnimator? = null

    private class Star(var x: Float, var y: Float, var radius: Float, var alpha: Int)

    init {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 50
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                updateStars()
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        stars.clear()
        val starCount = 200
        for (i in 0 until starCount) {
            stars.add(
                Star(
                    random.nextFloat() * w,
                    random.nextFloat() * h,
                    random.nextFloat() * 5f + 1f,
                    random.nextInt(255)
                )
            )
        }
    }

    private fun updateStars() {
        for (star in stars) {
            // Twinkle logic
            if (random.nextBoolean()) {
                val change = random.nextInt(30) - 15
                star.alpha += change
                if (star.alpha < 50) star.alpha = 50
                if (star.alpha > 255) star.alpha = 255
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        // Dark warm background (Very Dark Brown)
        canvas.drawColor(Color.parseColor("#3E2723")) 
        
        for (star in stars) {
            paint.alpha = star.alpha
            canvas.drawCircle(star.x, star.y, star.radius, paint)
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
