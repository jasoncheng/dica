package link.mawa.android.util

import com.google.gson.GsonBuilder
import link.mawa.android.App
import link.mawa.android.BuildConfig
import link.mawa.android.R
import link.mawa.android.bean.Consts
import link.mawa.android.bean.Profile
import link.mawa.android.bean.Status
import link.mawa.android.bean.User
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.File
import java.util.concurrent.TimeUnit

interface ApiService {

    @POST("statuses/update")
    @FormUrlEncoded
    fun statusUpdate(
        @Field("status") status: String,
        @Field("in_reply_to_status_id") in_reply_to_status_id: Int,
        @Field("lat") lat: String,
        @Field("long") long: String): Call<Status>

    @POST("statuses/update_with_media")
    @Multipart
    fun statusUpdate(
        @Part("status") status: RequestBody,
        @Part("in_reply_to_status_id") in_reply_to_status_id: RequestBody,
        @Part("lat") lat: RequestBody,
        @Part("lat") long: RequestBody,
        @Part image: MultipartBody.Part): Call<Status>

    @GET("statuses/show?include_entities=true")
    fun statusShow(@Query("id") id: Int,
                   @Query("conversation") conversation: Int): Call<List<Status>>

    @GET("statuses/friends_timeline?exclude_replies=true")
    fun statusPublicTimeline(@Query("since_id") since_id: String,
                             @Query("max_id") max_id: String): Call<List<Status>>

    @GET("statuses/user_timeline?exclude_replies=true")
    fun statusUserTimeline(
        @Query("user_id") user_id: Int,
        @Query("since_id") since_id: String,
        @Query("max_id") max_id: String): Call<List<Status>>

    @GET("friendica/profile/show")
    fun friendicaProfileShow(@Query("profile_id") profile_id: String?): Call<Profile>

    @GET("users/show")
    fun usersShow(@Query("user_id") user_id: String): Call<User>

    companion object Factory {
        private val client: OkHttpClient
        get() {
            val clientBuilder = OkHttpClient.Builder()

            if(BuildConfig.DEBUG) {
                val interceptor = HttpLoggingInterceptor()
                interceptor.level = HttpLoggingInterceptor.Level.BODY
                clientBuilder.addInterceptor(interceptor)
            }

            val authToken = Credentials.basic(PrefUtil.getUsername(), PrefUtil.getPassword())
            val basicAuthInterceptor = Interceptor {
                var request = it.request()
                val headers = request.headers().newBuilder().add("Authorization", authToken).build()
                request = request.newBuilder().headers(headers).build()
                it.proceed(request)
            }

            val statusSourceInterceptor = Interceptor {
                val original = it.request()
                val originalHttpUrl = original.url()
                val url = originalHttpUrl.newBuilder()
                    .addQueryParameter("source", App.instance.getString(R.string.app_name))
                    .build()
                val requestBuilder = original.newBuilder()
                    .url(url)
                it.proceed(requestBuilder.build())
            }

            val cacheInterceptor = Interceptor {
                val request = if(NetworkUtil.isNetworkConnected()) {
                    it.request().newBuilder().cacheControl(CacheControl.FORCE_NETWORK).build()
                } else {
                    it.request().newBuilder().cacheControl(CacheControl.FORCE_CACHE).build()
                }

                it.proceed(request)
            }

            val cacheSize = Consts.CACHE_SIZE_IN_MB * 1024 * 1024
            val cache = Cache(File(App.instance.cacheDir, "http-cache"), cacheSize.toLong())

            return clientBuilder.cache(cache)
                .addInterceptor(basicAuthInterceptor)
                .addInterceptor(statusSourceInterceptor)
                .addInterceptor(cacheInterceptor)
                .connectTimeout(Consts.API_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Consts.API_READ_TIMEOUT, TimeUnit.SECONDS)
                .build()
        }

        fun create(): ApiService {
            val gson = GsonBuilder().serializeNulls().setLenient().create()
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl("${PrefUtil.getApiUrl()}/api/")
                .client(client)
                .build()
            return retrofit.create(ApiService::class.java)
        }
    }
}