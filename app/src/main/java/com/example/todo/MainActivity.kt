package com.example.todo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

const val REQUEST_ID = 101
const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    lateinit var gso : GoogleSignInOptions
    lateinit var mSignClient: GoogleSignInClient
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<SignInButton>(R.id.sign_in_button).setOnClickListener {
            signIn()
        }

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.token))
            .requestEmail()
            .build()


        firebaseAuth = FirebaseAuth.getInstance()
        mSignClient = GoogleSignIn.getClient(this, gso)
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
                    val user = firebaseAuth.currentUser
                    updateUI(user)
                } else {
                    Log.d(TAG, "failure", task.exception)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        Log.d(TAG, user?.displayName.toString())
    }
}