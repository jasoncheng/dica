package cool.mixi.dica.util

import com.google.gson.GsonBuilder
import cool.mixi.dica.App
import cool.mixi.dica.BuildConfig
import cool.mixi.dica.bean.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.simpleframework.xml.convert.AnnotationStrategy
import org.simpleframework.xml.core.Persister
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.*
import retrofit2.http.Headers
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

interface ApiService {

    @POST("statuses/update")
    @FormUrlEncoded
    fun statusUpdate(
        @Field("source") source: String,
        @Field("status") status: String,
        @Field("in_reply_to_status_id") in_reply_to_status_id: Int,
        @Field("lat") lat: String,
        @Field("long") long: String,
        @Field("group_allow[]") group_allow: ArrayList<Int>?,
        @Field("media_ids") media_ids:String): Call<String>

//    @POST("statuses/update_with_media")
//    @Multipart
//    fun statusUpdate(
//        @Part("source") source: RequestBody,
//        @Part("status") status: RequestBody,
//        @Part("in_reply_to_status_id") in_reply_to_status_id: RequestBody,
//        @Part("lat") lat: RequestBody,
//        @Part("long") long: RequestBody,
//        @PartMap group_allow: Map<String, @JvmSuppressWildcards RequestBody>,
//        @Part("media_ids") mediaIds: RequestBody?): Call<String>

    @POST("media/upload")
    @Multipart
    fun mediaUpload(@Part media: MultipartBody.Part): Call<String>

    @GET("statuses/show?include_entities=true")
    fun statusShow(@Query("id") id: Int,
                   @Query("conversation") conversation: Int): Call<List<Status>>

    @GET("statuses/public_timeline?exclude_replies=true")
    fun statusPublicTimeline(@Query("since_id") since_id: String,
                             @Query("max_id") max_id: String): Call<List<Status>>

    @GET("statuses/friends_timeline?exclude_replies=true")
    fun statusFriendsTimeline(@Query("since_id") since_id: String,
                              @Query("max_id") max_id: String): Call<List<Status>>

    @GET("statuses/networkpublic_timeline?exclude_replies=true")
    fun statusNetworkTimeline(@Query("since_id") since_id: String,
                              @Query("max_id") max_id: String): Call<List<Status>>

    @GET("statuses/user_timeline?exclude_replies=true")
    fun statusUserTimeline(
        @Query("user_id") user_id: Int,
        @Query("since_id") since_id: String,
        @Query("max_id") max_id: String): Call<List<Status>>

    @POST("statuses/destroy")
    @FormUrlEncoded
    fun statusDestroy(
        @Field("id") status_id: Int
    ): Call<String>

//    @POST("statuses/retweet")
//    @FormUrlEncoded
//    fun statusRetweet(@Field("id") id: Int): Call<Status>

    @GET("friendica/photo")
    fun friendicaPhoto(@Query("photo_id") photo_id: String): Call<Photo>

//    @POST("friendica/photo/create")
//    @Multipart
//    fun friendicaPhotoCreate(
//        @Part("allow_gid") allow_gid: RequestBody,
//        @Part media: MultipartBody.Part,
//        @Part("album") album: RequestBody): Call<String>

    @POST("friendica/photo/update")
    @FormUrlEncoded
    fun friendicaPhotoUpdate(
        @Field("photo_id") photo_id: String,
        @Field("album") album: String? = "Wall Photos",
        @Field("allow_gid") allow_gid: String = "",
        @Field("deny_gid") deny_gid: String = "",
        @Field("allow_cid") allow_cid: String = "",
        @Field("deny_cid") deny_cid: String = ""
    ): Call<String>

    @GET("friendica/profile/show")
    fun friendicaProfileShow(@Query("profile_id") profile_id: String?): Call<Profile>

    @GET("search?exclude_replies=true")
    fun search(
        @Query("q") query: String,
        @Query("since_id") since_id: String,
        @Query("max_id") max_id: String): Call<List<Status>>

    @GET("users/show")
    fun usersShow(@Query("user_id") user_id: String): Call<User>

    @GET("users/search")
    fun usersSearch(@Query("q") q: String): Call<List<User>>

    @POST("friendica/activity/like")
    @FormUrlEncoded
    fun like(@Field("id") id: Int): Call<String>

    @POST("friendica/activity/unlike")
    @FormUrlEncoded
    fun unlike(@Field("id") id: Int): Call<String>

    @GET("friendica/group_show")
    fun friendicaGroupShow(): Call<ArrayList<Group>>

    @GET("friendica/notifications")
    fun friendicaNotifications(): Call<List<Notification>>

    @POST("friendica/notification/seen")
    @FormUrlEncoded
    fun friendicaNotificationSeen(@Field("id") nid: Int): Call<String>

    @GET("favoritesTimeline")
    fun favoritesTimeline(@Query("since_id") since_id: String,
                          @Query("max_id") max_id: String): Call<List<Status>>

    @POST("favorites/create")
    @FormUrlEncoded
    fun favoritesCreate(@Field("id") id: Int): Call<Status>

    @POST("favorites/destroy")
    @FormUrlEncoded
    fun favoritesDestroy(@Field("id") id: Int): Call<Status>

    @GET(".well-known/webfinger")
    fun webFinger(
        @Query(value = "resource", encoded = true) email: String
    ): Call<WebFinger>

    @Headers("Accept: application/atom+xml")
    @GET("{path}")
    fun apProfile(
        @Path(value = "path", encoded = true) path: String
    ): Call<AP>

    companion object Factory {

        var cookies = HashMap<String, String>()
        private const val cacheSize = Consts.CACHE_SIZE_IN_MB * 1024 * 1024

        private val client: OkHttpClient
            get() {
                val authToken = Credentials.basic(
                    PrefUtil.getUsername(),
                    PrefUtil.getPassword()
                )

                val basicAuthInterceptor = Interceptor {
                    var request = it.request()
                    val headers = request.headers().newBuilder().add("Authorization", authToken).build()
                    request = request.newBuilder().headers(headers).build()
                    it.proceed(request)
                }

                return getBuilder().addInterceptor(basicAuthInterceptor).build()
            }

        private val cacheInterceptor = Interceptor {
            val request = if(NetworkUtil.isNetworkConnected()) {
                it.request().newBuilder().cacheControl(CacheControl.FORCE_NETWORK).build()
            } else {
                it.request().newBuilder().cacheControl(CacheControl.FORCE_CACHE).build()
            }

            it.proceed(request)
        }

        private val saveCookieInterceptor = Interceptor {
            val host = it.request().url().host()
            val res = it.proceed(it.request())
            res.headers().toMultimap().forEach { (key, value) ->
                val thisCookie = value[0].split(";".toRegex())[0]
                if(key.toLowerCase() == "set-cookie" &&
                    (thisCookie.contains("PHPSESSID") || thisCookie.contains("session".toRegex()))){
                    cookies[host.toLowerCase()] = thisCookie
                }
            }

            res
        }

        private val addCookieInterceptor = Interceptor { it ->
            val host = it.request().url().host().toLowerCase()
            val builder = it.request().newBuilder()!!
            cookies[host]?.let {  builder.addHeader("Cookie", it) }
            it.proceed(builder.build())
        }

        private fun getCache(): Cache {
            return Cache(File(App.instance.cacheDir, "http-cache"), cacheSize.toLong())
        }

        private fun getBuilder(): OkHttpClient.Builder {
            val clientBuilder = OkHttpClient.Builder()
            if(BuildConfig.DEBUG) {
                val interceptor = HttpLoggingInterceptor()
                interceptor.level = HttpLoggingInterceptor.Level.HEADERS
                clientBuilder.addInterceptor(interceptor)
            }

            clientBuilder
                .cache(getCache())
                .connectTimeout(Consts.API_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Consts.API_READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Consts.API_WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(saveCookieInterceptor)
                .addInterceptor(addCookieInterceptor)
                .addInterceptor(cacheInterceptor)
            return clientBuilder
        }

        fun create(): ApiService {
            val gson = GsonBuilder().serializeNulls().setLenient().create()
            val retrofit = Retrofit.Builder()
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl("${PrefUtil.getApiUrl()}/api/")
                .client(client)
                .build()
            return retrofit.create(ApiService::class.java)
        }

        fun create(email: String): ApiService {
            val client = getBuilder().build()
            val url = "https://${email.emailGetDomain()}/"
            dLog("WebFinger $url")
            val gson = GsonBuilder().serializeNulls().setLenient().create()
            val retrofit = Retrofit.Builder()
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(url)
                .client(client)
                .build()
            return retrofit.create(ApiService::class.java)
        }

        fun createAP(url: String): ApiService {
            val tmp = URL(url)
            val client = getBuilder().build()
            val target = "${tmp.protocol}://${tmp.host}/"
            dLog("createAP $target")
            val retrofit = Retrofit.Builder()
                .addConverterFactory(SimpleXmlConverterFactory.createNonStrict(
                    Persister(AnnotationStrategy())
                ))
                .baseUrl(target)
                .client(client)
                .build()
            return retrofit.create(ApiService::class.java)
        }
    }
}