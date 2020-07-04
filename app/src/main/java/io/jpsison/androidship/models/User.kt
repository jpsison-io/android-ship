package io.jpsison.androidship.models

import android.os.Parcelable
import io.jpsison.androidship.enums.Gender
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(
    val firstName: String,
    val lastName: String,
    val age: Int,
    val gender: Gender
): Parcelable