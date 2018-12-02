package cool.mixi.dica.adapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import cool.mixi.dica.R
import cool.mixi.dica.activity.MainActivity
import cool.mixi.dica.fragment.*

class IndexPageAdapter(val activity: MainActivity, fragmentManager: FragmentManager): FragmentPagerAdapter(fragmentManager){

    private val names = activity.resources.getStringArray(R.array.index_tab)

    override fun getItem(position: Int): Fragment {
        return when(position) {
            0 -> TimelineFriendsFragment()
            1 -> TimelinePublicFragment()
            2 -> TimelineNetworkFragment()
            3 -> TimelineMyFragment()
            else -> TimelineFavoritesFragment()
        }
    }

    override fun getCount(): Int {
        return names.size
    }
}