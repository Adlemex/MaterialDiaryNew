package com.alex.materialdiary.sys.net

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AdlemxClient {
    private var retrofit: Retrofit? = null
    private var endpoints: AdlemxEndpoints? = null
    private const val baseUrl = "https://diary.adlemx.ru/api/"//"http://192.168.0.107:8090/"//"https://pskovedu.ml/api/"
    fun getClient(): Retrofit {
        if (retrofit == null) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            val okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor(interceptor)
                .addNetworkInterceptor {
                    it.proceed(
                        it.request().newBuilder()
                            .header("User-Agent", "ADLEMX Apps")
                            .header("Authorization", "2EQIG52H9J2JK5JS5485QPS5MF895")
                            .build()
                    )
                }
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }
    fun getEndpoints(): AdlemxEndpoints{
        if (endpoints==null)
            endpoints = getClient().create(AdlemxEndpoints::class.java)
        return endpoints!!
    }
}