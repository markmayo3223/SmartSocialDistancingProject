package com.uccpe.fac19

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.uccpe.fac19.Constants.ACTION_START_OR_RESUME_SERVICE
import com.uccpe.fac19.Constants.FASTEST_INTERVAL
import com.uccpe.fac19.Constants.INTERVAL
import timber.log.Timber

open class MapsFragment : Fragment(), OnMapReadyCallback{

    private var auth = Firebase.auth
    private var db = FirebaseFirestore.getInstance()
    private val currentUser = auth.currentUser
    private var email = ""
    private var username = ""
    private var results = FloatArray(1)


    private var mMap: GoogleMap? = null
    private var lastKnownLocation: Location? = null
    private lateinit var currentMarker: Marker

    private lateinit var locationRequest: LocationRequest
    private lateinit var builder: LocationSettingsRequest.Builder
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var requestingLocationUpdates = false
    private var locationPermissionGranted = false
    private val positions = ArrayList<LatLng>()
    private val usernames = ArrayList<String>()
    private val markers = ArrayList<Marker>()

    private var isLocate = false

    private fun postInitialValues() {
        currentUser?.let {
            for (profile in it.providerData) {
                email = profile.email.toString()
            }
        }

        db.collection("users").document(email).get()
            .addOnSuccessListener {document ->
                username = document.get("username") as String
                Log.d("usernameGet", "$username")
            }
    }

    private fun databaseListener() {
        db.collection("users").whereNotEqualTo("username", username).whereEqualTo("isTracking", true)
            .get()
            .addOnSuccessListener { document ->
                emptyValuesOnChange()
                var i = 0 // Indexing variable
                for (docs in document) {
                    usernames.add(docs.get("username") as String)
                    val pos = LatLng(
                        docs.get("Lat") as Double,
                        docs.get("Lng") as Double
                    )
                    positions.add(pos)

                    i += 1
                }
            }

    }

    private fun placeUsersMarkers() {
        for(i in positions.indices) {
            mMap?.addMarker(MarkerOptions()
                .position(positions[i])
                .title(usernames[i]))?.let { markers.add(it) }
        }
    }

    private fun removeUserMarkers() {
        try {
            for(i in markers.indices) {
                markers[i].remove()
            }
        } catch (e: Exception) {
            Log.d("MapsFragment", "$e")
        }

    }

    private fun emptyValuesOnChange() {
        positions.apply {
            removeAll(this)
        }
        usernames.apply {
            removeAll(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState?.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,requestingLocationUpdates)
        super.onSaveInstanceState(outState)
    }

    private fun updateValuesFromBundle(savedInstanceState: Bundle?) {
        savedInstanceState ?: return

        // Update the value of requestingLocationUpdates from the Bundle.
        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            requestingLocationUpdates = savedInstanceState.getBoolean(
                REQUESTING_LOCATION_UPDATES_KEY)
        }

        updateMap(lastKnownLocation, mMap)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateValuesFromBundle(savedInstanceState)

        val mMapView = requireActivity().findViewById<MapView>(R.id.mapView)

        postInitialValues()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    lastKnownLocation = location
                    updateMap(lastKnownLocation, mMap)
                    databaseListener()
                    removeUserMarkers()
                    placeUsersMarkers()
                }
            }

        }


        mMapView.onCreate(savedInstanceState)
        mMapView.onResume()
        mMapView.getMapAsync(this)
    }

    override fun onMapReady(p0: GoogleMap?) {
        this.mMap = p0
        p0!!.mapType = GoogleMap.MAP_TYPE_TERRAIN

        p0!!.moveCamera((CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM)))

        getLocationPermission()

        checkDeviceLocationSettings()

        getDeviceLocation(mMap)
    }

    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */

        if (activity?.applicationContext?.let {
                ContextCompat.checkSelfPermission(
                    it,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
            == PackageManager.PERMISSION_GRANTED) {
            if(!isLocate){
                isLocate = true
                sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
            }
            requestingLocationUpdates = true
            locationPermissionGranted = true
        } else {
            activity?.let {
                ActivityCompat.requestPermissions(
                    it, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), LocationService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    private fun checkDeviceLocationSettings(resolve:Boolean = true) {
        locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = INTERVAL
            fastestInterval = FASTEST_INTERVAL
        }
        builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                try {
                    exception.startResolutionForResult(requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(requireView(),
                    "Location Needed", Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                    // startLocationUpdates()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {

            }
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private fun getDeviceLocation(p0: GoogleMap?) {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationClient.lastLocation
                locationResult.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        Log.d("SUCCESSFUL LOCATION", "Initial")
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            var myPos = LatLng(lastKnownLocation!!.latitude,
                                lastKnownLocation!!.longitude)

                            currentMarker = p0?.addMarker(MarkerOptions().position(myPos)
                                .title("User Position")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))!!

                            p0?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    myPos, DEFAULT_ZOOM.toFloat()))
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        mMap?.moveCamera(
                            CameraUpdateFactory
                            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat()))
                        mMap?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    override fun onResume() {
        super.onResume()
        if (locationPermissionGranted){
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
      //  activity?.unregisterReceiver(brReceiver)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if(locationPermissionGranted){
            fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper())
        } else {
            Toast.makeText(activity,"Cannot start location updates. Please enable Location permissions.", Toast.LENGTH_SHORT)
            getLocationPermission()
            checkDeviceLocationSettings()
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateMap(location: Location?, p0: GoogleMap?) {
        if(location != null){
            try {
                val latLng = LatLng(location.latitude, location.longitude)
                currentMarker.position = latLng
                mMap!!.animateCamera((CameraUpdateFactory.newLatLng(latLng)))
            } catch (e: Exception) {
                mMap!!.animateCamera((CameraUpdateFactory.newLatLng(defaultLocation)))
                Timber.d("noCurrentLocationSource: Reload the map")
            }
        }
    }

    companion object {
        private const val REQUESTING_LOCATION_UPDATES_KEY = "Requesting Location Updates"
        private val TAG = DashboardActivity::class.java.simpleName
        private const val DEFAULT_ZOOM = 18f
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        const val REQUEST_TURN_DEVICE_LOCATION_ON = 99
        private val defaultLocation = LatLng(16.4096, 120.5937)

        // Keys for storing activity state.
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
    }
}


