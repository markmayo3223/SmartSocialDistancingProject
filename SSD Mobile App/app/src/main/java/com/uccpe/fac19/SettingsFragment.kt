package com.uccpe.fac19

import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.coroutines.Dispatchers.Main
import timber.log.Timber

class SettingsFragment : Fragment() {
    private var auth = Firebase.auth
    private val TAG = "SettingsFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        serviceSwitch.setOnClickListener {
            if(serviceSwitch.isChecked) {
                sendCommandToService(Constants.ACTION_PAUSE_SERVICE)
            } else {
                sendCommandToService(Constants.ACTION_START_OR_RESUME_SERVICE)
            }

        }

        tos_Button.setOnClickListener {
            val openURL = Intent(android.content.Intent.ACTION_VIEW)
            openURL.data = Uri.parse("https://www.termsofservicegenerator.net/live.php?token=Bjivi5z9CW44D9QW9ltkCa0rPfGSUnXB")
            startActivity(openURL)
        }

        pp_Button.setOnClickListener {
            val openURL = Intent(android.content.Intent.ACTION_VIEW)
            openURL.data = Uri.parse("https://www.privacypolicygenerator.info/live.php?token=PTh0EFU6RM2nIAJQlEtZ0PsxKFRW2Sn4")
            startActivity(openURL)
        }

        logoutButton.setOnClickListener {
            if(LocationService.isLocate.value == true) {
                sendCommandToService(Constants.ACTION_PAUSE_SERVICE)
            }

            auth.signOut()
            activity?.finish()
            activity?.let{

                val intent = Intent (it, LoginActivity::class.java)
                it.startActivity(intent)
            }

        }
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), LocationService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }
}