package com.example.chatdoctor.activity

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.icu.util.Calendar
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatdoctor.R
import com.example.chatdoctor.adapter.MessageAdapter
import com.example.chatdoctor.databinding.ActivityChatroomBinding
import com.example.chatdoctor.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_chatroom.*
import kotlinx.android.synthetic.main.sent.*
import java.io.ByteArrayOutputStream
import java.util.*


class Chatroom : AppCompatActivity() {
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var databaseAuth: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var profileImage: String? = null
    private lateinit var senderUid: String
    private lateinit var receiverUid: String
    private lateinit var binding: ActivityChatroomBinding
    private lateinit var dialog: ProgressDialog
    private lateinit var date: Date
    private var isattachment = false
    var receiverRoom: String? = null
    var senderRoom: String? = null
    private val imageRequestCode = 1888
    private val cameraRequestCode = 1
    private val camera = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityChatroomBinding = ActivityChatroomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //database, firebase storage, senderUid, date initialize
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        senderUid = FirebaseDatabase.getInstance().toString()
        date = Date()

        //dialog setup when data not load
        dialog = ProgressDialog(this)
        dialog.setMessage("Uploading Image...")
        dialog.setCancelable(false)

        //get name code
        val tvName = findViewById<TextView>(R.id.tvName)
        val chatProfileImage = findViewById<ImageView>(R.id.chatProfileImage)
        tvName.text = intent.getStringExtra("name")
        profileImage = intent.getStringExtra("imageUrl")
        Glide.with(this).load(profileImage).into(chatProfileImage)
        //    Log.d( "onCreate: ", profileImage.toString())

        //set back button
        ivBackButton.setOnClickListener {
            onBackPressed()
        }

        // code for online, offline & typing Status show
        //taking reference of receiver and sender UID
        val receiverUid = intent.getStringExtra("uid")
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        //database initialize
        databaseAuth = FirebaseDatabase.getInstance().reference
        // create presence folder in database and add in receiverUID
        database.reference.child("presence").child(receiverUid!!)
            .addValueEventListener(object : ValueEventListener { //create object showing status
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) { //read the data through snapshot.
                        val tvStatus = snapshot.getValue(String::class.java)
                        if (tvStatus == "Offline") {
                            //set the offline status
                            binding.tvStatus.visibility = View.VISIBLE
                        } else {
                            binding.tvStatus.text = tvStatus
                            binding.tvStatus.visibility = View.VISIBLE
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        databaseAuth.child("chats").child(senderRoom!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    messageList.clear()
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(Message::class.java)
                        message!!.messageId = postSnapshot.key
                        messageList.add(message)
                    }
                    messageAdapter.notifyDataSetChanged()
                    chatRecyclerView.scrollToPosition(messageList.size - 1)

                }

                override fun onCancelled(error: DatabaseError) {}

            })

        val mediaPlayer = MediaPlayer()
        fun sentPlayTone() {
            try {
                val afd = applicationContext.assets.openFd("sent.mp3")
                mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                mediaPlayer.prepare()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            mediaPlayer.start()
        }

        fun receivedTone() {
            try {
                val afd = applicationContext.assets.openFd("tone.mp3")
                mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                mediaPlayer.prepare()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            mediaPlayer.start()
        }

        msgSend.setOnClickListener {
            // val chatMessageId = UUID.randomUUID().toString()
            val myMessage = etMessage.text.toString()
            if (myMessage.isNotEmpty()) {
                val messageObject = Message(myMessage, senderUid)
                sentPlayTone()
                databaseAuth.child("chats").child(senderRoom!!).child("messages").push()
                    .setValue(messageObject).addOnSuccessListener {
                        databaseAuth.child("chats").child(receiverRoom!!).child("messages").push()
                            .setValue(messageObject)
                        receivedTone()
                    }
                etMessage.setText("")

            } else {
                Toast.makeText(this, "Write some message in the box", Toast.LENGTH_SHORT).show()
            }
        }


        val handler = Handler()
        etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                database.reference.child("presence").child(senderUid!!).setValue("typing...")
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed(userStoppedTyping, 1000)
            }

            val userStoppedTyping = Runnable {
                database.reference.child("presence").child(senderUid!!).setValue("Online")
            }
        })
        //view the data

        chatRecyclerView = findViewById(R.id.rvChat)
        messageBox = findViewById(R.id.etMessage)
        sendButton = findViewById(R.id.msgSend)
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList, senderRoom!!, receiverRoom!!)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)

        chatRecyclerView.adapter = messageAdapter

        messageAdapter.notifyDataSetChanged()

        attachment.setOnClickListener {
            if (!isattachment) {
                isattachment = true
                cardView1.visibility = View.VISIBLE
            } else {
                isattachment = false
                cardView1.visibility = View.GONE
            }

        }


        cameraIcon.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, cameraRequestCode)
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.CAMERA), cameraRequestCode
                )
            }
        }

        galleryIcon.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent, imageRequestCode)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, camera)
            } else {
                Toast.makeText(
                    this,
                    "Oops you just denied the camera permission. " + "Don't worry you can allow in the settings",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val mediaPlayer = MediaPlayer()
    fun playTone() {
        try {
            val afd = applicationContext.assets.openFd("sent.mp3")
            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mediaPlayer.prepare()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        mediaPlayer.start()
    }

    fun receivedPlayTone() {
        try {
            val afd = applicationContext.assets.openFd("tone.mp3")
            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mediaPlayer.prepare()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        mediaPlayer.start()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("onActivityResult: ", requestCode.toString())
        if (requestCode == cameraRequestCode) {
            val clickImage = data?.extras?.get("data") as Bitmap
            val stream = ByteArrayOutputStream()
            clickImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val data = stream.toByteArray()
            val calendar = Calendar.getInstance()
            dialog.show()
            val cameraReference =
                storage.reference.child("chats").child(calendar.timeInMillis.toString() + "")
            cameraReference.putBytes(data).addOnCompleteListener { task ->
                dialog.dismiss()
                playTone()
                if (task.isSuccessful) {
                    cameraReference.downloadUrl.addOnSuccessListener { uri ->
                        val filePath = uri.toString()
                        val etMessage: String = messageBox.text.toString()
                        val date = Date()
                        val message = Message(etMessage, senderUid, date.time)
                        message.message = "photo"
                        message.imageUrl = filePath
                        val randomKeys = database.reference.push().key
                        val lastClickObj = HashMap<String, Any>()
                        lastClickObj["lastClick"] = message.message!!
                        lastClickObj["lastClickTime"] = date.time
                        database.reference.child("chats").updateChildren(lastClickObj)
                        database.reference.child("chats").child(receiverRoom!!)
                            .updateChildren(lastClickObj)
                        database.reference.child("chats").child(senderRoom!!).child("messages")
                            .child(randomKeys!!).setValue(message).addOnSuccessListener {
                                receivedPlayTone()
                                database.reference.child("chats").child(receiverRoom!!)
                                    .child("messages").child(randomKeys).setValue(message)
                                    .addOnSuccessListener {}
                            }
                    }
                }
            }

        }
        if (requestCode == imageRequestCode) {
            if (data != null) {
                if (data.data != null) {
                    val selectedImage = data.data
                    val calendar = Calendar.getInstance()
                    dialog.show()
                    val refence = storage.reference.child("chats")
                        .child(calendar.timeInMillis.toString() + "")
                    refence.putFile(selectedImage!!).addOnCompleteListener { task ->
                        dialog.dismiss()
                        playTone()
                        if (task.isSuccessful) {
                            refence.downloadUrl.addOnSuccessListener { uri ->
                                val filePath = uri.toString()
                                val etMessage: String = messageBox.text.toString()
                                val date = Date()
                                val message = Message(etMessage, senderUid, date.time)
                                message.message = "photo"
                                message.imageUrl = filePath
                                //  messageBox.setText("")
                                val randomKey = database.reference.push().key
                                val lastMsgObj = HashMap<String, Any>()
                                lastMsgObj["lastMsg"] = message.message!!
                                lastMsgObj["lastMsgTime"] = date.time
                                database.reference.child("chats").updateChildren(lastMsgObj)
                                database.reference.child("chats").child(receiverRoom!!)
                                    .updateChildren(lastMsgObj)
                                database.reference.child("chats").child(senderRoom!!)
                                    .child("messages").child(randomKey!!).setValue(message)
                                    .addOnSuccessListener {
                                        receivedPlayTone()
                                        database.reference.child("chats").child(receiverRoom!!)
                                            .child("messages").child(randomKey).setValue(message)
                                            .addOnSuccessListener {}
                                    }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val currentId = FirebaseAuth.getInstance().uid
        database.reference.child("presence").child(currentId!!).setValue("Online")
    }

    override fun onPause() {
        super.onPause()
        val currentId = FirebaseAuth.getInstance().uid
        database.reference.child("presence").child(currentId!!).setValue("Offline")
    }

//    fun getImageData(bmp: Bitmap) {
//        val bao = ByteArrayOutputStream()
//        bmp.compress(Bitmap.CompressFormat.PNG, 100, bao) // bmp is bitmap from user image file
//        bmp.recycle()
//        val byteArray: ByteArray = bao.toByteArray()
//        val imageB64: String = Base64.encodeToString(byteArray, Base64.URL_SAFE)
//        //  store & retrieve this string which is URL safe(can be used to store in FBDB) to firebase
//        // Use either Realtime Database or Firestore
//    }
}