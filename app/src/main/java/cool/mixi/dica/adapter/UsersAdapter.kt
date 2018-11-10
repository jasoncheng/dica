package cool.mixi.dica.adapter

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.activity.UserActivity
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.bean.User
import cool.mixi.dica.fragment.UsersDialog
import kotlinx.android.synthetic.main.users_list_item.view.*
import java.util.*

class UsersAdapter(val data: ArrayList<User>, private val fragment: UsersDialog)
    : RecyclerView.Adapter<UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        var view = LayoutInflater.from(fragment.context).inflate(R.layout.users_list_item, parent, false)
        view.setOnClickListener { gotoUserPage(it) }
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        var user = data?.get(position)
        holder.itemView.tag = position
        holder.userName.text = user.screen_name
        Glide.with(App.instance.applicationContext)
            .load(user.profile_image_url_large)
            .apply(RequestOptions().circleCrop())
            .into(holder.avatar!!)
    }

    private fun gotoUserPage(view: View) {
        var position = view.tag as Int
        val i = Intent(fragment.activity, UserActivity::class.java)
        i.putExtra(Consts.EXTRA_USER, data[position])
        fragment.activity?.startActivity(i)
    }

}

open class UserViewHolder(view: View): RecyclerView.ViewHolder(view) {
    var userName = view.tv_username
    var avatar = view.avatar
}