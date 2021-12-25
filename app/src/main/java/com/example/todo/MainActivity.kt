package com.example.todo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

const val REQUEST_ID = 101
const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    lateinit var gso : GoogleSignInOptions
    lateinit var mSignClient: GoogleSignInClient
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var db: FirebaseFirestore
    lateinit var tvDisplayName: TextView
    lateinit var todoRecyclerView: RecyclerView
    lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<SignInButton>(R.id.sign_in_button).setOnClickListener {
            signIn()
        }
        tvDisplayName = findViewById(R.id.tv_display_name)
        todoRecyclerView = findViewById(R.id.todo_recycler_view)
        logoutButton = findViewById(R.id.log_out_button)

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.token))
            .requestEmail()
            .build()

        logoutButton.setOnClickListener {
            logoutFromGoogle()
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
            try{
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "From Google: ${account?.id}")
                firebaseAuthWithGoogle(account?.idToken!!)
            }catch (e: ApiException){
                Toast.makeText(this,"sign in failed",Toast.LENGTH_LONG).show()
                Log.w(TAG,"Google sign in failed ${e.localizedMessage}", e)
            }

        }
    }

    fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "sign in success")
                    Toast.makeText(this,"Sign in successful",Toast.LENGTH_SHORT).show()
                    findViewById<SignInButton>(R.id.sign_in_button).visibility = View.GONE
                    findViewById<TextView>(R.id.tv_login).visibility = View.GONE
                    tvDisplayName.visibility = View.VISIBLE
                    todoRecyclerView.visibility = View.VISIBLE
                    logoutButton.visibility = View.VISIBLE

                    val user = firebaseAuth.currentUser
                    updateUI(user)
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

    private fun logoutFromGoogle(){
        firebaseAuth.signOut()
        Toast.makeText(this,"LogOut successful",Toast.LENGTH_SHORT).show()
        println("${firebaseAuth.currentUser}wow")
    }
}