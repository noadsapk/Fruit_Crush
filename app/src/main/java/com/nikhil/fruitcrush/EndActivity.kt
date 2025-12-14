package com.nikhil.fruitcrush

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Context
import com.nikhil.fruitcrush.databinding.ActivityEndBinding

class EndActivity : AppCompatActivity() {
    lateinit var b: ActivityEndBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityEndBinding.inflate(layoutInflater)
        setContentView(b.root)

        val score = intent.getIntExtra("score", 0)
        b.scoreText.text = "Score: $score"
        
        val prefs = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        var highScore = prefs.getInt("high_score", 0)
        
        if (score > highScore) {
            highScore = score
            prefs.edit().putInt("high_score", highScore).apply()
        }
        
        b.highScoreText.text = "High Score: $highScore"
        b.timeText.text = "Time Speed: " + intent.getLongExtra("time",0)/1000 + " sec"
        b.restartBtn.setOnClickListener { finish() }
    }
}
