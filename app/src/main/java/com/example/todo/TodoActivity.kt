package com.example.todo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

const val TAG = "TodoActivity"

class TodoActivity : AppCompatActivity() {
    private lateinit var backButton: Button
    private lateinit var buttonAddTodo: Button
    private lateinit var textInputEditText: TextInputEditText
    private lateinit var textInputLayout: TextInputLayout
    private lateinit var spinner: ProgressBar
    private var user: FirebaseUser? = null
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvDisplayName: TextView
    private lateinit var todoRecyclerView: RecyclerView
    private lateinit var logoutButton: Button
    private lateinit var addTodoButton: FloatingActionButton
    private var todoList: MutableList<String> = mutableListOf()
    private var message: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo)

        tvDisplayName = findViewById(R.id.tv_display_name)
        todoRecyclerView = findViewById(R.id.todo_recycler_view)
        logoutButton = findViewById(R.id.log_out_button)
        addTodoButton = findViewById(R.id.add_todo_button)


        logoutButton.setOnClickListener {
            logoutFromGoogle()
        }

        addTodoButton.setOnClickListener {
            addTodo()
        }

        val idToken = intent.getStringExtra("idToken")

        firebaseAuth = FirebaseAuth.getInstance()

        db = Firebase.firestore

        firebaseAuthWithGoogle(idToken!!)
    }

    private fun addTodo() {
        textInputLayout = findViewById(R.id.text_input_layout)
        textInputEditText = findViewById(R.id.text_input_edit_text)
        buttonAddTodo = findViewById(R.id.button_add_todo)
        backButton = findViewById(R.id.back_button)
        tvDisplayName.visibility = View.GONE
        logoutButton.visibility = View.GONE
        todoRecyclerView.visibility = View.GONE
        addTodoButton.visibility = View.GONE
        textInputLayout.visibility = View.VISIBLE
        textInputEditText.visibility = View.VISIBLE
        buttonAddTodo.visibility = View.VISIBLE
        backButton.visibility = View.VISIBLE
        backButton.setOnClickListener {
            showRecyclerView()
        }
        buttonAddTodo.setOnClickListener {
            message = textInputEditText.text.toString()
            println("note${message}")
            if (message != "") {
                todoList.add(message!!)
                showRecyclerView()
                textInputEditText.text = null
                addUser(User(user?.email.toString(), user?.displayName.toString(), todoList))
            } else {
                textInputLayout.error = "Please enter the text"
            }
        }
    }

    private fun showRecyclerView() {
        tvDisplayName.visibility = View.VISIBLE
        logoutButton.visibility = View.VISIBLE
        todoRecyclerView.visibility = View.VISIBLE
        addTodoButton.visibility = View.VISIBLE
        textInputLayout.visibility = View.GONE
        textInputEditText.visibility = View.GONE
        buttonAddTodo.visibility = View.GONE
        backButton.visibility = View.GONE
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "sign in success")
                    user = firebaseAuth.currentUser
                    spinner = findViewById(R.id.progressBar1)
                    spinner.visibility = View.VISIBLE
                    getUserData(user)
                    tvDisplayName.text = user?.displayName.toString()
                } else {
                    Log.d(TAG, "failure", task.exception)
                }
            }
    }

    private fun logoutFromGoogle() {
        firebaseAuth.signOut()
        finish()
        Toast.makeText(this, "LogOut successful", Toast.LENGTH_SHORT).show()
    }

    private fun getUserData(user: FirebaseUser?) {
        val docRef = db.collection("users").document(user?.email.toString())
        docRef.get()
            .addOnSuccessListener { document ->
                println("doc$document")
                println(document.data)
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    if (document.data != null)
                        todoList = document.data?.get("todos") as MutableList<String>
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
            .addOnCompleteListener {
                addUser(User(user?.email.toString(), user?.displayName.toString(), todoList))
                spinner.visibility = View.GONE
                addTodoButton.visibility = View.VISIBLE
            }
    }

    private fun addUser(user: User) {
        db.collection("users")
            .document(user.id)
            .set(User(user.id, user.name, todoList))
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
                performOptionsMenuClick(position)
            }
        })

    }

    private fun performOptionsMenuClick(position: Int) {

        // creating a popup menu
        val popup = PopupMenu(this, todoRecyclerView[position].findViewById(R.id.tv_options))
        // inflating menu from xml resource
        popup.inflate(R.menu.options_menu)
        // adding click listener
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.delete -> { //handle delete click
                    todoList.removeAt(position)
                    addUser(User(user?.email.toString(), user?.displayName.toString(), todoList))
                    true
                }
                R.id.cancel ->                         //handle cancel click
                    true
                else -> false
            }
        }
        // displaying the popup
        popup.show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }


}