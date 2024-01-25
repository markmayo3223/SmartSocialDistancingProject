package com.uccpe.fac19

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.fragment_home.*
import timber.log.Timber
import java.lang.Exception

class HomeFragment : Fragment() {

    private val pickerContent = registerForActivityResult(ActivityResultContracts.GetContent()){
        homeFragProfImage.setImageURI(it)
    }
    private var auth = Firebase.auth
    private var db = FirebaseFirestore.getInstance()
    private var storage = Firebase.storage
    private val currentUser = auth.currentUser
    private var name = ""
    private var email = ""
    private var contactCount = 0
    private var trackingCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeFragProfImage.setOnClickListener(){
            pickerContent.launch("image/*")
        }

        // Call system update
        updateSystem()

        contactCardView.setOnClickListener {
            activity?.let{
                val intent = Intent (it, ContactRecyclerActivity::class.java)
                it.startActivity(intent)
            }
        }

        tracking_card_view.setOnClickListener {
            activity?.let{
                val intent = Intent (it, TrackingRecyclerActivity::class.java)
                it.startActivity(intent)
            }
        }

        // When checked, it enables Text Input in the Home Fragment
        editSwitch.setOnClickListener{
            if(updateButton.isEnabled){
                updateButton.isEnabled = false
                fullNameEditChild.isEnabled = false
                contactNumEditChild.isEnabled = false
                currentAddressEditChild.isEnabled = false
            }else{
                updateButton.isEnabled = true
                fullNameEditChild.isEnabled = true
                contactNumEditChild.isEnabled = true
                currentAddressEditChild.isEnabled = true
            }
            updateSystem()
        }

        // When clicked, information provided will be written to Firebase Firestore
        updateButton.setOnClickListener{
            val string = fullNameEditChild.text
            var splitString = string?.split(" ")?.toMutableList()

            if (splitString != null) {
                // Handling long surnames separated by spaces
                // there's probably an easier way but I'm stupid
                // Please fix
                if(splitString.size > 3){
                    var concatSurname = ""
                    for((x, str) in splitString.withIndex()){
                        if(x > 1){
                            concatSurname = "$concatSurname$str "
                        }
                    }
                    splitString[2] = concatSurname

                    updateInfo(splitString)
                }else if(splitString.size == 3){
                    updateInfo(splitString)
                }else{
                    Toast.makeText(activity,"Follow full name format. Ex: Juan T. Dela Cruz",Toast.LENGTH_LONG).show()
                }

                fullNameEditChild.isEnabled = false
                contactNumEditChild.isEnabled = false
                currentAddressEditChild.isEnabled = false
                updateButton.isEnabled = false
                editSwitch.isChecked = false
            }else{
                Toast.makeText(activity,"Follow full name format. Ex: Juan T. Dela Cruz",Toast.LENGTH_LONG).show()
            }

            // Update FirebaseAuth details
            name = fullNameEditChild.text.toString()
            val profileUpdates = userProfileChangeRequest {
                displayName = name
            }

            currentUser!!.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("profUpdateSuccess", "User profile updated.")
                    }
                }
            // Call system update
            updateSystem()
        }
    }

    private fun updateSystem(){


            contactCount = 0
            trackingCount = 0
            // Get name and email from FirebaseAuth
            currentUser?.let {
                for (profile in it.providerData) {
                    name = profile.displayName.toString()
                    email = profile.email.toString()
                }
            }

            db.collection("users").document(email)
                .collection("logs")
                .document("contact")
                .collection("tracing")
                .get()
                .addOnSuccessListener { document ->
                    for(docs in document) {
                        try{
                            contactCount += 1
                            numContactText.text = contactCount.toString()
                        } catch (e: Exception) {
                            Log.d("HomeFragment", " $e")
                        }
                    }
                }

            db.collection("users").document(email)
                .collection("logs")
                .document("tracking")
                .collection("location")
                .get()
                .addOnSuccessListener { document ->
                    for(docs in document) {
                        try {
                            trackingCount += 1
                            numTrackingText.text = trackingCount.toString()
                        } catch (e: Exception) {
                            Log.d("HomeFragment", " $e")
                        }
                    }
                }


        // Get user information from Firebase Firestore
        val userData = db.collection("users").document(email)
        userData.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    try {
                        Log.d("DocRecoverySuccess", "DocumentSnapshot data: ${document.data}")
                        name = "${document.getString("firstName")} ${document.getString("middleInitial")} ${document.getString("surname")}"
                        emailDispText.text = email
                        usernameDispText.text = document.getString("username")
                        fullNameEditChild.setText("${document.getString("firstName")} ${document.getString("middleInitial")} ${document.getString("surname")}")
                        currentAddressEditChild.setText(document.getString("currentAddress"))
                        contactNumEditChild.setText(document.getString("contactNumber"))
                    } catch(e: Exception) {
                        Timber.d("Exception error: $e")
                    }


                } else {
                    Log.d("docRecNoExist", "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d("error", "get failed with ", exception)
            }
    }

    private fun updatePhoto(){

        var storageRef = storage.reference

        var imagesRef: StorageReference? = storageRef.child("images")
    }

    private fun updateInfo(splitString: MutableList<String>){
        val updatedInfo = hashMapOf(
            "firstName" to (splitString?.get(0) ?: "null"),
            "middleInitial" to (splitString?.get(1) ?: "null"),
            "surname" to (splitString?.get(2) ?: "null"),
            "contactNumber" to contactNumEditChild.text.toString(),
            "currentAddress" to currentAddressEditChild.text.toString()
        )

        db.collection("users").document(email)
            .set(updatedInfo, SetOptions.merge())
            .addOnSuccessListener { Log.d("overwriteSuccess", "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.w("overwriteFail", "Error writing document", e) }
    }
}
