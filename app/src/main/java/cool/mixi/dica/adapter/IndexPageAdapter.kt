package cool.mixi.dica.adapter

import android.view.ViewGroup
import cool.mixi.dica.R
import cool.mixi.dica.activity.IndexActivity
import cool.mixi.dica.fragment.*
import java.util.*

class IndexPageAdapter(val activity: IndexActivity, fragmentManager: androidx.fragment.app.FragmentManager): androidx.fragment.app.FragmentPagerAdapter(fragmentManager){

    private val names = activity.resources.getStringArray(R.array.index_tab)
    private var pagers: WeakHashMap<Int, TimelineFragment> = WeakHashMap()

    override fun getItem(position: Int): androidx.fragment.app.Fragment {
        val fg = when(position) {
            0 -> TimelineFriendsFragment()
            1 -> TimelinePublicFragment()
            2 -> TimelineNetworkFragment()
            3 -> TimelineMyFragment()
            else -> TimelineFavoritesFragment()
        }
        pagers[position] = fg
        return fg
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        super.destroyItem(container, position, `object`)
        pagers.remove(position)
    }

    override fun getCount(): Int {
        return names.size
    }

    fun getTimelineFragment(pos: Int): TimelineFragment?{
        return pagers[pos]
    }
}