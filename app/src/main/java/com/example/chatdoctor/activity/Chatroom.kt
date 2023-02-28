package com.example.chatdoctor.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.icu.util.Calendar
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
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
    private lateinit var receiverUid: String
    private lateinit var senderUid: String
    private lateinit var binding: ActivityChatroomBinding
    private lateinit var dialog: ProgressDialog
    private lateinit var date: Date
    private var isattachment = false
    private var isRead = false
    var receiverRoom: String? = null
    var senderRoom: String? = null
    private val imageRequestCode = 1888
    private val cameraRequestCode = 1
    private val camera = 2
    private val contactPermissionCode = 3
    private val contactPickCode = 4

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityChatroomBinding = ActivityChatroomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //database, firebase storage, senderUid, date initialize
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
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
        receiverUid = intent.getStringExtra("uid").toString()
        senderUid = FirebaseAuth.getInstance().currentUser?.uid.toString()
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
                val sentPlayTone = applicationContext.assets.openFd("sent.mp3")
                mediaPlayer.setDataSource(
                    sentPlayTone.fileDescriptor,
                    sentPlayTone.startOffset,
                    sentPlayTone.length
                )
                sentPlayTone.close()
                mediaPlayer.prepare()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            mediaPlayer.start()
        }

        /*val readMessageObjects = HashMap<String, Any>()
        readMessageObjects["read"] = true
        database.reference.child("chats").child(receiverRoom!!).child("messages")
            .updateChildren(readMessageObjects)*/

        msgSend.setOnClickListener {
            val myMessage = etMessage.text.toString()
            if (myMessage.isNotEmpty()) {
                val messageObject = Message()
                messageObject.apply {
                    message = myMessage
                    senderId = senderUid
                    receiverId = receiverUid
                    isRead = false

                    val c = Calendar.getInstance()
                    val hour = c.get(Calendar.HOUR_OF_DAY)
                    val minuts = c.get(Calendar.MINUTE)
                    val timeStamp = "$hour:$minuts"
                    val messageObjects = HashMap<String, Any>()
                    messageObjects["lastMessage"] = message!!
                    messageObjects["messageTime"] = timeStamp

                    database.reference.child("chats").updateChildren(messageObjects)

                }
                sentPlayTone()
                databaseAuth.child("chats").child(senderRoom!!).child("messages").push()
                    .setValue(messageObject).addOnSuccessListener {
                        databaseAuth.child("chats").child(receiverRoom!!).child("messages").push()
                            .setValue(messageObject)
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

        contactIcon.setOnClickListener {
            if (checkContactPermission()) {
                pickContact()
            } else {
                requestContactPermission()
            }
        }

    }

    private fun checkContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestContactPermission() {
        val permission = arrayOf(Manifest.permission.READ_CONTACTS)
        ActivityCompat.requestPermissions(this, permission, contactPermissionCode)
    }

    private fun pickContact() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        startActivityForResult(intent, contactPickCode)
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
        if (requestCode == contactPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickContact()
            } else {
                Toast.makeText(
                    this,
                    "Oops you just denied the contact permission. " + "Don't worry you can allow in the settings",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val mediaPlayer = MediaPlayer()
    fun imageSentPlayTone() {
        try {
            val sentTone = applicationContext.assets.openFd("sent.mp3")
            mediaPlayer.setDataSource(
                sentTone.fileDescriptor,
                sentTone.startOffset,
                sentTone.length
            )
            sentTone.close()
            mediaPlayer.prepare()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        mediaPlayer.start()
    }

    fun imageReceivedPlayTone() {
        try {
            val receivedTone = applicationContext.assets.openFd("tone.mp3")
            mediaPlayer.setDataSource(
                receivedTone.fileDescriptor,
                receivedTone.startOffset,
                receivedTone.length
            )
            receivedTone.close()
            mediaPlayer.prepare()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        mediaPlayer.start()
    }

    @SuppressLint("Range")
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == contactPickCode) {
            if (data != null) {
                // sentMessage.text = ""
                val cursor1: Cursor
                val cursor2: Cursor?
                val selectedContact = data.data
                cursor1 = contentResolver.query(selectedContact!!, null, null, null, null)!!
                if (cursor1.moveToFirst()) {
                    val idResults =
                        cursor1.getString(cursor1.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                    val idResultHold = idResults.toInt()
                    if (idResultHold == 1) {
                        cursor2 = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ",
                            null,
                            null
                        )
                        while (cursor2!!.moveToNext()) {
                            val contactNumber =
                                cursor2.getString(cursor2.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            sentMessage.append("\nPhone: $contactNumber")
                        }
                        cursor2.close()
                    }
                    cursor1.close()
                }
                val calendar = Calendar.getInstance()
                dialog.show()
                val refrance = storage.reference.child("chats")
                    .child(calendar.timeInMillis.toString() + "")
                refrance.putFile(selectedContact).addOnCompleteListener { task ->
                    dialog.dismiss()
                    cardView1.visibility = View.GONE
                    if (task.isSuccessful) {

                        refrance.downloadUrl.addOnSuccessListener { uri ->
                            // val filePath = uri.toString()
                            val etMessage: String = messageBox.text.toString()
                            val date = Date()
                            val message = Message(etMessage, senderUid, date.time)
                            message.message = "contact"
                            //   message.imageUrl = filePath
                            val contactRandomKey = database.reference.push().key
                            val lastContObj = HashMap<String, Any>()
                            lastContObj["lastMsg"] = message.message!!
                            lastContObj["lastMsgTime"] = date.time
                            database.reference.child("chats").updateChildren(lastContObj)
                            database.reference.child("chats").child(receiverRoom!!)
                                .updateChildren(lastContObj)
                            database.reference.child("chats").child(senderRoom!!)
                                .child("messages").child(contactRandomKey!!).setValue(message)
                                .addOnSuccessListener {
                                    imageReceivedPlayTone()
                                    database.reference.child("chats").child(receiverRoom!!)
                                        .child("messages").child(contactRandomKey).setValue(message)
                                        .addOnSuccessListener {}
                                }
                        }
                    }
                }
            }
        }
        //Camera Image implementation
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
                cardView1.visibility = View.GONE
                imageSentPlayTone()
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
                        database.reference.child("chats").child(senderRoom!!)
                            .child("messages").child(randomKeys!!).setValue(message)
                            .addOnSuccessListener {
                                imageReceivedPlayTone()
                                database.reference.child("chats").child(receiverRoom!!)
                                    .child("messages").child(randomKeys).setValue(message)
                                    .addOnSuccessListener {}
                            }
                    }
                }
            }

        }

        //Image implementation
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
                        cardView1.visibility = View.GONE
                        imageSentPlayTone()
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
                                database.reference.child("chats").child(senderRoom!!)
                                    .child("messages").child(randomKey!!).setValue(message)
                                    .addOnSuccessListener {
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
}