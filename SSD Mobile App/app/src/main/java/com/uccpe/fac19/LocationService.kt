package com.uccpe.fac19

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.uccpe.fac19.Constants.ACTION_PAUSE_SERVICE
import com.uccpe.fac19.Constants.ACTION_SHOW_MAPS_FRAGMENT
import com.uccpe.fac19.Constants.ACTION_START_OR_RESUME_SERVICE
import com.uccpe.fac19.Constants.ACTION_STOP_SERVICE
import com.uccpe.fac19.Constants.NOTIFICATION_CHANNEL_ID
import com.uccpe.fac19.Constants.NOTIFICATION_CHANNEL_NAME
import com.uccpe.fac19.Constants.NOTIFICATION_ID
import com.uccpe.fac19.Constants.WARNING_MESSAGE
import com.uccpe.fac19.Constants.WARNING_NOTIFICATION_CHANNEL_ID
import com.uccpe.fac19.Constants.WARNING_NOTIFICATION_CHANNEL_NAME
import com.uccpe.fac19.Constants.WARNING_NOTIFICATION_ID
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class LocationService : LifecycleService() {
    private val TAG = "LocationService"
    private var auth = Firebase.auth
    private var db = FirebaseFirestore.getInstance()
    private val currentUser = auth.currentUser
    private var lastKnownLocation: Location? = null
    private var lastKnownTimestamp : Long? = null

    var email = ""
    var username = ""
    var isFirstRun = true
    var results = FloatArray(1) // Cache for the computed distance from another user

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var mfusedLocationProviderClient: FusedLocationProviderClient

    companion object {
        val isLocate = MutableLiveData<Boolean>()
        var myPos = LatLng(0.0, 0.0)
        val userNames = ArrayList<String>()
        var timeDiffs = ArrayList<Long>()
        var timeThens = ArrayList<Long>()
        var ret = false
        val positions = ArrayList<LatLng>()
        val distances = ArrayList<Float>()
    }

    @SuppressLint("VisibleForTests")
    override fun onCreate() {
        super.onCreate()
        postInitialValues()
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        mfusedLocationProviderClient = FusedLocationProviderClient(this)
        updateTrackingStatus(ACTION_START_OR_RESUME_SERVICE)
        isLocate.observe(this, {
            updateLocationTracking(it)
        })
    }

    private fun postInitialValues() {
        currentUser?.let {
            for (profile in it.providerData) {
                email = profile.email.toString()
            }
        }

        db.collection("users").document(email).get()
            .addOnSuccessListener { document ->
                username = document.get("username") as String
                Log.d("usernameGet", "$username")
            }

        updateTrackingStatus(ACTION_START_OR_RESUME_SERVICE)
        isLocate.postValue(false)
    }

    private fun setMyPos(location: Location) {
        location.let {
            val pos = LatLng(location.latitude, location.longitude)
            myPos = pos
        }
    }

    private fun getTime(ms: Long) : String {
        val sdf = SimpleDateFormat("HH:mm:ss.ssss")
        return sdf.format(ms)
    }

    private fun listenAndBroadcastData() {
        db.collection("users").whereNotEqualTo("username", username)
            .whereEqualTo("isTracking", true)
            .get()
            .addOnSuccessListener { document ->
                emptyValuesOnChange()
                var i = 0 // Indexing variable
                for (docs in document) {
                    userNames.add(docs.get("username") as String)
                    val pos = LatLng(
                        docs.get("Lat") as Double,
                        docs.get("Lng") as Double
                    )
                    positions.add(pos)

                    // Calculating distance from other users
                    Location.distanceBetween(
                        myPos.latitude,
                        myPos.longitude,
                        positions[i].latitude,
                        positions[i].longitude, results
                    )

                    distances.add(results[0])
                    i += 1
                }

            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkDistancesIfValid() {
        for(i in distances.indices) {
            if(distances[i] < 1.0) {
                //sendNotification()

                if(checkIfNotRedundant(userNames[i])){
                    Log.d("LocationService", "${distances[i]}")

                    sendWarningNotification()

                    val sdf = SimpleDateFormat("dd/MM/yy")
                    val date = sdf.format(System.currentTimeMillis())

                    val updatedInfo = hashMapOf(
                        "Lat" to myPos.latitude,
                        "Lng" to myPos.longitude,
                        "contactUsername" to userNames[i],
                        "date" to date,
                        "msTime" to lastKnownLocation?.time?.let { getTime(it) },
                        "location" to getCompleteAddress(myPos.latitude, myPos.longitude)
                    )


                    lastKnownLocation?.time?.let { getTime(it) }?.let {
                        db.collection("users").document(email)
                            .collection("logs")
                            .document("contact")
                            .collection("tracing")
                            .document(it)
                            .set(updatedInfo, SetOptions.merge())
                            .addOnSuccessListener { Log.d("contactWriteSuccess", "NEW WRITE") }
                            .addOnFailureListener { e -> Timber.w("Error writing: $e, contactWriteFail") }
                    }
                }

            }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun checkIfNotRedundant(us: String) : Boolean {
        //16.4076979544, 120.598843816
        //16.408229, 120.598973
        val sdf = SimpleDateFormat("dd/MM/yy")
        val date = sdf.format(System.currentTimeMillis())
        ret = false

        db.collection("users").document(email)
            .collection("logs")
            .document("contact")
            .collection(date).whereEqualTo("contactUsername", us)
            .get()
            .addOnSuccessListener { document ->
                for(docs in document) {
                    var timeNow = lastKnownLocation?.time
                    var timeThen: Long? = docs.get("msTime") as Long?
                    var timeDiff = timeThen?.let { timeNow?.minus(it) }
                    if (timeThen != null) {
                        timeThens.add(timeThen)
                    }
                    if (timeDiff != null && timeThen != null) {
                        timeDiffs.add(timeDiff)
                        Log.d("LocationServiceAdd", "$timeDiffs")
                    }
                }
            }


        if(timeThens.isEmpty()) {
            ret = true
        }

        timeThens.apply {
            removeAll(this)
        }

        Log.d("LocationServiceDelete", "$ret $timeDiffs")
        // Checking if last write is done 15 minutes ago
        for(i in timeDiffs.indices) {
            ret = timeDiffs[i] > 900000
            Log.d("LocationServiceCheck", "$ret $us $timeDiffs")
        }
        Log.d("LocationServiceDone", "$ret $us $timeDiffs")

        // Empty values upon finishing logic
        if(timeDiffs.isNotEmpty()) {
            timeDiffs.apply {
                removeAll(this)
            }
        }
        return ret
    }

    private fun emptyValuesOnChange() {
        userNames.apply {
            removeAll(this)
        }
        positions.apply {
            removeAll(this)
        }
        distances.apply {
            removeAll(this)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    updateTrackingStatus(ACTION_START_OR_RESUME_SERVICE)
                    if (isFirstRun) {
                        startForegroundService()
                        //createAnotherNotificationChannel()
                        isFirstRun = false
                    } else {
                        Timber.d("Resuming service...")
                        startForegroundService()
                    }

                }
                ACTION_PAUSE_SERVICE -> {
                    pauseService()
                    updateTrackingStatus(ACTION_PAUSE_SERVICE)
                    Timber.d("ACTION_PAUSE_SERVICE")
                }
                ACTION_STOP_SERVICE -> {
                    Timber.d("ACTION_STOP_SERVICE")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    val locationCallback = object : LocationCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            if(isLocate.value!!) {
                result?.locations?.let { locations ->
                    for(location in locations) {
                        lastKnownLocation = location
                        setMyPos(location)
                        var time = getTime(location.time)
                        Timber.d("NEW LOCATION: ${location.latitude}, ${location.longitude}")
                        myPos = LatLng(location.latitude, location.longitude)
                        val updatedInfo = hashMapOf(
                            "Lat" to location.latitude,
                            "Lng" to location.longitude
                        )

                        db.collection("users")
                            .document(email)
                            .set(updatedInfo, SetOptions.merge())
                            .addOnSuccessListener { Timber.d("5minUpdateWriteSuccess") }
                            .addOnFailureListener { e -> Timber.w("Error writing: $e") }

                        listenAndBroadcastData()
                        checkDistancesIfValid()
                    }
                }
            }
        }
    }

    val mlocationCallback = object : LocationCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            if(isLocate.value!!) {
                result?.locations?.let { locations ->
                    for(location in locations) {

                        val sdf = SimpleDateFormat("dd/MM/yy")
                        val date = sdf.format(System.currentTimeMillis())

                        val updatedInfo = hashMapOf(
                            "Lat" to location.latitude,
                            "Lng" to location.longitude,
                            "date" to date,
                            "time" to getTime(location.time),
                            "location" to getCompleteAddress(location.latitude, location.longitude)
                        )

                        db.collection("users").document(email)
                            .collection("logs")
                            .document("tracking")
                            .collection("location")
                            .document(getTime(location.time))
                            .set(updatedInfo, SetOptions.merge())
                            .addOnSuccessListener { Log.d(TAG, "5-minute update new") }
                            .addOnFailureListener { e -> Log.w(
                                "overwriteFail",
                                "Error writing document",
                                e
                            ) }
                    }
                }
            }
        }
    }

    private fun getCompleteAddress(latitude: Double, longitude: Double): String {
        var strAdd = ""
        var geocoder : Geocoder

        geocoder = Geocoder(this, Locale.getDefault())

        try{
            var addresses = geocoder.getFromLocation(latitude, longitude, 1) as ArrayList<Address>
            return addresses.get(0).getAddressLine(0)
        } catch(e: Exception) {
            Log.d(TAG, "$e")
        }

        return "Google Location failed. Address not found."
    }


    private fun updateTrackingStatus(action: String) {
        if(action == ACTION_START_OR_RESUME_SERVICE) {
            val updatedInfo = hashMapOf(
                "isTracking" to true
            )

            db.collection("users")
                .document(email)
                .set(updatedInfo, SetOptions.merge())
                .addOnSuccessListener { Log.d("LocationService", "isTracking == TRUE") }
                .addOnFailureListener { e -> Timber.w("Error writing: $e") }

        } else {
            val updatedInfo = hashMapOf(
                "isTracking" to false
            )

            db.collection("users")
                .document(email)
                .set(updatedInfo, SetOptions.merge())
                .addOnSuccessListener { Log.d("LocationService", "isTracking == FALSE") }
                .addOnFailureListener { e -> Timber.w("Error writing: $e") }
        }

    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("WrongConstant")
    private fun pauseService() {
        isLocate.postValue(false)
        stopForeground(NOTIFICATION_ID)
    }


    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean) {
        if(isTracking) {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = Constants.INTERVAL
                fastestInterval = Constants.FASTEST_INTERVAL
            }
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            val mlocationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 300000
                fastestInterval = 300000
            }
            mfusedLocationProviderClient.requestLocationUpdates(
                mlocationRequest,
                mlocationCallback,
                Looper.getMainLooper()
            )
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            mfusedLocationProviderClient.removeLocationUpdates(mlocationCallback)
        }
    }

    private fun startForegroundService() {

        isLocate.postValue(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        val notificationBuilder =  NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_fac19_generic)
            .setContentTitle("SDMASCTO App Running")
            .setContentText("Tracking Location.")
            .setContentIntent(getDashboardActivityPendingIntent())

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun getDashboardActivityPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, DashboardActivity::class.java).also {
            it.action = ACTION_SHOW_MAPS_FRAGMENT
        },
        FLAG_UPDATE_CURRENT
    )

    override fun onDestroy() {
        super.onDestroy()
        updateTrackingStatus(ACTION_STOP_SERVICE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_HIGH
        )

        notificationManager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createAnotherNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            WARNING_NOTIFICATION_CHANNEL_ID,
            WARNING_NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_HIGH
        )

        notificationManager.createNotificationChannel(channel)
    }

    private fun sendWarningNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createAnotherNotificationChannel(notificationManager)
        }

        val notificationBuilder =  NotificationCompat.Builder(this, WARNING_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_fac19_generic)
            .setContentTitle("SDMASCTO Warning")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(WARNING_MESSAGE))



        with(NotificationManagerCompat.from(this)) {
            notify(WARNING_NOTIFICATION_ID, notificationBuilder.build())
        }

    }


}
