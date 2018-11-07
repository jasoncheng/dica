package link.mawa.android.util

import android.text.TextUtils
import link.mawa.android.bean.Status
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class FriendicaUtil {

    companion object {
        private val proxyImagePattern = Pattern.compile("\\/proxy\\/([a-z0-9]{2})\\/",
            Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL)

        fun stripStatusTextProxyUrl(status: Status) {
            if(status.text == null || status.text.isEmpty()) {
                return
            }

            var urls = status.text.urls()
            urls.forEach {
                while(proxyImagePattern.matcher(it).find()){
                    dLog("MatchProxyImage: ${it}")
                    status.text = status.text.replace(it, "", true)
                    break
                }
            }
        }

        fun getProxyUrlPartial(originalUrl: String): String{
            var tmpUrl = TextUtils.htmlEncode(originalUrl)
            var shortpath = tmpUrl.md5()
            var longpath = shortpath.substring(0, 2)
            var base64 =
                String(android.util.Base64.encode(
                    tmpUrl.toByteArray(),
                    android.util.Base64.URL_SAFE), StandardCharsets.UTF_8)
            longpath+="/"+base64.replace("\\+\\/".toRegex(), "-_")
            return longpath
        }
    }
}