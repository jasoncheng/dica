package link.mawa.android.adapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import link.mawa.android.R
import link.mawa.android.activity.MainActivity
import link.mawa.android.fragment.FriendsTimelineFragment
import link.mawa.android.fragment.MyTimelineFragment
import link.mawa.android.fragment.PublicTimelineFragment

class IndexPageAdapter(val activity: MainActivity, fragmentManager: FragmentManager): FragmentPagerAdapter(fragmentManager){

    private val names = activity.resources.getStringArray(R.array.index_tab)

    override fun getItem(position: Int): Fragment {
        return when(position) {
            0 -> FriendsTimelineFragment()
            1 -> PublicTimelineFragment()
            else -> MyTimelineFragment()
        }
    }

    override fun getCount(): Int {
        return names.size
    }
}