package com.morningstarwang.tmdmobile.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface PredictService {

    @POST("get_8_mode")
    fun predict8(@Body postData: RequestBody): Call<ResponseBody>

    @POST("get_4_mode")
    fun predict4(@Body postData: RequestBody): Call<ResponseBody>

    @POST("get_4_mode_2")
    fun predict42(@Body postData: RequestBody): Call<ResponseBody>

    @POST("get_8_mode_pf")
    fun predict8pf(@Body postData: RequestBody): Call<ResponseBody>
}