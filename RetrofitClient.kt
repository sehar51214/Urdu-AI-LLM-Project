package com.example.urduai

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String = ""

    fun getApi(context: Context): ApiService {
        val newBaseUrl = NetworkUtils.getIpAddress(context)

        if (retrofit == null || currentBaseUrl != newBaseUrl) {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(180, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(newBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            currentBaseUrl = newBaseUrl
        }
        return retrofit!!.create(ApiService::class.java)
    }
}