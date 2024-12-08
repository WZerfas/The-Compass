package com.cs407.the_compass

import android.content.Intent
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    private lateinit var arrow: ImageView
    private lateinit var rotateAnimation: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Initialize the arrow view
        arrow = findViewById(R.id.arrow)

        // Load the rotation animation
        rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_arrow)

        // Set AnimationListener to transition after animation ends
        rotateAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                // Transition to MainActivity after animation ends
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                // Apply the fade transition
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                finish()
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })

        // Start the rotation animation
        arrow.startAnimation(rotateAnimation)
    }
}
