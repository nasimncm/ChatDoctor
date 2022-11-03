package com.example.chatdoctor

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatdoctor.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_chatroom.*

class Chatroom : AppCompatActivity() {
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var databaseAuth: DatabaseReference

    var receiverRoom: String? = null
    var senderRoom: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatroom)


        //get name code
        val tvName = findViewById<TextView>(R.id.tvName)
        tvName.text = intent.getStringExtra("name")

        //set back button
        ivBackButton.setOnClickListener {
            onBackPressed()
        }

        // code for chat room
        val receiverUid = intent.getStringExtra("uid")
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        databaseAuth = FirebaseDatabase.getInstance().reference

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        databaseAuth.child("chats").child(senderRoom!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    messageList.clear()
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(Message::class.java)
                        messageList.add(message!!)
                    }
                    messageAdapter.notifyDataSetChanged()
                    chatRecyclerView.scrollToPosition(messageList.size-1)

                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

        msgSend.setOnClickListener {
            val message = etMessage.text.toString()
            val messageObject = Message(message, senderUid)
            databaseAuth.child("chats").child(senderRoom!!).child("messages").push()
                .setValue(messageObject).addOnSuccessListener {
                    databaseAuth.child("chats").child(receiverRoom!!).child("messages").push()
                        .setValue(messageObject)
                }
            etMessage.setText("")

        }
        //view the data

        chatRecyclerView = findViewById(R.id.rvChat)
        messageBox = findViewById(R.id.etMessage)
        sendButton = findViewById(R.id.msgSend)
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)

        chatRecyclerView.adapter = messageAdapter

        messageAdapter.notifyDataSetChanged()


    }
}