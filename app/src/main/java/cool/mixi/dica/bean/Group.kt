package cool.mixi.dica.bean

data class Group(
    var name: String,
    var gid: Int,
    var user: ArrayList<User>
)