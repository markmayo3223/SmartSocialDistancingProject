package com.uccpe.fac19

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.common.collect.Maps
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_login.*
import java.util.jar.Manifest

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailLogin: TextInputEditText
    private lateinit var passLogin: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth
        emailLogin = findViewById(R.id.userEmailSignIn)
        passLogin = findViewById(R.id.userPassSignIn)

        val signUpClickMe = findViewById<TextView>(R.id.signUpHere)
        signUpClickMe.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener() {
            if(validateEntries()){
                auth.signInWithEmailAndPassword(emailLogin.text.toString(), passLogin.text.toString())
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information

                            val user = auth.currentUser
                            updateUI(user)
                        } else {
                            // If sign in fails, display a message to the user.
                            updateUI(null)
                            Toast.makeText(baseContext, "Incorrect username and/or password.", Toast.LENGTH_SHORT).show()
                        }
                        // ...
                    }
            }
        }

    }

    public override fun onStart() {
        super.onStart()

        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun updateUI(currentUser : FirebaseUser?){
        if(currentUser != null){
            if(currentUser.isEmailVerified){
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }else {
                val email = currentUser.email
                val disp = "Email address not verified for $email. Please verify first with the email sent."
                Toast.makeText(
                    baseContext, disp, Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun validateEntries(): Boolean{
        if(emailLogin.text.toString().isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(emailLogin.text.toString()).matches()){
            // Nothing to write so here's a cookie
        }else{
            userEmailSignIn.error = "Please enter valid email."
            userEmailSignIn.requestFocus()
            return false
        }

        // Password validation

        if(passLogin.text.toString().length < 8){
            passLogin.error = "Min. 8 characters."
            passLogin.requestFocus()
            return false
        }
        return true
    }

}