package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceConstants
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.dp
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
const val GEOFENCE_RADIUS_IN_METERS = 100f
private const val TAG = "SaveReminderFragment"
private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val ACTION_GEOFENCE_EVENT = "SaveReminderFragment.project4.action.ACTION_GEOFENCE_EVENT"

class SaveReminderFragment : BaseFragment() {

    //Get the view model this time as a single to be shared with the another fragment

    override val viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var contxt: Context
    private var resultCode = 0
    private lateinit var geofencingClient: GeofencingClient

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(contxt, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(contxt, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q


    override fun onAttach(context: Context) {
        super.onAttach(context)
        contxt = context
    }

    override fun onResume() {
        super.onResume()
        contxt = requireContext()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("<<SaveRmndrFrag>>", "onCreateView: ")
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)
        geofencingClient = LocationServices.getGeofencingClient(contxt)
        binding.viewModel = viewModel

        return binding.root
    }
//------------------------------------------------------------------
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this
        Log.d("<<SaveRmndrFrag>>", "onViewCreated: ")

        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.toSelectLocationFragment())
        }
        Log.d("<<SaveRmndrFrag>>", "After binding.selectLocation ")
        ViewCompat.setElevation(binding.progressBar, 100.dp)

        binding.saveReminder.setOnClickListener {
            val title = viewModel.reminderTitle.value
            val description = viewModel.reminderDescription.value
            val poi = viewModel.selectedPlaceOfInterest.value
            val latitude = poi?.latLng?.latitude
            val longitude = poi?.latLng?.longitude
            val radius = viewModel.selectedRadius.value

            // Create a data base record.
            val reminderData = ReminderDataItem(
                    title,
                    description,
                    poi?.name,
                    latitude,
                    longitude,
                    radius)


            Log.d("<<SaveRmndrFrag>>", "setPermissions is Next,,,,, ")
            SetPermissions()
            Log.d("<<SaveRmndrFrag>>", "requestLocationPermissions is  Next,,,,, ")
            requestLocationPermissions()

            if (viewModel.validateAndSaveReminder(reminderData)) {
                Log.d("<<SaveRmndrFrag>>", "!BOOM! call addGeofence(reminderData) ")
                addGeofence(reminderData)
            }

            Log.d("<<SaveRmndrFrag>>", "title       =  " + title)
            Log.d("<<SaveRmndrFrag>>", "description =  " + description)
            Log.d("<<SaveRmndrFrag>>", "latitude       =  " + latitude)
            Log.d("<<SaveRmndrFrag>>", "longitude      =  " + longitude)
            Log.d("<<SaveRmndrFrag>>", "poi            =  " + poi)
            Log.d("<<SaveRmndrFrag>>", "radius         =  " + radius)

        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(reminderData: ReminderDataItem) {

        Log.d("<<SaveRmndrFrag>>", " ADDING A GEOFENCE " + reminderData.latitude)
        Log.d("<<SaveRmndrFrag>>", " ADDING A GEOFENCE " + reminderData.longitude)
        Log.d("<<SaveRmndrFrag>>", " ADDING A GEOFENCE " + reminderData.radius)

        // Building the geofence request
        // 3rd Step - Create and add geofences.
        //-------------------------------
        val geofence = Geofence.Builder()
            .setRequestId(reminderData.id)
            .setCircularRegion(
                reminderData.latitude!!,
                reminderData.longitude!!,
                reminderData.radius!!
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()
        //------------------------------


        // The INITIAL_TRIGGER_ENTER
        //------------------------------
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
        Log.d("<<SaveRmndrFrag>> ", "(INITIAL_TRIGGER_ENTER) GEOFENCE_TRANSITION_ENTER " + Geofence.GEOFENCE_TRANSITION_ENTER )
        Log.d("<<SaveRmndrFrag>> ", "(STEP 2) AFTER INITIAL_TRIGGER_ENTER " + GeofencingRequest.INITIAL_TRIGGER_ENTER )
        //------------------------------


        //Step 4 -  Defining a Pending Intent that starts a BroadCastReceiver
        //------------------------------
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        Log.d("<<SaveRmndrFrag>> ", "(STEP 3) AFTER INTENT/GEOBROADCASTRCVR " + intent)
        intent.action = GeofenceConstants.ACTION_GEOFENCE_EVENT
        Log.d("<<SaveRmndrFrag>> ", "(STEP 4) AFTER INTENT ACTIVE_GEOFENCE_EVENT" +
                GeofenceConstants.ACTION_GEOFENCE_EVENT)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        //------------------------------
        Log.d("<<SaveRmndrFrag>> ", "(STEP 5) PendingIntent.FLAG_UPDATE_CURRENT " + PendingIntent.FLAG_UPDATE_CURRENT)
       //?????? Left Off here....
        val geofencingClient = LocationServices.getGeofencingClient(requireContext())
        Log.d("<<SaveRmndrFrag>>", "(STEP 6) client LocationServices.getGeofencingClient " + geofencingClient)
        Log.d("<<SaveRmndrFrag>>", "(STEP 7) LISTENERS USING request & pendingIntent " + request  + pendingIntent)
        // Adds the geofence using addGeofences method using the request object
        // and the pending intent built prior to this,


        geofencingClient.addGeofences(request, geofencePendingIntent)?.run {
            addOnSuccessListener {
                Log.d("<<SaveRmndrFrag>>", "!!GEOFENCESADDED!! ")
                viewModel.geofenceActivated()
                Log.d("<<SaveRmndFrag>>", "Added geofence for reminder with id ${reminderData.id} successfully.")

            }
            addOnFailureListener {
                Log.d("<<SaveRmndrFrag>>", "!!GEOFENCESFAILED!!")
                viewModel.showErrorMessage.postValue(getString(R.string.error_adding_geofence))
                it.message?.let { message ->
                    Log.w("<<Save", message)
                }
            }
        }

        Log.d("<<SaveRmndrFrag>>", "(STEP 8) JUST AFTER ADDING GEOFENCES")
    }

    @TargetApi(29)
    private fun SetPermissions() :Boolean {
        Log.d("<<SaveRmndrFrag>>", "in SetPermissions(1) ")
        //-------------------------------------------//
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            contxt,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                )
        Log.d("<<SaveRmndrFrag>>", "in SetPermissions(2) " + Manifest.permission.ACCESS_FINE_LOCATION )
        Log.d("<<SaveRmndrFrag>>", "runningQOrLater(3)   " + runningQOrLater )

        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            contxt, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                Log.d("<<SaveRmndrFrag>>", "(4)else "   )
                true
            }
        Log.d("<<SaveRmndrFrag>>", "in SetPermissions(5) " + Manifest.permission.ACCESS_BACKGROUND_LOCATION )
        Log.d("<<SaveRmndrFrag>>", "forgroundLocationApproved(6)    " + foregroundLocationApproved )
        Log.d("<<SaveRmndrFrag>>", "backgroundPermissionApproved(7) " + backgroundPermissionApproved )

        return foregroundLocationApproved && backgroundPermissionApproved

    }
        //------------------------------------------//

  @TargetApi(29)
  private fun requestLocationPermissions() {
        Log.d("<<SaveRmndrFrag>>", " in requestLocationPermissions(1) ")
        //This provides the result[LOCATION_PERMISSION_INDEX]
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        Log.d(
          "<<SaveRmndrFrag>>",
          " in requestLocationPermissions(2) " +
                  Manifest.permission.ACCESS_FINE_LOCATION
      )
        resultCode = when {
            runningQOrLater -> {
                // this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
                Log.d("<<SaveRmndrFrag>>", "[BACKGROUND_LOCATION_PERMISSION_INDEX] " + REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE)
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }

            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        Log.d("<<SaveRmndrFrag>>", "Request foreground only location permission")
        requestPermissions(
            permissionsArray,
            resultCode
        )

    }




    override fun onDestroy() {
        super.onDestroy()
        Log.d("<<SaveRmndrFrag>> ", "onDestroy ")
        //make sure to clear the view model after destroy, as it's a single view model.
        viewModel.onClear()
    }
}
