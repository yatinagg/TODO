package com.example.todo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

const val REQUEST_ID = 101
const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    lateinit var gso: GoogleSignInOptions
    lateinit var mSignClient: GoogleSignInClient
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var db: FirebaseFirestore
    lateinit var tvDisplayName: TextView
    lateinit var todoRecyclerView: RecyclerView
    lateinit var logoutButton: Button
    lateinit var todoList: MutableList<String>
    lateinit var addTodoButton: FloatingActionButton
    private lateinit var customAdapter: CustomAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        todoList = mutableListOf()
        findViewById<SignInButton>(R.id.sign_in_button).setOnClickListener {
            signIn()
        }
        tvDisplayName = findViewById(R.id.tv_display_name)
        todoRecyclerView = findViewById(R.id.todo_recycler_view)
        logoutButton = findViewById(R.id.log_out_button)
        addTodoButton = findViewById(R.id.add_todo_button)

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.token))
            .requestEmail()
            .build()

        logoutButton.setOnClickListener {
            logoutFromGoogle()
        }

        addTodoButton.setOnClickListener {
            val newTodo = "newTodo"
            todoList.add(newTodo)
            addUser(firebaseAuth.currentUser)
        }

        firebaseAuth = FirebaseAuth.getInstance()
        mSignClient = GoogleSignIn.getClient(this, gso)

        db = Firebase.firestore
    }

    private fun signIn() {
        val signInIntent = mSignClient.signInIntent
        startActivityForResult(signInIntent, REQUEST_ID)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ID) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "From Google: ${account?.id}")
                firebaseAuthWithGoogle(account?.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "sign in failed", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Google sign in failed ${e.localizedMessage}", e)
            }

        }
    }

    fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "sign in success")
                    Toast.makeText(this, "Sign in successful", Toast.LENGTH_SHORT).show()
                    findViewById<SignInButton>(R.id.sign_in_button).visibility = View.GONE
                    findViewById<TextView>(R.id.tv_login).visibility = View.GONE
                    tvDisplayName.visibility = View.VISIBLE
                    todoRecyclerView.visibility = View.VISIBLE
                    logoutButton.visibility = View.VISIBLE
                    addTodoButton.visibility = View.VISIBLE

                    val user = firebaseAuth.currentUser
                    getUserData(user?.email.toString())
                    updateUI(user)
                    addUser(user)
                } else {
                    Log.d(TAG, "failure", task.exception)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        println("${firebaseAuth.currentUser}wow")
        tvDisplayName.text = user?.displayName.toString()
        Log.d(TAG, user?.displayName.toString())
    }

    private fun logoutFromGoogle() {
        firebaseAuth.signOut()
        tvDisplayName.visibility = View.GONE
        todoRecyclerView.visibility = View.GONE
        logoutButton.visibility = View.GONE
        addTodoButton.visibility = View.GONE
        findViewById<SignInButton>(R.id.sign_in_button).visibility = View.VISIBLE
        findViewById<TextView>(R.id.tv_login).visibility = View.VISIBLE
        Toast.makeText(this, "LogOut successful", Toast.LENGTH_SHORT).show()
        println("${firebaseAuth.currentUser}wow")
    }

    private fun getUserData(id: String) {
        val docRef = db.collection("users").document(id)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    todoList = document.data?.get("todos") as MutableList<String>
                    println("todoList $todoList")
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }

    private fun addUser(user: FirebaseUser?) {
        db.collection("users")
            .document(user?.email.toString())
            .set(User(user?.displayName.toString(), todoList))
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot added")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
        todoRecyclerView.layoutManager = LinearLayoutManager(this)

        todoRecyclerView.adapter = CustomAdapter(todoList, object :
            CustomAdapter.OptionsMenuClickListener {
            // implement the required method
            override fun onOptionsMenuClicked(position: Int) {
                // this method will handle the onclick options click
                // it is defined below
                performOptionsMenuClick(position)
            }
        })

    }

    private fun performOptionsMenuClick(position: Int) {

            //creating a popup menu
            //creating a popup menu
            val popup = PopupMenu(this, todoRecyclerView[position].findViewById(R.id.tv_options))
            //inflating menu from xml resource
            popup.inflate(R.menu.options_menu)
            //adding click listener
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.delete -> {
                        Log.d(
                            "adapter",
                            position.toString()
                        )                        //handle delete click
                        true
                    }
                    R.id.cancel ->                         //handle cancel click
                        true
                    else -> false
                }
            }
            //displaying the popup
            //displaying the popup
            popup.show()
    }
}