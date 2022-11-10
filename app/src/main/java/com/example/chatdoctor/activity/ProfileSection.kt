package com.example.chatdoctor.activity

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.chatdoctor.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_profile_section.*

class ProfileSection : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_section)

        ivBackButton.setOnClickListener {
            onBackPressed()
        }

        val profileName = findViewById<TextView>(R.id.profileName)
        val ivProfile = findViewById<ImageView>(R.id.ivProfile)
        val profileStatus = findViewById<TextView>(R.id.profileStatus)

        //make a variable name of presentUserId and call the FirebaseAuth with getInstance currentuser with uid
        val presentUserId = FirebaseAuth.getInstance().currentUser?.uid

        //call firebasedatabase and get the refrance of created data base name "user" and find the urrent user with uid
        FirebaseDatabase.getInstance().getReference("user").child(presentUserId.toString())

            //add successes listner and get the image and name
            .get().addOnSuccessListener {
                Glide.with(this).load(it.child("imageUrl").value).into(ivProfile)
                profileName.text = it.child("name").value.toString()
            }
    }
}