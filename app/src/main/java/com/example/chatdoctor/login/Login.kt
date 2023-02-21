package com.example.chatdoctor.login

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatdoctor.R
import com.example.chatdoctor.activity.Chatboard
import com.example.chatdoctor.sharepref.Constant
import com.example.chatdoctor.sharepref.PrefHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

class Login : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var prefHelper: PrefHelper
    private val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
    private lateinit var  mProgress: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //sharedpref implimentation
        prefHelper = PrefHelper(this)

        mProgress = ProgressDialog(this)
        mProgress.setTitle("Processing...")
        mProgress.setMessage("Please wait...")
        mProgress.setCancelable(false)
        mProgress.isIndeterminate
        //initializ the text view
        val textSignup = findViewById<TextView>(R.id.tvSignup)
        val backButton = findViewById<ImageView>(R.id.ivBackButton)

        //clicklistioner
        textSignup.setOnClickListener {
            val intent = Intent(this, Signup::class.java)
            startActivity(intent)
            finish()
        }

        //clicklistioner
        backButton.setOnClickListener {
            onBackPressed()
        }

        //initialize firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        //sign in imlimentation
        btnSignin.setOnClickListener {

            mProgress.show()

            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            //for sharedpref save the session
            saveSession(etEmail.text.toString(), etPassword.text.toString())

            //check email validation
            if (email.matches(emailPattern.toRegex()) && password.isNotEmpty()) {

                //login with email & password
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {

                        mProgress.dismiss()

                        //if success then redirect to chat list activity
                        val intent = Intent(this, Chatboard::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        //if not success show the error messages
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                        mProgress.dismiss()
                    }
                }
            } else {
                //if email & password empty
                Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show()
                mProgress.dismiss()
            }
        }
    }

    //check user login or not in login page
    override fun onStart() {
        super.onStart()
        if (prefHelper.getBoolean(Constant.PREF_IS_LOGIN)) {
            val intent = Intent(this, Chatboard::class.java)
            startActivity(intent)
            finish()
        }
    }

    //session save for sharepref
    private fun saveSession(etEmail: String, etPassword: String) {
        prefHelper.put(Constant.PREF_EMAIL, etEmail)
        prefHelper.put(Constant.PREF_PASSWORD, etPassword)
        prefHelper.put(Constant.PREF_IS_LOGIN, true)
    }
}