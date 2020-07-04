package io.jpsison.androidship.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import io.jpsison.androidship.enums.CardType
import io.jpsison.androidship.models.User
import io.jpsison.androidship.ui.fragment.CustomerCartFragmentBuilder
import io.jpsison.androidship.ui.fragment.CustomerProfileFragment
import io.jpsison.androidship.ui.fragment.CustomerProfileFragmentBuilder

class ShopPagerAdapter(
    fm: FragmentManager,
    user: User
) : FragmentStatePagerAdapter(
        fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) {

    private val fragments = listOf(
        Pair(
            "Profile",
            CustomerProfileFragmentBuilder()
                .setCardType(CardType.MASTERCARD)
                .create()
        ),
        Pair(
            "Profile",
            CustomerCartFragmentBuilder()
                .create()
        )
    )

    override fun getItem(position: Int): Fragment {
        return fragments[position].second
    }

    override fun getCount(): Int = fragments.size

    override fun getPageTitle(position: Int): CharSequence = fragments[position].first
}