package com.example.chatdoctor

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.chatdoctor.activity.Chatboard
import com.example.chatdoctor.login.MainActivity
import com.google.firebase.auth.FirebaseAuth

class Splash : AppCompatActivity() {
    private var firebaseAuth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val tvChatDoctor = findViewById<TextView>(R.id.tvChatDoctor)
        val slideAnimation = AnimationUtils.loadAnimation(this, R.anim.side_slide)
        tvChatDoctor.startAnimation(slideAnimation)

        Handler(Looper.getMainLooper()).postDelayed({
            if (checkLoginStatus()) {
                val intent = Intent(this, Chatboard::class.java)
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, 5000)
    }

    private fun checkLoginStatus(): Boolean {
        return firebaseAuth.currentUser?.uid?.isNotEmpty() == true
    }
}