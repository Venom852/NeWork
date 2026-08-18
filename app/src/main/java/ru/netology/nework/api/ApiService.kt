package ru.netology.nework.api

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import ru.netology.nework.BuildConfig
import ru.netology.nework.auth.AuthState
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.Post
import okhttp3.Interceptor
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Job
import ru.netology.nework.dto.User

private const val BASE_URL = "${BuildConfig.BASE_URL}/api/"

fun okhttp(vararg interceptors: Interceptor): OkHttpClient = OkHttpClient.Builder()
    .apply {
        interceptors.forEach {
            this.addInterceptor(it)
        }
    }
    .build()

fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BASE_URL)
    .client(client)
    .build()

interface ApiService {
    @GET("posts")
    suspend fun getAllPosts(): Response<List<Post>>

    @GET("posts/{id}/before")
    suspend fun getBeforePosts(@Path("id") id: Long, @Query("count") count: Int): Response<List<Post>>

    @GET("posts/{id}/after")
    suspend fun getAfterPosts(@Path("id") id: Long, @Query("count") count: Int): Response<List<Post>>

    @GET("posts/latest")
    suspend fun getLatestPosts(@Query("count") count: Int): Response<List<Post>>

    @GET("posts/{id}")
    suspend fun getByIdPost(@Path("id") id: Long): Response<Post>

    @POST("posts")
    suspend fun savePost(@Body post: Post): Response<Post>

    @DELETE("posts/{id}")
    suspend fun removeByIdPost(@Path("id") id: Long): Response<Unit>

    @POST("posts/{id}/likes")
    suspend fun likeByIdPost(@Path("id") id: Long): Response<Post>

    @DELETE("posts/{id}/likes")
    suspend fun dislikeByIdPost(@Path("id") id: Long): Response<Post>

    @GET("posts/{id}/newer")
    suspend fun getNewerPosts(@Path("id") id: Long): Response<List<Post>>

    @Multipart
    @POST("media")
    suspend fun upload(@Part media: MultipartBody.Part): Response<Media>

    //TODO(-------------------------------------)

    @GET("events")
    suspend fun getAllEvents(): Response<List<Event>>

    @GET("events/{id}/before")
    suspend fun getBeforeEvents(@Path("id") id: Long, @Query("count") count: Int): Response<List<Event>>

    @GET("events/{id}/after")
    suspend fun getAfterEvents(@Path("id") id: Long, @Query("count") count: Int): Response<List<Event>>

    @GET("events/latest")
    suspend fun getLatestEvents(@Query("count") count: Int): Response<List<Event>>

    @GET("events/{id}")
    suspend fun getByIdEvent(@Path("id") id: Long): Response<Event>

    @POST("events")
    suspend fun saveEvent(@Body event: Event): Response<Event>

    @DELETE("events/{id}")
    suspend fun removeByIdEvent(@Path("id") id: Long): Response<Unit>

    @POST("events/{id}/likes")
    suspend fun likeByIdEvent(@Path("id") id: Long): Response<Event>

    @DELETE("events/{id}/likes")
    suspend fun dislikeByIdEvent(@Path("id") id: Long): Response<Event>

    @GET("events/{id}/newer")
    suspend fun getNewerEvents(@Path("id") id: Long): Response<List<Event>>

    @POST("events/{id}/participants")
    suspend fun participateByIdEvent(@Path("id") id: Long): Response<Event>

    @DELETE("events/{id}/participants")
    suspend fun cancelParticipateByIdEvent(@Path("id") id: Long): Response<Event>

    //TODO(-------------------------------------)

    @FormUrlEncoded
    @POST("users/authentication")
    suspend fun updateUser(
        @Field("login") login: String,
        @Field("pass") pass: String
    ): Response<AuthState>

    @FormUrlEncoded
    @POST("users/registration")
    suspend fun registerUser(
        @Field("login") login: String,
        @Field("pass") pass: String,
        @Field("name") name: String
    ): Response<AuthState>

    @Multipart
    @POST("users/registration")
    suspend fun registerWithPhoto(
        @Part("login") login: RequestBody,
        @Part("pass") pass: RequestBody,
        @Part("name") name: RequestBody,
        @Part media: MultipartBody.Part
    ): Response<AuthState>

    @GET("users")
    suspend fun getAllUsers(): Response<List<User>>

    @GET("users/{id}")
    suspend fun getByIdUser(@Path("id") id: Long): Response<User>

    //TODO(-------------------------------------)

    @GET("{authorId}/wall")
    suspend fun getAllWallPosts(@Path("authorId") authorId: Long): Response<List<Post>>

    @GET("{authorId}/wall/{id}")
    suspend fun getByIdWallPost(@Path("authorId") authorId: Long, @Path("id") id: Long): Response<Post>

    @GET("{authorId}/wall/{id}/before")
    suspend fun getBeforeWallPosts(@Path("authorId") authorId: Long, @Path("id") id: Long, @Query("count") count: Int): Response<List<Post>>

    @GET("{authorId}/wall/{id}/after")
    suspend fun getAfterWallPosts(@Path("authorId") authorId: Long, @Path("id") id: Long, @Query("count") count: Int): Response<List<Post>>

    @GET("{authorId}/wall/latest")
    suspend fun getLatestWallPosts(@Path("authorId") authorId: Long, @Query("count") count: Int): Response<List<Post>>

    @POST("{authorId}/wall/{id}/likes")
    suspend fun likeByIdWallPost(@Path("authorId") authorId: Long, @Path("id") id: Long): Response<Post>

    @DELETE("{authorId}/wall/{id}/likes")
    suspend fun dislikeByIdWallPost(@Path("authorId") authorId: Long, @Path("id") id: Long): Response<Post>

    @GET("{authorId}/wall/{id}/newer")
    suspend fun getNewerWallPosts(@Path("authorId") authorId: Long, @Path("id") id: Long): Response<List<Post>>

    //TODO(-------------------------------------)

    @GET("my/wall")
    suspend fun getAllMyWallPosts(): Response<List<Post>>

    @GET("my/wall/{id}")
    suspend fun getByIdMyWallPost(@Path("id") id: Long): Response<Post>

    @GET("my/wall/{id}/before")
    suspend fun getBeforeMyWallPosts(@Path("id") id: Long, @Query("count") count: Int): Response<List<Post>>

    @GET("my/wall/{id}/after")
    suspend fun getAfterMyWallPosts(@Path("id") id: Long, @Query("count") count: Int): Response<List<Post>>

    @GET("my/wall/latest")
    suspend fun getLatestMyWallPosts(@Query("count") count: Int): Response<List<Post>>

    @POST("my/wall/{id}/likes")
    suspend fun likeByIdMyWallPost(@Path("id") id: Long): Response<Post>

    @DELETE("my/wall/{id}/likes")
    suspend fun dislikeByIdMyWallPost(@Path("id") id: Long): Response<Post>

    @GET("my/wall/{id}/newer")
    suspend fun getNewerMyWallPosts(@Path("id") id: Long): Response<List<Post>>

    //TODO(-------------------------------------)

    @GET("my/jobs")
    suspend fun getAllMyJobs(): Response<List<Job>>

    @POST("my/jobs")
    suspend fun saveMyJob(@Body job: Job): Response<Job>

    @DELETE("my/jobs/{id}")
    suspend fun removeByIdMyJob(@Path("id") id: Long): Response<Unit>

    //TODO(-------------------------------------)

    @GET("{userId}/jobs")
    suspend fun getAllJobs(@Path("userId") userId: Long): Response<List<Job>>
}