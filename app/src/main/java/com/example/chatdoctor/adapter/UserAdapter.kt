package com.example.chatdoctor.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatdoctor.R
import com.example.chatdoctor.activity.Chatroom
import com.example.chatdoctor.model.UserModel

class UserAdapter(
    val context: Context,
    val userList: ArrayList<UserModel>
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName = itemView.findViewById<TextView>(R.id.tvUserName)
        val userImage = itemView.findViewById<ImageView>(R.id.userImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.user_layout, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
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
}