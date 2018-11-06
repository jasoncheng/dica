package link.mawa.android.util

import com.google.gson.GsonBuilder
import link.mawa.android.App
import link.mawa.android.BuildConfig
import link.mawa.android.R
import link.mawa.android.bean.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*
import java.io.File
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

interface ApiService {

    @POST("statuses/update")
    @FormUrlEncoded
    fun statusUpdate(
        @Field("status") status: String,
        @Field("in_reply_to_status_id") in_reply_to_status_id: Int,
        @Field("lat") lat: String,
        @Field("long") long: String,
        @Field("group_allow[]") group_allow: ArrayList<Int>?): Call<Status>

    @POST("statuses/update_with_media")
    @Multipart
    fun statusUpdate(
        @Part("status") status: RequestBody,
        @Part("in_reply_to_status_id") in_reply_to_status_id: RequestBody,
        @Part("lat") lat: RequestBody,
        @Part("lat") long: RequestBody,
        @PartMap group_allow: Map<String, @JvmSuppressWildcards RequestBody>,
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

    @POST("friendica/activity/like")
    @FormUrlEncoded
    fun like(@Field("id") id: Int): Call<String>

    @POST("friendica/activity/unlike")
    @FormUrlEncoded
    fun unlike(@Field("id") id: Int): Call<String>

    @GET("friendica/group_show")
    fun friendicaGroupShow(): Call<ArrayList<Group>>

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
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl("${PrefUtil.getApiUrl()}/api/")
                .client(client)
                .build()
            return retrofit.create(ApiService::class.java)
        }

        // for BAD format JSON response, so i have to do this.
        fun like(isLike: Boolean, id: Int, callback: ILike) {
            var fn= if(isLike) { ApiService.create().like(id) } else { ApiService.create().unlike(id) }
            fn.enqueue(object: Callback<String>{
                override fun onFailure(call: Call<String>, t: Throwable) {
                    t.message?.let { eLog(it) }
                    callback.fail()
                }

                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if(response.code() == HttpsURLConnection.HTTP_OK
                        && response.body().toString().contains("ok")) {
                        callback.done()
                        return
                    }
                    callback.fail()
                }

            })
        }
    }
}

interface ILike {
    fun done()
    fun fail()
}