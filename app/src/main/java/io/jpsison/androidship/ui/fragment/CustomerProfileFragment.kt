package io.jpsison.androidship.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.jpsison.androidship.R
import io.jpsison.androidship.cargo
import io.jpsison.androidship.cargo.Checkout
import io.jpsison.ship_annotation.FragmentShip

@FragmentShip(cargo = Checkout::class)
class CustomerProfileFragment : Fragment() {

    val cargo: CustomerProfileFragmentArgs by cargo()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("Default Payment: ${cargo.cardType}")
        println("Default Card: ${cargo.cart}")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.fragment_customer_profile,
            container, false
        )
        return view
    }
}