package com.example.chatdoctor.adapter

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

class UserAdapter(
    val context: Context,
    var userList: ArrayList<UserModel>,
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>(), Filterable {

    private var allContact = userList
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName = itemView.findViewById<TextView>(R.id.tvUserName)
        val recentMsg = itemView.findViewById<TextView>(R.id.recentMsg)
        val userImage = itemView.findViewById<ImageView>(R.id.userImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.user_layout, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        val fromId = FirebaseAuth.getInstance().uid
        holder.recentMsg.text = FirebaseDatabase.getInstance().getReference("lastmessage/$fromId").toString()
        holder.txtName.text = currentUser.name
        Glide.with(context).load(currentUser.imageUrl).into(holder.userImage)
      //  Log.d( "onBindViewHolder: ",currentUser.imageUrl.toString());

        holder.itemView.setOnClickListener {
            val intent = Intent(context, Chatroom::class.java)
            intent.putExtra("name", currentUser.name)
            intent.putExtra("uid", currentUser.uid)
            intent.putExtra("imageUrl", currentUser.imageUrl)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return userList.size
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