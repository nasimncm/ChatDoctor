package com.example.chatdoctor.login

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatdoctor.R
import com.example.chatdoctor.activity.Profile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_signup.*

class Signup : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
    private lateinit var mProgress: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        mProgress = ProgressDialog(this)
        mProgress.setTitle("Processing...")
        mProgress.setMessage("Please wait...")
        mProgress.setCancelable(false)
        mProgress.isIndeterminate
        //sign in button clickonlistner
        val textSignIn = findViewById<TextView>(R.id.tvSignIn)
        textSignIn.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        //back button implimentation
        val btnBack = findViewById<ImageView>(R.id.ivBack)
        btnBack.setOnClickListener {
            onBackPressed()
        }

        //firebase auth initilization
        firebaseAuth = FirebaseAuth.getInstance()

        //signup implimentation
        val signupButton = findViewById<Button>(R.id.btnSignup)
        signupButton.setOnClickListener {
            mProgress.show()
            //define the email,password,confpass, variable
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()
            //  val name = etName.text.toString()

            //null check
            if (email.matches(emailPattern.toRegex()) && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {

                    //create user with email and password
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                mProgress.dismiss()
                                //if successful redirect to Profile creation
                                val intent = Intent(this, Profile::class.java)
                                startActivity(intent)
                                finish()
                            } else {

                                //if not success show the error messages
                                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT)
                                    .show()
                                mProgress.dismiss()
                            }
                        }

                } else {
                    //if password not match
                    Toast.makeText(this, "Password does not match", Toast.LENGTH_SHORT).show()
                    mProgress.dismiss()
                }
            } else {
                //if email & password empty
                Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show()
                mProgress.dismiss()
            }
        }
    }
}