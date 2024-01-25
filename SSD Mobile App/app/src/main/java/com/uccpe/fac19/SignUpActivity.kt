package com.uccpe.fac19

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.sign_up_activity.*

class SignUpActivity : AppCompatActivity() {
    // Variable Declarations
    private lateinit var surname: TextInputEditText
    private lateinit var firstName: TextInputEditText
    private lateinit var middleInitial: TextInputEditText
    private lateinit var username: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var currentAddress: TextInputEditText
    private lateinit var contactNumber: TextInputEditText
    private lateinit var password: TextInputEditText
    private lateinit var conPassword: TextInputEditText
    private var validated : Boolean = false

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    val TAG = "Message"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_up_activity)

        auth = FirebaseAuth.getInstance()

        // Variable initializations
        surname  = findViewById(R.id.SurSignUpChild)
        firstName = findViewById(R.id.FirSignUpChild)
        middleInitial = findViewById(R.id.MNSignUpChild)
        username = findViewById(R.id.userIDSignUpChild)
        emailInput = findViewById(R.id.emailSignUpChild)
        currentAddress = findViewById(R.id.currentAddressSignUpChild)
        contactNumber = findViewById(R.id.numberSignUpChild)
        password = findViewById(R.id.passSignUpChild)
        conPassword = findViewById(R.id.conPassSignUpChild)


        // TO DO

        val termsOfServiceLink = findViewById<TextView>(R.id.tos_text)
        termsOfServiceLink.setOnClickListener {

            val openURL = Intent(android.content.Intent.ACTION_VIEW)
            openURL.data = Uri.parse("https://www.termsofservicegenerator.net/live.php?token=Bjivi5z9CW44D9QW9ltkCa0rPfGSUnXB")
            startActivity(openURL)
        }

        val privacyPolicyLink = findViewById<TextView>(R.id.pp_text)
        privacyPolicyLink.setOnClickListener {

            val openURL = Intent(android.content.Intent.ACTION_VIEW)
            openURL.data = Uri.parse("https://www.privacypolicygenerator.info/live.php?token=PTh0EFU6RM2nIAJQlEtZ0PsxKFRW2Sn4")
            startActivity(openURL)
        }

        registerButton.setOnClickListener{
            validated = validateEntries()
            if(validated){

                // User creation
                auth.createUserWithEmailAndPassword(
                    emailInput.text.toString(),
                    password.text.toString()
                )
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val currentUser = Firebase.auth.currentUser

                            // Email verification
                            currentUser!!.sendEmailVerification()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Log.d(TAG, "Email sent.")
                                        writeData()
                                    }
                                }
                        } else {
                            Toast.makeText(baseContext, "Email already exists.", Toast.LENGTH_SHORT).show()
                        }
                    }
                // --> End of User creation
            }
        }
    }

    private fun validateEntries(): Boolean{
        // Name Validation

        if(surname.text.toString().isEmpty()) {
            SurSignUp.error = "Field required."
            surname.requestFocus()
            return false
        }

        if(firstName.text.toString().isEmpty()) {
            FirSignUp.error = "Field required."
            firstName.requestFocus()
            return false
        }

        if(middleInitial.text.toString().isEmpty()) {
            MNSignUp.error = "Field required."
            middleInitial.requestFocus()
            return false
        }

        // Username verification

        if(username.text.toString().isEmpty()) {
            userIDSignUp.error = "Field required."
            username.requestFocus()
            return false
        }

        // Permanent Address validation

        if(currentAddress.text.toString().isEmpty()) {
            currentAddressSignUpChild.error = "Field cannot be empty."
            currentAddress.requestFocus()
            return false
        }

        // Contact Number validation

        if(contactNumber.text.toString().isEmpty()) {
            numberSignUp.error = "Field cannot be empty."
            numberSignUpChild.requestFocus()
            return false
        }

        if(contactNumber.text.toString()[0] != '0' || contactNumber.text.toString()[1] != '9' || contactNumber.text.toString().length < 11) {
            numberSignUp.error = "Please follow format. Ex. 09258388857"
            numberSignUpChild.requestFocus()
            return false
        }

        // Email validation

        if(emailInput.text.toString().isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(emailInput.text.toString()).matches()){

        }else{
            emailSignUp.error = "Please enter valid email"
            emailInput.requestFocus()
            return false
        }

        // Password validation

        if(password.text.toString().length < 8){
            passSignUp.error = "Min. 8 characters"
            password.requestFocus()
            return false
        }

        if(conPassword.text.toString() != password.text.toString()){
            passSignUp.error = "Passwords do not match"
            conPassSignUp.error = "Passwords do not match"
            password.requestFocus()
            conPassword.requestFocus()
            return false
        }

        return true
    }

    private fun writeData() {
        // Create a new user
        val user = hashMapOf(
            "surname" to surname.text.toString(),
            "firstName" to firstName.text.toString(),
            "middleInitial" to middleInitial.text.toString(),
            "username" to username.text.toString(),
            "currentAddress" to currentAddress.text.toString(),
            "contactNumber" to contactNumber.text.toString()
        )

        // Add a new document with a generated ID
        db.collection("users").document(emailInput.text.toString())
            .set(user)
            .addOnSuccessListener {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                Toast.makeText(baseContext, "Sign-up successful.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(baseContext, "Sign-up failed. Try again.", Toast.LENGTH_SHORT).show()
            }
    }

}