package ru.adel.incidentstrackerandroid.models

import android.os.Parcel
import android.os.Parcelable
import java.time.LocalDateTime

data class NotificationMessage(
    val incidentId: Long,
    val title: String,
    val longitude: Double,
    val latitude: Double,
    val timestamp: LocalDateTime
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readDouble(),
        LocalDateTime.parse(parcel.readString())
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(incidentId)
        parcel.writeString(title)
        parcel.writeDouble(longitude)
        parcel.writeDouble(latitude)
        parcel.writeString(timestamp.toString())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<NotificationMessage> {
        override fun createFromParcel(parcel: Parcel): NotificationMessage {
            return NotificationMessage(parcel)
        }

        override fun newArray(size: Int): Array<NotificationMessage?> {
            return arrayOfNulls(size)
        }
    }
}
