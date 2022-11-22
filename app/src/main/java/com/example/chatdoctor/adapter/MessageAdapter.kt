package com.example.chatdoctor.adapter

import android.content.Context
import android.media.MediaPlayer
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatdoctor.R
import com.example.chatdoctor.databinding.DeleteLayoutForReceiverBinding
import com.example.chatdoctor.databinding.DeleteLayoutForSenderBinding
import com.example.chatdoctor.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MessageAdapter(
    val context: Context,
    val messageList: ArrayList<Message>,
    val senderRoom: String,
    val receiverRoom: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val ITEM_RECEIVE = 1
    val ITEM_SENT = 2

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sentMessage = itemView.findViewById<TextView>(R.id.sentMessage)
        val sentImage = itemView.findViewById<ImageView>(R.id.sentImage)
    }

    class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveMessage = itemView.findViewById<TextView>(R.id.receiveMessage)
        val receiveImage = itemView.findViewById<ImageView>(R.id.receiveImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 1) {
            val view: View = LayoutInflater.from(context).inflate(R.layout.receive, parent, false)
            return ReceiveViewHolder(view)
        } else {
            val view: View = LayoutInflater.from(context).inflate(R.layout.sent, parent, false)
            return SentViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messageList[position]
        if (holder.javaClass == SentViewHolder::class.java) {

            val viewHolder = holder as SentViewHolder
            holder.sentMessage.text = currentMessage.message
            if (currentMessage.message.equals("photo")) {
                viewHolder.sentImage.visibility = View.VISIBLE
                viewHolder.sentMessage.visibility = View.GONE
                Glide.with(context).load(currentMessage.imageUrl)
                    .placeholder(R.drawable.placeholder).into(viewHolder.sentImage)
            }else{
                viewHolder.sentImage.visibility = View.GONE
                viewHolder.sentMessage.visibility = View.VISIBLE
            }
            viewHolder.itemView.setOnClickListener {
                val view = LayoutInflater.from(context).inflate(R.layout.delete_layout_for_sender, null)
                val binding: DeleteLayoutForSenderBinding = DeleteLayoutForSenderBinding.bind(view)
                val dialog = AlertDialog.Builder(context)
                    .setTitle("Delete Message")
                    .setView(binding.root)
                    .create()
                binding.everyone.setOnClickListener {
                    currentMessage.message = "This message is removed"
                    currentMessage.messageId.let { it1 ->
                        FirebaseDatabase.getInstance().reference.child("chats")
                            .child(senderRoom)
                            .child("message")
                            .child(it1!!).setValue(currentMessage)
                    }
                    currentMessage.messageId.let { it1 ->
                        FirebaseDatabase.getInstance().reference.child("chats")
                            .child(receiverRoom)
                            .child("message")
                            .child(it1!!).setValue(currentMessage)
                    }
                    dialog.dismiss()
                }
                binding.delete.setOnClickListener {
                    currentMessage.messageId.let { it1 ->
                        FirebaseDatabase.getInstance().reference.child("chats")
                            .child(senderRoom)
                            .child("message")
                            .child(it1!!).setValue(null)
                    }
                    dialog.dismiss()
                }
                binding.cancel.setOnClickListener { dialog.dismiss() }
                dialog.show()
                false
            }
        } else {
            val viewHolder = holder as ReceiveViewHolder
            holder.receiveMessage.text = currentMessage.message
            if (currentMessage.message.equals("photo")) {
                viewHolder.receiveImage.visibility = View.VISIBLE
                viewHolder.receiveMessage.visibility = View.GONE
                Glide.with(context).load(currentMessage.imageUrl)
                    .placeholder(R.drawable.placeholder)
                    .into(viewHolder.receiveImage)
            }else{
                viewHolder.receiveImage.visibility = View.GONE
                viewHolder.receiveMessage.visibility = View.VISIBLE
            }


        /*
            viewHolder.itemView.setOnClickListener {
                val view = LayoutInflater.from(context).inflate(R.layout.delete_layout_for_receiver, null)
                val binding: DeleteLayoutForReceiverBinding = DeleteLayoutForReceiverBinding.bind(view)
                val dialog = AlertDialog.Builder(context)
                    .setTitle("Delete Message")
                    .setView(binding.root)
                    .create()
                binding.delete.setOnClickListener {
                    currentMessage.messageId.let { it1 ->
                        FirebaseDatabase.getInstance().reference.child("chats")
                            .child(receiverRoom)
                            .child("message")
                            .child(it1!!)
                            .setValue(null)
                    }
                    dialog.dismiss()
                }
                binding.cancel.setOnClickListener { dialog.dismiss() }
                dialog.show()
                false
            }*/
        }
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage = messageList[position]
        if (FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.senderId)) {
            return ITEM_SENT
        } else {
            return ITEM_RECEIVE
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    fun receivedTone() {
        val mediaPlayer = MediaPlayer()
        try {
            val receivedPlayTone = context.assets.openFd("tone.mp3")
            mediaPlayer.setDataSource(receivedPlayTone.fileDescriptor, receivedPlayTone.startOffset, receivedPlayTone.length)
            receivedPlayTone.close()
            mediaPlayer.prepare()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        mediaPlayer.start()
    }
}