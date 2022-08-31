package com.udacity.project4.locationreminders.savereminder.selectreminderlocation
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import androidx.navigation.fragment.DialogFragmentNavigatorDestinationBuilder
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.geofence.GeofenceConstants
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.LocationUtils
import com.udacity.project4.utils.PermissionsResultEvent
import com.udacity.project4.utils.toLatLng
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.log

private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {


    //Use Koin to get the view model of the SaveReminder
    override val viewModel: SaveReminderViewModel by inject()

    private val selectLocationViewModel: SelectLocationViewModel by viewModel()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var selectedLocationMarker: Marker
    private lateinit var selectedLocationCircle: Circle
    private val REQUEST_LOCATION_PERMISSION = 1

    private val DEFAULT_ZOOM = 17f
    private val defaultLocation = LatLng(44.97974325243857, 10.709856046507827)
    private lateinit var lastKnownLocation: Location
    private lateinit var selectedMarker: Marker



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        Log.d("<<SelectLocFragment>>", "onCreateView: ")
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        //binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        binding.onSaveButtonClicked = View.OnClickListener { onLocationSelected() }
        binding.viewModel = selectLocationViewModel


        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)




        checkDeviceLocationSettings()

        val mapFragment = childFragmentManager.findFragmentById(R.id.fragment_container) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.saveButton.setOnClickListener {
            onLocationSelected()
        }


        setupGoogleMap()

        return binding.root
    }

    private fun setupGoogleMap() {
        Log.d("<<SelectLocFragment>>", "setupGoogleMap: ")
        val mapFragment = childFragmentManager
            .findFragmentByTag(getString(R.string.map_fragment)) as? SupportMapFragment
            ?: return


        selectLocationViewModel.radius.observe(viewLifecycleOwner) {
            if (!::selectedLocationCircle.isInitialized) {
                return@observe
            }

            selectedLocationCircle.radius =
                it?.toDouble() ?: GeofenceConstants.DEFAULT_RADIUS_IN_METRES.toDouble()
        }

        selectLocationViewModel.selectedLocation.observe(viewLifecycleOwner) {
            selectedLocationMarker.position = it.latLng
            selectedLocationCircle.center = it.latLng
            setCameraTo(it.latLng)
        }

        mapFragment.getMapAsync(this)
    }

    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        Log.d("<<SelectLocFrag>> ", "getDeviceLocation: ")

        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        try {
            if (isPermissionGranted()) {
                map.isMyLocationEnabled = true
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        if (task.result != null) {
                            lastKnownLocation = task.result!!
                            map?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    ),
                                    DEFAULT_ZOOM.toFloat()
                                )
                            )
                        }
                    } else {
                        Log.d("<<SelectLocFrag>>", "Current location is null. Using defaults.")
                        Log.e("<<SelectLocFrag>>", "Exception: %s", task.exception)
                        map?.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat())
                        )
                        map?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            } else {
                requestPermissions(
                    arrayOf<String>(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION
                )
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun onLocationSelected() {
        Log.d("<<SelectLocFragment>>", "(onLocationSelected:) ")
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
        selectLocationViewModel.closeRadiusSelector()
        viewModel.setSelectedLocation(selectLocationViewModel.selectedLocation.value!!)
        viewModel.setSelectedRadius(selectLocationViewModel.radius.value!!)
        viewModel.navigationCommand.postValue(NavigationCommand.Back)

    }


    private fun checkDeviceLocationSettings(resolve: Boolean = true) {

        Log.d("<<SelectLocFrag>>", "checkDeviceLocationSettings: ")

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        activity,
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("<<SelectLocFrag>>", "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    this.requireView(),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                }.show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("<<SelectLocFrag>>", "onRequestPermissionsResult: ")
            getDeviceLocation()
        } else {
            Snackbar.make(
                binding.selectLocationFragment,
                R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
            ).setAction(android.R.string.ok) {
                requestPermissions(
                    arrayOf<String>(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION
                )
            }.show()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        selectLocationViewModel.closeRadiusSelector()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        fun setMapType(mapType: Int): Boolean {
            map.mapType = mapType
            return true
        }

        return when (item.itemId) {
            R.id.normal_map -> setMapType(GoogleMap.MAP_TYPE_NORMAL)
            R.id.hybrid_map -> setMapType(GoogleMap.MAP_TYPE_HYBRID)
            R.id.terrain_map -> setMapType(GoogleMap.MAP_TYPE_TERRAIN)
            R.id.satellite_map -> setMapType(GoogleMap.MAP_TYPE_SATELLITE)

            else -> false
        }
    }
// Left here researching the onMap ready function below. It's use ....
// What's the reason for this and how is used. It appears it's not being used.

    override fun onMapReady(map: GoogleMap?) {
        this.map = map!!
        Log.d("<<SelectLocFrag>>", "onMapReady: ")
        map.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                requireContext(),
                R.raw.map_style
            )
        )

        val markerOptions = MarkerOptions()
            .position(map?.cameraPosition.target)
            .title(getString(R.string.dropped_pin))
            .draggable(true)

        selectedLocationMarker = map.addMarker(markerOptions)

        val circleOptions = CircleOptions()
            .center(map?.cameraPosition.target)
            .fillColor(ResourcesCompat.getColor(resources, R.color.map_radius_fill_color, null))
            .strokeColor(ResourcesCompat.getColor(resources, R.color.map_radius_stroke_color, null))
            .strokeWidth(4f)
            .radius(GeofenceConstants.DEFAULT_RADIUS_IN_METRES.toDouble())


        selectedLocationCircle = map.addCircle(circleOptions)


        viewModel.selectedPlaceOfInterest.value.let {
            selectLocationViewModel.setSelectedLocation(
                it ?: PointOfInterest(map.cameraPosition.target, null, null)
            )

            if (it == null) {
                Log.d("<<SelectLocFrag>>", "startAtCurrentLocation() ")
                startAtCurrentLocation()
            }
        }

        map.setOnMapClickListener {
            if (selectLocationViewModel.isRadiusSelectorOpen.value == true) {
                selectLocationViewModel.closeRadiusSelector()
            } else {
                Log.d("<<SelectLocFrag>>", "setSelectedLocation(it) ")
                selectLocationViewModel.setSelectedLocation(it)
            }
        }

        map.setOnPoiClickListener {
            if (selectLocationViewModel.isRadiusSelectorOpen.value == true) {
                selectLocationViewModel.closeRadiusSelector()
            } else {
                Log.d("<<SelectLocFrag>>", "onMapReady: ")
                selectLocationViewModel.setSelectedLocation(it)
            }
        }

        map.setOnCameraMoveListener {
            selectLocationViewModel.zoomSize = map.cameraPosition.zoom
        }

    }

    private fun locationPermissionHandler(event: PermissionsResultEvent, handler: () -> Unit) {
        if (event.areAllGranted) {
            handler()
            return
        }

        if (event.shouldShowRequestRationale) {
            viewModel.showSnackBar.postValue(getString(R.string.permission_denied_explanation))
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAtCurrentLocation() {
        Log.d("<<SelectlocFrag>>", "startAtCurrentLocation: ")
        if (!LocationUtils.hasLocationPermissions()) {
            LocationUtils.requestPermissions {
                locationPermissionHandler(it, this::startAtCurrentLocation)
            }

            return
        }

        fun resetToCurrentLocation() =
            LocationUtils.requestSingleUpdate {
                Log.d("<<SelectLocFrag>>", "resetToCurrentLocation: ")
                selectLocationViewModel.setSelectedLocation(it.toLatLng())
            }

        map.isMyLocationEnabled = true

        map.setOnMyLocationButtonClickListener {
            Log.d("<<Selectlocfrag>>", "startAtCurrentLocation: ")
            selectLocationViewModel.closeRadiusSelector()
            resetToCurrentLocation()
            true
        }

        resetToCurrentLocation()
    }


    private fun setCameraTo(latLng: LatLng) {
        Log.d("<<SelectLocFragment>>", "setCameraTo: ")
        val cameraPosition =
            CameraPosition.fromLatLngZoom(latLng, selectLocationViewModel.zoomSize)
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)

        map.animateCamera(cameraUpdate)
    }

    private fun isPermissionGranted(): Boolean {
        return context?.let {
            ContextCompat.checkSelfPermission(
                it,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        } == PackageManager.PERMISSION_GRANTED
    }


}
