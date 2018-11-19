package cool.mixi.dica.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cool.mixi.dica.R
import cool.mixi.dica.adapter.PhotoViewerAdapter
import cool.mixi.dica.bean.Consts
import kotlinx.android.synthetic.main.fg_photoviewer.*
import java.lang.ref.WeakReference

class PhotoViewerFragment: BaseDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater?.inflate(R.layout.fg_photoviewer, container, false)
        return view!!
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getSerializable(Consts.EXTRA_PHOTOS).let {
            photos.adapter  = PhotoViewerAdapter(it as ArrayList<String>, WeakReference(this))
            photos.currentItem = arguments?.getInt(Consts.EXTRA_PHOTO_INDEX, 0)!!
            tab_layout.setupWithViewPager(photos, true)
        }
    }
}