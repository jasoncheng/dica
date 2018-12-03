package cool.mixi.dica.adapter

import androidx.viewpager.widget.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.fragment.PhotoViewerFragment
import kotlinx.android.synthetic.main.fg_photoviewer_item.view.*
import java.lang.ref.WeakReference

class PhotoViewerAdapter(val data: ArrayList<String>, private val ref: WeakReference<PhotoViewerFragment>): androidx.viewpager.widget.PagerAdapter() {

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = LayoutInflater.from(ref.get()?.context)
        val v =  inflater.inflate(R.layout.fg_photoviewer_item, null) as ViewGroup
        val url = data[position]
        Glide.with(App.instance.applicationContext).load(url).into(v.photo)
        container.addView(v)
        return v
    }

    override fun getCount(): Int {
        return data.size
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}