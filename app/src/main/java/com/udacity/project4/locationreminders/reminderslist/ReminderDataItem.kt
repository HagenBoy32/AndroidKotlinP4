package com.udacity.project4.locationreminders.reminderslist

import android.util.Log
import androidx.room.Entity
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import java.io.Serializable
import java.util.*
@Entity(tableName = "reminders")
data class ReminderDataItem(
    var title: String?,
    var description: String?,
    var location: String?,
    var latitude: Double?,
    var longitude: Double?,
    val radius: Float?,
    val id: String = UUID.randomUUID().toString()
) : Serializable {

    val printableLocation: String
        get() {

            if (location != null) {
                Log.d("<<ReminderDataItem>>", "!!BOONYA!! Location= " + location)
                return location as String
            }

            return "Lat: $latitude Lon: $longitude"
        }
}

fun ReminderDataItem.toDTO() = ReminderDTO(
    title = title,
    description = description,
    location = location,
    latitude = latitude,
    longitude = longitude,
    radius = radius
)