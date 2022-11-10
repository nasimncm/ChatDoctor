package com.example.chatdoctor.activity

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.chatdoctor.R
import com.example.chatdoctor.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_profile.*
import java.util.Date

class Profile : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var selectedImg: Uri
    private lateinit var dialog: AlertDialog.Builder
    private lateinit var mProgress: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        //Back Button code
        ivBackButton.setOnClickListener {
            onBackPressed()
        }

        mProgress = ProgressDialog(this)
        mProgress.setTitle("Processing...")
        mProgress.setMessage("Please wait...")
        mProgress.setCancelable(false)
        mProgress.isIndeterminate

        //variable initialize
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()

        // when clicked on Image button
        profileImage.setOnClickListener {
            //open Gallery code
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

        // When clicked on Continue button code
        continueBtn.setOnClickListener {
            //name and image validation
            if (userName.text!!.isEmpty()){
                Toast.makeText(this, "Please enter name", Toast.LENGTH_SHORT).show()
            }else if (selectedImg == null){
                Toast.makeText(this, "Please select image", Toast.LENGTH_SHORT).show()
            }else uploadData()// Creating a function for upload the data in database
        }
    }

    private fun uploadData() {
        mProgress.show()
        //create a name in data base using reference variable and take a Data reference for storing data
        val reference = storage.reference.child("Profile").child(Date().time.toString())
        reference.putFile(selectedImg).addOnCompleteListener {
            //checking condition and make a image url
            if (it.isSuccessful){
                reference.downloadUrl.addOnSuccessListener { task ->
                    uploadInfo(task.toString()) //create upload info function for set the values on image and name
                }
            }
        }
    }

    private fun uploadInfo(imgUrl: String) {
        //take a variable reference for adding data in "user" data name
        val user = UserModel(uid = firebaseAuth.uid.toString(), name= userName.text.toString(), imageUrl = imgUrl)
        database.reference.child("user")
            .child(firebaseAuth.uid.toString())
            .setValue(user)
            .addOnSuccessListener {
                mProgress.dismiss()
                startActivity(Intent(this, Chatboard::class.java))
                finish()
            }
    }

    //set the selected image from gallery on current text view
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data!=null){
            if (data.data != null){
                selectedImg = data.data!!
                profileImage.setImageURI(selectedImg)
            }
        }
    }
}