package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.launch
import kotlin.math.log

class SaveReminderViewModel(val app: Application, val dataSource: ReminderDataSource) :
    BaseViewModel(app) {

    val reminderTitle = MutableLiveData<String>()
    val reminderDescription = MutableLiveData<String>()
    private val _selectedPlaceOfInterest = MutableLiveData<PointOfInterest>()
    private val _selectedRadius = MutableLiveData<Float>()
    //private val _geofenceIndex = state.getLiveData(GEOFENCE_INDEX_KEY, -1)
    //private val _hintIndex = state.getLiveData(HINT_INDEX_KEY, 0)
    val selectedPlaceOfInterest: LiveData<PointOfInterest>
        get() = _selectedPlaceOfInterest

    val selectedRadius: LiveData<Float>
        get() = _selectedRadius

    val selectedPlaceOfInterestName = Transformations.map(_selectedPlaceOfInterest) {
        if (it == null) {
            return@map app.getString(R.string.select_location)
        }

        if (it.name.isNullOrBlank()) {
            return@map "Lat: ${it.latLng.latitude} Lon: ${it.latLng.longitude}"
        }
        Log.d("<<SaveReminderVM>>", "_selectedPlaceOfInterest " + _selectedPlaceOfInterest)
        it.name.replace("\n", "").trim()
    }


    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    fun onClear() {
        Log.d("<<SaveReminder>> ", "onClear: ")
        reminderTitle.value = null
        reminderDescription.value = null
        _selectedPlaceOfInterest.value = null

    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder(reminderData: ReminderDataItem):Boolean {
        Log.d("<<SaveRmndrVM", "validateAndSaveReminder: ")
        if (validateEnteredData(reminderData)) {
            Log.d("<<SaveReminderVM>> ", "BEFORE validateAndSaveReminder: ")
            saveReminder(reminderData)
            Log.d("<<SaveReminderVM>> ", "AFTER validateAndSaveReminder: return true ")
            return true
        }
        return  false
    }

    /**
     * Save the reminder to the data source
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        Log.d("<<SaveReminderVM>>", "reminderData.location = " + reminderData.printableLocation )
        showLoading.value = true
        reminderData.location = "test"
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.radius,
                    reminderData.id
                )
            )
            Log.d("<<SavermndrVM>> ", "!!saveReminder!! ")
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
            navigationCommand.value = NavigationCommand.Back
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        Log.d("<<SaveRmndrVM", " in validateEnteredData: ")
        if (reminderData.title.isNullOrEmpty()) {
            Log.d("<<SaveRmndrVM>>", "reminder.title is null " + reminderData.title )
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.latitude == null || reminderData.longitude == null) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }

        if (reminderData.radius == null) {
            showSnackBarInt.value = R.string.err_select_radius
            return false
        }
              return true
    }


    fun setSelectedLocation(placeOfInterest: PointOfInterest) {
        _selectedPlaceOfInterest.postValue(placeOfInterest)
    }

    fun setSelectedRadius(radius: Float) {
        _selectedRadius.postValue(radius)
    }

    fun geofenceActivated() {
      // _geofenceIndex.value = _hintIndex.value
        Log.d("<<SaveRmndrViewModel>>", "geofenceActivated: ")
    }

}

private const val HINT_INDEX_KEY = "hintIndex"
private const val GEOFENCE_INDEX_KEY = "geofenceIndex"