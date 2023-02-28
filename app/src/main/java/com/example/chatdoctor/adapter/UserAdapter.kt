package com.example.chatdoctor.adapter

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatdoctor.R
import com.example.chatdoctor.activity.Chatroom
import com.example.chatdoctor.model.Message
import com.example.chatdoctor.model.UserModel
import com.example.chatdoctor.sharepref.Constant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserAdapter(
    val context: Context,
    var userList: ArrayList<UserModel>,
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>(), Filterable {

    private var allContact = userList
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName = itemView.findViewById<TextView>(R.id.tvUserName)
        val recentMsg = itemView.findViewById<TextView>(R.id.recentMsg)
        val userImage = itemView.findViewById<ImageView>(R.id.userImage)
        val msgNotiCount = itemView.findViewById<TextView>(R.id.msg_notification)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.user_layout, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        Log.d( "onBindViewHolder: ", currentUser.email.toString())
        holder.txtName.text = currentUser.name
        Glide.with(context).load(currentUser.imageUrl).into(holder.userImage)
        getChatMessage(holder.msgNotiCount, position)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, Chatroom::class.java)
            intent.putExtra("name", currentUser.name)
            intent.putExtra("uid", currentUser.uid)
            intent.putExtra("email", currentUser.email)
            intent.putExtra("imageUrl", currentUser.imageUrl)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    val mediaPlayer = MediaPlayer()
    fun sentPlayTone() {
        try {
            val sentPlayTone = context.assets.openFd("receive.mp3")
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


    private fun getChatMessage(msgNotiCounTV: TextView, position: Int) {
        if (userList.size > 0) {
            val senderUid = FirebaseAuth.getInstance().currentUser?.uid.toString()
            val senderRoom: String = userList[position].uid + senderUid
            val databaseAuth: DatabaseReference = FirebaseDatabase.getInstance().reference
            databaseAuth.child("chats").child(senderRoom).child("messages")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val msgList: ArrayList<Message> = ArrayList()
                        for (postSnapshot in snapshot.children) {
                            val message = postSnapshot.getValue(Message::class.java)
                            message!!.messageId = postSnapshot.key
                            msgList.add(message)

                        }
                        if (msgList.size > 0) {
                            msgNotiCounTV.visibility = View.VISIBLE
                            msgNotiCounTV.text = "${msgList.size}"

                        }
                        else {
                            msgNotiCounTV.visibility = View.GONE
                        }

                    }

                    override fun onCancelled(error: DatabaseError) {}

                })
        }

    }


    override fun getFilter(): Filter {
        return object : Filter(){
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchContent = constraint.toString()
                if (searchContent.isEmpty())
                    allContact = userList
                else{
                    val filterContact = ArrayList<UserModel>()
                    for (userModel in userList){
                        if (userModel.name?.toLowerCase()
                                ?.trim()?.contains(searchContent.toLowerCase().trim()) == true
                        )
                            filterContact.add(userModel)
                    }
                    allContact = filterContact
                }
                val filterResults = FilterResults()
                filterResults.values=allContact
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                userList = results?.values as ArrayList<UserModel>
                notifyDataSetChanged()
            }
        }
    }
}