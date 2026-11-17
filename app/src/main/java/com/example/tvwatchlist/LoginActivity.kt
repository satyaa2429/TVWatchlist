
package com.example.tvwatchlist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.tvwatchlist.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        try {
            val task = GoogleSignIn
                .getSignedInAccountFromIntent(result.data)

            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (!idToken.isNullOrBlank()) {
                firebaseAuthWithGoogle(idToken)
            } else {
                showLoading(false)
                showMessage("Google Sign-In failed: Missing ID Token")
            }

        } catch (e: ApiException) {

            showLoading(false)

            Log.e("GoogleSignIn", "Sign-in failed", e)

            val message = when (e.statusCode) {
                10 -> "Configuration error: Check Firebase SHA-1"
                7 -> "Network error. Check internet connection"
                12500 -> "Google Sign-In failed. Check Firebase settings"
                12501 -> "Google Sign-In cancelled"
                else -> "Google Sign-In error: ${e.statusCode}"
            }

            showMessage(message)

        } catch (e: Exception) {
            showLoading(false)
            showMessage("Error: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showLoading(false)

        binding.btnGuestSignIn.setOnClickListener {
            navigateToMain()
        }

        try {
            auth = FirebaseAuth.getInstance()

            if (auth.currentUser != null) {
                navigateToMain()
                return
            }

            setupGoogleSignIn()

        } catch (e: Exception) {
            showMessage("Firebase configuration error: ${e.message}")
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun setupGoogleSignIn() {

        val clientIdResource = resources.getIdentifier(
            "default_web_client_id",
            "string",
            packageName
        )

        if (clientIdResource == 0) {
            showMessage("Firebase Web Client ID is missing")
            return
        }

        val webClientId = getString(clientIdResource)

        val gso = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {

        if (!::googleSignInClient.isInitialized ||
            !::auth.isInitialized) {

            showMessage("Complete Firebase configuration first")
            return
        }

        showLoading(true)

        try {
            googleSignInLauncher.launch(
                googleSignInClient.signInIntent
            )

        } catch (e: Exception) {

            showLoading(false)
            showMessage("Unable to sign in: ${e.message}")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {

        val credential = GoogleAuthProvider.getCredential(
            idToken,
            null
        )

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->

                showLoading(false)

                if (task.isSuccessful) {

                    showMessage("Authentication successful!")

                    navigateToMain()

                } else {

                    Log.e(
                        "FirebaseAuth",
                        "Authentication failed",
                        task.exception
                    )

                    showMessage(
                        "Firebase authentication failed: " +
                                task.exception?.localizedMessage
                    )
                }
            }
    }

    private fun showLoading(isLoading: Boolean) {

        binding.progressBarLogin.visibility =
            if (isLoading) View.VISIBLE else View.GONE

        binding.btnGoogleSignIn.isEnabled = !isLoading

        binding.btnGuestSignIn.isEnabled = !isLoading
    }

    private fun showMessage(message: String) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun navigateToMain() {

        val intent = Intent(
            this,
            MainActivity::class.java
        )

        startActivity(intent)
        finish()
    }
}
