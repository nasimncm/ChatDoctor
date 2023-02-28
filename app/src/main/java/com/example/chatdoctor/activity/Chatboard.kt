package com.example.chatdoctor.activity

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.chatdoctor.R
import com.example.chatdoctor.adapter.UserAdapter
import com.example.chatdoctor.login.MainActivity
import com.example.chatdoctor.model.Message
import com.example.chatdoctor.model.UserModel
import com.example.chatdoctor.sharepref.Constant
import com.example.chatdoctor.sharepref.PrefHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_chatboard.*
import kotlinx.android.synthetic.main.user_layout.*

class Chatboard : AppCompatActivity() {
    private lateinit var userRecyclerView: RecyclerView
    private lateinit var userList: ArrayList<UserModel>
    private lateinit var messageList: ArrayList<Message>
    private lateinit var adapter: UserAdapter
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseAuth: DatabaseReference
    private lateinit var prefHelper: PrefHelper
    private lateinit var database: FirebaseDatabase
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var mProgress: ProgressDialog
    private lateinit var search: SearchView
    private var notification = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatboard)
        setSupportActionBar(toolbar)

        search = SearchView(this)

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

        //Internet Dialog show
        val dialogBuilder= AlertDialog.Builder(this)
        dialogBuilder.setTitle("Internet Connection Lost")
        dialogBuilder.setMessage("Your Internet Connection is lost Please Check your wifi or Network")
        dialogBuilder.setPositiveButton("Refresh") { dialogInterface: DialogInterface, i: Int ->
            Toast.makeText(this, "Go to setting", Toast.LENGTH_LONG).show()
        }
        //set the adapter in recycler view
        userRecyclerView.adapter = adapter
        getChatList()// create function for show the user data


        if (checkInternet(this)){
        }else{
            mProgress.dismiss()
          //  Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show()
            dialogBuilder.create().show()
        }


    }

    //Internet Connectivity
    private fun checkInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network)
            return when{
                activeNetwork!!.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)-> true
                else ->false
            }
        }else{
            @Suppress("DEPRECATION") val networkInfo = connectivityManager.activeNetworkInfo?: return false
            @Suppress("DEPRECATION") return networkInfo.isConnected
        }
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
        val searchViewItem = menu?.findItem(R.id.searchMenu)
        val searchView = MenuItemCompat.getActionView(searchViewItem) as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                if (adapter.equals(query)) {
                    adapter.filter.filter(query)
                }
                userList.clear()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                userRecyclerView = findViewById(R.id.rvUserList)

                //set the adapter in recycler view
                userRecyclerView.adapter = adapter
                getChatList()
                adapter.notifyDataSetChanged()
                return false
            }
        })




        return true
    }

    //menu item clickable
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.profile -> {
                val intent = Intent(this, ProfileSection::class.java)
                startActivity(intent)
            }
            /*R.id.searchMenu -> {
                val intent = Intent(this, Search::class.java)
                startActivity(intent)
            }*/
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