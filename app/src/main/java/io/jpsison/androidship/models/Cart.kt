package io.jpsison.androidship.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Cart(
    val user: User,
    val product: List<Product>
) : Parcelable