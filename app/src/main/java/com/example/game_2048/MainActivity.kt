package com.example.game_2048

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.game_2048.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        mDetector = GestureDetectorCompat(this, MyGestureListener())
        binding.buttonRestart.setOnClickListener {
            initBoard()
            dispBoart()
        }
    }

    override fun onWindowFocusChanged(hasFocus: boolean) {
        
    }
}