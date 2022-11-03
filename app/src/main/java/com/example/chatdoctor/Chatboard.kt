package com.example.chatdoctor

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatdoctor.model.UserModel
import com.example.chatdoctor.sharepref.Constant
import com.example.chatdoctor.sharepref.PrefHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_chatboard.*
import kotlinx.android.synthetic.main.activity_chatboard.toolbar

class Chatboard : AppCompatActivity() {
    private lateinit var userRecyclerView: RecyclerView
    private lateinit var userList: ArrayList<UserModel>
    private lateinit var adapter: UserAdapter
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseAuth: DatabaseReference
    private lateinit var prefHelper: PrefHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatboard)
        setSupportActionBar(toolbar)

        //Loader Setup
        val loading = Loader(this)
        loading.startLoading()
        val handler = Handler()
        handler.postDelayed(object : Runnable{
            override fun run() {
                loading.isDismiss()
            }
        }, 2000)

        // sharedPrefrances
        prefHelper = PrefHelper(this)
        tvEmail.text = prefHelper.getString(Constant.PREF_EMAIL)

        //data showing through recycler view
        userRecyclerView = findViewById(R.id.rvUserList)
        firebaseAuth = FirebaseAuth.getInstance()
        databaseAuth = FirebaseDatabase.getInstance().reference
        userList = ArrayList()
        adapter = UserAdapter(this, userList)
        userRecyclerView.layoutManager = LinearLayoutManager(this)
        userRecyclerView.adapter = adapter

        databaseAuth.child("user").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (postSnapshot in snapshot.children) {
                    val currentUser = postSnapshot.getValue(UserModel::class.java)
                    if (firebaseAuth.currentUser?.uid != currentUser?.uid) {

                        userList.add(currentUser!!)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    //menu creation
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    //menu item clickable
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.profile -> {
                val intent = Intent(this, Profile::class.java)
                startActivity(intent)
            }
            R.id.search -> {}
            R.id.refresh -> {}
            R.id.logout -> {
                prefHelper.clear()
                firebaseAuth.signOut()
                val  intent = Intent (this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}