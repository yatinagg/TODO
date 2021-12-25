package com.example.todo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException

const val REQUEST_ID = 101

class LoginPageActivity : AppCompatActivity() {
    private lateinit var gso: GoogleSignInOptions
    private lateinit var mSignClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        findViewById<SignInButton>(R.id.sign_in_button).setOnClickListener {
            signIn()
        }

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.token))
            .requestEmail()
            .build()

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
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "From Google: ${account?.id}")
                val intent = Intent(this, TodoActivity::class.java)
                intent.putExtra("idToken", account?.idToken!!)
                Toast.makeText(this, "sign in success", Toast.LENGTH_LONG).show()
                startActivity(intent)
            } catch (e: ApiException) {
                Toast.makeText(this, "sign in failed", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Google sign in failed ${e.localizedMessage}", e)
            }

        }
    }

}