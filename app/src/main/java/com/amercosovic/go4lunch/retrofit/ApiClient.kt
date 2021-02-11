package com.amercosovic.go4lunch.retrofit

import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
//https://maps.googleapis.com/maps/api/place/details/json?place_id=ChIJY31kehwbU4gRxuEGnqSlHyY&fields=name,website,formatted_phone_number&key=AIzaSyB4g9Ihjg2bJrUIsCOWI9D0ZdNL5fwEPLw
object ApiClient {
    // create retrofit
    var BASE_URL: String = "https://maps.googleapis.com/maps/api/"
    val getClient: ApiInterface
        get() {

            val gson = GsonBuilder().setLenient().create()
            val interceptor = HttpLoggingInterceptor()
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            val client = OkHttpClient.Builder().addInterceptor(interceptor).build()


            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .build()

            return retrofit.create(ApiInterface::class.java)
        }
}