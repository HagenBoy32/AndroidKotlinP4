package com.udacity.project4.locationreminders.geofence

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private val remindersLocalRepository by inject<ReminderDataSource>()

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    companion object {
        private const val JOB_ID = 573

        //        TODO: call this to start the JobIntentService to handle the geofencing transition events
        fun enqueueWork(context: Context, intent: Intent) {
            Log.d("<<GeoFncetransjob>>", "enqueueWork: ")
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }

    override fun onHandleWork(intent: Intent) {

        Log.d("<<GeoFnceTransjob", "onHandleWork: ")

        val event = GeofencingEvent.fromIntent(intent)

        Log.d("<<GeoFnceTransjob>>", "event.errorCode " + event.errorCode)

        if (event.hasError()) {
            Log.e(TAG, "Error with event: ${event.errorCode}")
            return
        }

        Log.d("<<GeoFnceTransjob>>", "event.geofenceTransition " + event.geofenceTransition )

        Log.d("<<GeoTransJobInt>> ", "Geofence.GEOFENCE_TRANSITION_ENTER" + Geofence.GEOFENCE_TRANSITION_ENTER)
        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d("<<GeoFenceTrans>>", getString(R.string.geofence_entered))
            event.triggeringGeofences.forEach(::sendNotification)
        }
    }

    //TODO: get the request id of the current geofence
    private fun sendNotification(geofence: Geofence) {
        Log.d("<<GeoTransJobIntent>>", "sendNotification: ")
        // Interaction to the repository has to be through a coroutine scope
        CoroutineScope(coroutineContext).launch(SupervisorJob()) {
            //get the reminder with the request id
            val result = remindersLocalRepository.getReminder(geofence.requestId)
            Log.d("<<GeoFenceTrans>>", "sendNotification: ")
            if (result is Result.Success<ReminderDTO>) {
                val reminderDTO = result.data
                //send a notification to the user with the reminder details
                sendNotification(
                    this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                        reminderDTO.title,
                        reminderDTO.description,
                        reminderDTO.location,
                        reminderDTO.latitude,
                        reminderDTO.longitude,
                        reminderDTO.radius,
                        reminderDTO.id
                    )
                )
            }
        }
    }

}
