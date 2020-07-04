package io.jpsison.androidship.cargo

import android.os.Parcelable
import io.jpsison.androidship.enums.CardType
import io.jpsison.androidship.models.Cart
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Checkout(
    val cardType: CardType? = CardType.MASTERCARD,
    val cart: Cart? = null
): Parcelable