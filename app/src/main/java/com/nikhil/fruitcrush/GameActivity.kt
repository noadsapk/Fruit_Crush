package com.nikhil.fruitcrush

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.SoundPool
import android.os.*
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random
import com.nikhil.fruitcrush.R
import kotlin.math.cos
import kotlin.math.sin

class GameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
        
        val container = findViewById<FrameLayout>(R.id.game_container)
        container.addView(GameView(this))
    }

    data class Fruit(
        var x: Float,
        var y: Float, 
        val symbol: String, 
        val speed: Float,
        val isBomb: Boolean = false,
        var isBlast: Boolean = false,
        var blastStartTime: Long = 0,
        var blastColor: Int = Color.WHITE, // Color for the crush effect
        val blastSeed: Long = Random.nextLong() // Seed for consistent splash pattern
    )

    inner class GameView(val ctx: Context) : View(ctx) {

        private val fruits = CopyOnWriteArrayList<Fruit>()
        private val fruitSymbols = listOf("🍎", "🍌", "🍇", "🍊", "🍓", "🍍", "🥭", "🍉", "🍒", "🥝")
        
        // Map fruit symbols to their dominant color
        private val fruitColors = mapOf(
            "🍎" to Color.RED,
            "🍌" to Color.YELLOW,
            "🍇" to Color.rgb(128, 0, 128), // Purple
            "🍊" to Color.rgb(255, 165, 0), // Orange
            "🍓" to Color.RED,
            "🍍" to Color.YELLOW,
            "🥭" to Color.rgb(255, 165, 0), // Orange-ish
            "🍉" to Color.GREEN,
            "🍒" to Color.RED,
            "🥝" to Color.GREEN
        )

        private var score = 0
        private var miss = 0
        private var spawnTimer = 0L
        private val spawnInterval = 2000L // Spawn every 2 seconds
        private var lastTime = System.currentTimeMillis()
        private var startTime = System.currentTimeMillis()
        
        private var isGameEnded = false // Flag to prevent multiple EndActivity starts

        private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        private val textPaint = Paint().apply {
            textSize = 120f
            textAlign = Paint.Align.CENTER
        }
        private val scorePaint = Paint().apply {
            textSize = 60f
            color = Color.WHITE // White text for dark gradient background
        }
        
        // Paint for the crush effect (circle)
        private val crushPaint = Paint().apply {
            style = Paint.Style.FILL
        }

        // Bitmap for bomb blast
        private val blastBmp = getBitmapFromDrawable(ctx, R.drawable.blast_effect)?.scale(150, 150, true)

        private val soundPool = SoundPool.Builder().setMaxStreams(4).build()
        private val popSound = soundPool.load(ctx, R.raw.pop, 1) // Fruit crush/pop sound
        private val failSound = soundPool.load(ctx, R.raw.fail, 1) // Bomb blast or game over sound

        init {
            spawnTimer = System.currentTimeMillis()
        }

        private fun spawnFruits() {
            if (isGameEnded) return

            val w = if (width > 0) width else 1080
            val sectionWidth = w / 3f
            // Drop 3 at a time, spaced out horizontally
            for (i in 0 until 3) {
                val minX = i * sectionWidth + 50
                val maxX = (i + 1) * sectionWidth - 50
                val safeMaxX = if (maxX > minX) maxX else minX + 1f
                
                val isBomb = Random.nextFloat() < 0.2f // 20% chance of bomb
                val symbol = if (isBomb) "💣" else fruitSymbols.random()
                val color = if (isBomb) Color.DKGRAY else (fruitColors[symbol] ?: Color.WHITE)

                fruits.add(Fruit(
                    x = Random.nextFloat() * (safeMaxX - minX) + minX,
                    y = -150f - Random.nextFloat() * 200, // Start above screen
                    symbol = symbol,
                    speed = Random.nextFloat() * 15 + 10, // Random speed
                    isBomb = isBomb,
                    blastColor = color
                ))
            }
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            // Initial spawn once size is known
            if (fruits.isEmpty() && !isGameEnded) {
                spawnFruits()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (isGameEnded) return
            
            val now = System.currentTimeMillis()
            // Adjust speed for frame time roughly (simple time delta)
            val dt = (now - lastTime).coerceAtMost(100) // Cap dt to avoid huge jumps
            lastTime = now

            // Spawning
            if (now - spawnTimer > spawnInterval) {
                spawnFruits()
                spawnTimer = now
            }

            // Update and Draw Fruits
            for (fruit in fruits) {
                if (fruit.isBlast) {
                    if (fruit.isBomb) {
                         // Use the bitmap blast for bombs
                        blastBmp?.let {
                            canvas.drawBitmap(it, fruit.x - it.width / 2, fruit.y - it.height / 2, null)
                        }
                    } else {
                        // Realistic fruit crush effect (juice splash)
                        val elapsed = now - fruit.blastStartTime
                        val maxDuration = 400f
                        val progress = elapsed / maxDuration
                        
                        if (progress < 1f) {
                            // Use java.util.Random seeded with unique ID for consistent splash pattern
                            val rng = java.util.Random(fruit.blastSeed)
                            
                            crushPaint.color = fruit.blastColor
                            val alpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)
                            crushPaint.alpha = alpha
                            
                            // 1. Central expanding blob (juice core)
                            canvas.drawCircle(fruit.x, fruit.y - 40, 30f + 40f * progress, crushPaint)
                            
                            // 2. Droplets flying outwards
                            repeat(12) {
                                val angle = rng.nextFloat() * 2 * Math.PI
                                val speed = rng.nextFloat() * 150f + 50f
                                val distance = speed * progress
                                val size = (rng.nextFloat() * 12f + 4f) * (1f - progress)
                                
                                val dx = cos(angle) * distance
                                val dy = sin(angle) * distance
                                
                                canvas.drawCircle((fruit.x + dx).toFloat(), (fruit.y - 40 + dy).toFloat(), size.toFloat(), crushPaint)
                            }
                        }
                    }

                    // Remove blast after 400ms
                    if (now - fruit.blastStartTime > 400) {
                        fruits.remove(fruit)
                    }
                } else {
                    // Move fruit
                    fruit.y += fruit.speed * (dt / 16f) 

                    // Draw fruit
                    // Adjust y to draw from center (drawText draws from baseline)
                    canvas.drawText(fruit.symbol, fruit.x, fruit.y + 40, textPaint)

                    // Check miss
                    if (fruit.y > height + 100) {
                        if (!fruit.isBomb) { // Only miss if it's a regular fruit
                            miss++
                            soundPool.play(failSound, 1f, 1f, 1, 0, 1f)
                            if (miss >= 10) { // Game Over after 10 misses
                                endGame()
                                return // Stop processing this frame
                            }
                        }
                        fruits.remove(fruit)
                    }
                }
            }

            // UI
            canvas.drawText("Score: $score", 150f, 120f, scorePaint)
            canvas.drawText("Miss: $miss", 150f, 200f, scorePaint)

            if (!isGameEnded) {
                invalidate() // Continuous loop
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (isGameEnded) return true
            if (event.action == MotionEvent.ACTION_DOWN) {
                performClick()
                val ex = event.x
                val ey = event.y
                
                for (fruit in fruits) {
                    if (!fruit.isBlast) {
                        val dx = ex - fruit.x
                        val dy = ey - fruit.y
                        // Hit radius approx 80
                        if (dx * dx + dy * dy < 80 * 80) {
                            fruit.isBlast = true
                            fruit.blastStartTime = System.currentTimeMillis()
                            
                            if (fruit.isBomb) {
                                miss++
                                // Blast sound for bomb
                                soundPool.play(failSound, 1f, 1f, 1, 0, 1f)
                                // Vibrate for 500ms on bomb hit
                                if (Build.VERSION.SDK_INT >= 26) {
                                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(500)
                                }
                                
                                if (miss >= 10) {
                                    endGame()
                                    return true
                                }
                            } else {
                                score++
                                // Pop sound for fruit
                                soundPool.play(popSound, 1f, 1f, 1, 0, 1f)
                            }
                            break 
                        }
                    }
                }
            }
            return true
        }

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }

        private fun endGame() {
            if (isGameEnded) return
            isGameEnded = true
            
            soundPool.release()
            val i = Intent(ctx, EndActivity::class.java)
            i.putExtra("score", score)
            // Time logic changed, passing total played time approximation
            i.putExtra("time", System.currentTimeMillis() - startTime) 
            ctx.startActivity(i)
            if (ctx is GameActivity) ctx.finish()
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            soundPool.release()
        }

        private fun getBitmapFromDrawable(context: Context, drawableId: Int): Bitmap? {
            val drawable = AppCompatResources.getDrawable(context, drawableId) ?: return null
            val bitmap = createBitmap(
                if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 300,
                if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 300,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
    }
}
