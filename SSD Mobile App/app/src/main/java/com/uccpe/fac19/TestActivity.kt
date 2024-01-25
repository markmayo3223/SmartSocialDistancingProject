package com.uccpe.fac19

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_test.*
import timber.log.Timber

class TestActivity: AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    var string = ""
    var x = 0
    var users = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        auth = Firebase.auth
        val currentUser = auth.currentUser

        //getDistances()
        textView2.text = string
        for(us in users) {
            Log.d("nice", "$us")
        }

        Timber.d("user size alright ${users.size}")
    }

    /*private fun getDistances() {
        db.collection("users").whereEqualTo("isTracking", true)
            .get()
            .addOnSuccessListener { document ->
                for(docs in document) {
                    LocationService.userNames.value?.add(docs.id)
                    LocationService.positions.value?.add(docs.get("LatLng") as LatLng)
                    Timber.d("${LocationService.userNames.value} ${LocationService.positions.value}")
                }
            }
    }
    */

}