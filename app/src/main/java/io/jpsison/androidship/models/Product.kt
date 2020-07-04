package io.jpsison.androidship.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val discount: Float,
    val age: Int
) : Parcelable