package com.example.chatdoctor

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatdoctor.sharepref.Constant
import com.example.chatdoctor.sharepref.PrefHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

class Login : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var prefHelper: PrefHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefHelper = PrefHelper(this)


        val textSignup = findViewById<TextView>(R.id.tvSignup)
        val backButton = findViewById<ImageView>(R.id.ivBackButton)

        textSignup.setOnClickListener {
            val intent = Intent(this, Signup::class.java)
            startActivity(intent)
            finish()
        }

        backButton.setOnClickListener {
            onBackPressed()
        }

        firebaseAuth = FirebaseAuth.getInstance()
        btnSignin.setOnClickListener {

            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            saveSession(etEmail.text.toString(), etPassword.text.toString())

            if (email.isNotEmpty() && password.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {

                        val intent = Intent(this, Chatboard::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Empty fields are not allowed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (prefHelper.getBoolean(Constant.PREF_IS_LOGIN)) {
            val intent = Intent(this, Chatboard::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun saveSession(etEmail: String, etPassword: String) {
        prefHelper.put(Constant.PREF_EMAIL, etEmail)
        prefHelper.put(Constant.PREF_PASSWORD, etPassword)
        prefHelper.put(Constant.PREF_IS_LOGIN, true)
    }
}