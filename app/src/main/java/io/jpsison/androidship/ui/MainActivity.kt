package io.jpsison.androidship.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import io.jpsison.androidship.R
import io.jpsison.androidship.cargo.Checkout
import io.jpsison.androidship.databinding.ActivityMainBinding
import io.jpsison.androidship.enums.Gender
import io.jpsison.androidship.models.User
import io.jpsison.androidship.ui.adapter.ShopPagerAdapter
import io.jpsison.ship_annotation.ActivityShip

@ActivityShip(cargo = Checkout::class)
class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    lateinit var adapter: ShopPagerAdapter

    val user = User(
        firstName = "Finn",
        lastName = "Human",
        age = 150,
        gender = Gender.MALE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        adapter = ShopPagerAdapter(supportFragmentManager, user)
        binding.pager.adapter = adapter
        binding.tabLayout.setupWithViewPager(binding.pager)
    }
}
