package cool.mixi.dica.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import cool.mixi.dica.R
import cool.mixi.dica.activity.IndexActivity
import cool.mixi.dica.fragment.*

class IndexPageAdapter(val activity: IndexActivity, fragmentManager: androidx.fragment.app.FragmentManager): androidx.fragment.app.FragmentPagerAdapter(fragmentManager){

    private val names = activity.resources.getStringArray(R.array.index_tab)

    override fun getItem(position: Int): androidx.fragment.app.Fragment {
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