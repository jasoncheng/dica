package link.mawa.android.util

import com.google.gson.GsonBuilder
import link.mawa.android.App
import link.mawa.android.R
import link.mawa.android.bean.Profile
import link.mawa.android.bean.Status
import okhttp3.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*






interface ApiService {

    @POST("statuses/update")
    @FormUrlEncoded
    fun statusUpdate(@Field("status") status: String): Call<Status>

    @POST("statuses/update")
    @Multipart
    fun statusUpdate(
        @Part("status") status: RequestBody,
        @Part image: MultipartBody.Part): Call<Status>

    @GET("statuses/show")
    fun statusShow(@Query("id") id: Int,
                   @Query("conversation") conversation: Int): Call<List<Status>>

    @GET("statuses/public_timeline?exclude_replies=true")
    fun statusPublicTimeline(@Query("since_id") since_id: String,
                             @Query("max_id") max_id: String): Call<List<Status>>

    @GET("friendica/profile/show")
    fun friendicaProfileShow(@Query("profile_id") profile_id: String?): Call<Profile>

    companion object Factory {
        private val client: OkHttpClient
        get() {
//            Log.i("ApiService", "=========> ${PrefUtil.getUsername()}, ${PrefUtil.getPassword()}")
//            val interceptor = HttpLoggingInterceptor()
//            interceptor.level = HttpLoggingInterceptor.Level.BODY
            val clientBuilder = OkHttpClient.Builder()
//            clientBuilder.addInterceptor(interceptor)
            val authToken = Credentials.basic(PrefUtil.getUsername(), PrefUtil.getPassword())
            val headerAuthorizationInterceptor = Interceptor { chain ->
                var request = chain.request()
                val headers = request.headers().newBuilder().add("Authorization", authToken).build()
                request = request.newBuilder().headers(headers).build()
                chain.proceed(request)
            }
            val defaultSourceInterceptor = Interceptor {
                val original = it.request()
                val originalHttpUrl = original.url()

                val url = originalHttpUrl.newBuilder()
                    .addQueryParameter("source", App.instance.getString(R.string.app_name))
                    .build()
                val requestBuilder = original.newBuilder()
                    .url(url)
                it.proceed(requestBuilder.build())
            }

            clientBuilder.addInterceptor(headerAuthorizationInterceptor)
            clientBuilder.addInterceptor(defaultSourceInterceptor)
            return clientBuilder.build()
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