package cool.mixi.dica.bean

import cool.mixi.dica.util.dicaHTMLFilter
import cool.mixi.dica.util.getAvatar
import cool.mixi.dica.util.toAttachments
import org.simpleframework.xml.*
import java.text.SimpleDateFormat

@Default(required = false, value = DefaultType.FIELD)
@Root(strict = false)
data class AP (
    @field:Element var id: String = "",
    @field:Element(required = false) var title: String = "",
    @field:Element(required = false) var subtitle: String = "",
    @field:Element var author: APAuthor = APAuthor(),
    @field:ElementList(inline = true, required = false) var link: List<APLink> = ArrayList(),
    @field:ElementList(inline = true, required = false) var entry: List<APEntry> = ArrayList()
){
    fun toUser(): User {
        var u = author.toUser()
        if(u.description.isNullOrEmpty() && !subtitle.isNullOrEmpty()){
            u.description = subtitle
        }

        link.getAvatar().let {
            if(!it.isNullOrEmpty()){
                u.profile_image_url_large = it
            }
        }
        return u
    }
}

@Root(strict = false, name = "entry")
@Default(required = false, value = DefaultType.FIELD)
data class APEntry (
    @field:Element(required = false) var id: String = "",
    @field:Element(required = false) var title: String = "",
    @field:Element(required = false) var content: String = "",
    @field:Attribute(required = false) var contentType: String = "",
    @field:Element(required = false) var published: String = "",
    @field:Element(required = false, name = "georss:point") var georss: String = "",
    @field:Element(required = false) var author: APAuthor = APAuthor(),
    @field:ElementList(required = false, inline = true) var link: List<APLink> = ArrayList(),
    @field:Element(required = false, name = "activity:verb") var verb: String = "",
    @field:Element(required = false, name = "status_net") var statusNet: StatusNet = StatusNet(),
    @field:Element(required = false, name = "statusnet:notice_info") var statusNetNoticeInfo: StatusNetNoticeInfo = StatusNetNoticeInfo(),
    @field:Element(required = false, name = "activity:object") var activityObj: APEntry?
){

    constructor(): this("", "", "", "", "", "", APAuthor(), ArrayList(), "", StatusNet(), StatusNetNoticeInfo(), null)

    fun toStatus(): Status {
        val status = Status()
        if(!georss.isNullOrEmpty()){
            var geo = Geo()
            var ar = georss.split(" ".toRegex(), 2)
            geo.coordinates = DoubleArray(2)
            geo.coordinates[0] = ar[0].toDouble()
            geo.coordinates[1] = ar[1].toDouble()
            status.geo = geo
        }

        // Date process
        try {
            //2018-11-17T12:41:14.939156Z
            if(published.contains("\\.([0-9]{6})Z".toRegex())){
                published = published.substring(0, published.indexOf("."))+"Z"
            }
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
            val date = format.parse(published)
            status.created_at = date.toString()
        }catch (e: Exception){}

        status.source = statusNetNoticeInfo.source
        status.external_url = id
        status.text = content.dicaHTMLFilter()
        status.attachments = link.toAttachments()
        return status
    }
}

@Root(name = "author", strict = false)
data class APAuthor(
    @field:Element(required = false) var id: String = "",
    @field:Element var name: String = "",
    @field:Element(required = false, name = "poco:preferredUsername") var preferredUsername: String = "",
    @field:Element(required = false, name = "poco:displayName") var displayName: String = "",
    @field:Element var uri: String = "",
    @field:Element(required = false) var email: String = "",
    @field:Element(required = false) var summary: String = "",
    @field:ElementList(required = false, inline = true) var link: List<APLink> = ArrayList()
){
    fun toUser(): User{
        var user = User()
        user.url = id
        user.screen_name = name
        user.statusnet_profile_url = id
        user.description = summary
        user.profile_image_url_large = link.getAvatar()
        user.description.isNotEmpty().let {
            user.description = user.description.dicaHTMLFilter()
        }
        return user
    }
}

@Root(name = "link")
data class APLink(
    @field:Attribute(required = false) var rel: String = "",
    @field:Attribute(required = false) var type: String = "",
    @field:Attribute(required = false) var href: String = ""
)

@Root(name = "statusnet:notice_info")
data class StatusNetNoticeInfo(
    @field:Attribute(required = false) var source: String = "",
    @field:Attribute(required = false, name = "local_id") var id: String = ""
)

@Root(name = "status_net")
data class StatusNet(
    @field:Attribute(required = false, name = "notice_id") var id: String = ""
)