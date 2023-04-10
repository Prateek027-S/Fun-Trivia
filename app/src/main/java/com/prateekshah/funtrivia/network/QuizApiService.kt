package com.prateekshah.funtrivia.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val BASE_URL = "https://opentdb.com"

private val moshi = Moshi.Builder() // Moshi converts JSON string into Kotlin objects
    .add(KotlinJsonAdapterFactory()) // For Moshi's annotations to work properly with Kotlin, in the Moshi builder, add the KotlinJsonAdapterFactory
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL) //adds base URL
    .build() // Creates retrofit object

interface QuizApiService {
    @GET("api.php")
    suspend fun getQuestions(@Query("amount") num: Int,
                             @Query("type") questionType: String,
                             @Query("difficulty") diff: String,
                             @Query("category") categoryNum: Int): DataResponse

    @GET("api.php")
    suspend fun getQuestions(@Query("amount") num: Int,
                             @Query("type") questionType: String,
                             @Query("category") categoryNum: Int): DataResponse
}

object QuizApi{
    val retrofitService: QuizApiService by lazy {
        retrofit.create(QuizApiService::class.java)
    }
}