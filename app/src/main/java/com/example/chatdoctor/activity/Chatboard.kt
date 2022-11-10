package com.example.chatdoctor.activity

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.chatdoctor.Loader
import com.example.chatdoctor.MainActivity
import com.example.chatdoctor.R
import com.example.chatdoctor.adapter.UserAdapter
import com.example.chatdoctor.model.UserModel
import com.example.chatdoctor.sharepref.Constant
import com.example.chatdoctor.sharepref.PrefHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_chatboard.*

class Chatboard : AppCompatActivity() {
    private lateinit var userRecyclerView: RecyclerView
    private lateinit var userList: ArrayList<UserModel>
    private lateinit var adapter: UserAdapter
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseAuth: DatabaseReference
    private lateinit var prefHelper: PrefHelper
    private lateinit var database: FirebaseDatabase
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var mProgress: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatboard)
        setSupportActionBar(toolbar)

        //swipe page refresh code
        swipeRefreshLayout = findViewById(R.id.swipeLayout)
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            adapter.notifyDataSetChanged()
        }
        //Loader Setup
        mProgress = ProgressDialog(this)
        mProgress.setTitle("Processing...")
        mProgress.setMessage("Please wait...")
        mProgress.setCancelable(false)
        mProgress.isIndeterminate

        //database initialize
        database = FirebaseDatabase.getInstance()

        // SharedPreferences
        prefHelper = PrefHelper(this)
        tvEmail.text = prefHelper.getString(Constant.PREF_EMAIL)

        //data showing through recycler view
        //recycle view initialize
        userRecyclerView = findViewById(R.id.rvUserList)

        //firebase Auth initialize
        firebaseAuth = FirebaseAuth.getInstance()

        //firebase Database initialize
        databaseAuth = FirebaseDatabase.getInstance().reference

        //Data model initialize
        userList = ArrayList()

        //user adapter initialize
        adapter = UserAdapter(this, userList)

        //Linear layout manager initialize in recycler view
        userRecyclerView.layoutManager = LinearLayoutManager(this)

        //set the adapter in recycler view
        userRecyclerView.adapter = adapter
        getChatList()// create function for show the user data

    }


    fun getChatList() {
        mProgress.show()
        //get the data from database "user" section
        databaseAuth.child("user").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //clear the user list
                userList.clear()

                //read the data from db using snapshot
                for (postSnapshot in snapshot.children) {
                    val currentUser = postSnapshot.getValue(UserModel::class.java)
                    //code for current user not show in chat list
                    if (firebaseAuth.currentUser?.uid != currentUser?.uid) {
                        userList.add(currentUser!!)
                    }
                }
                mProgress.dismiss()
                //data change refresh
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {

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
                val intent = Intent(this, ProfileSection::class.java)
                startActivity(intent)
            }
            R.id.search -> {}
            R.id.logout -> {
                prefHelper.clear()
                firebaseAuth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}