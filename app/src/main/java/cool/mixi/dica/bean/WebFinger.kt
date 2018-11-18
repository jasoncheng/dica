package cool.mixi.dica.bean

data class WebFinger(
    var subject: String?,
    var aliases: ArrayList<String>?,
    var links: ArrayList<WebFingerLink>?
) {
    fun getATOMXMLUrl(): String? {
        links?.forEach {
            if(it.type == "application/atom+xml") return it.href
        }
        return null
    }
}