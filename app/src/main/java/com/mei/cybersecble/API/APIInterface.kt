package com.mei.cybersecble.API

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface APIInterface {
    @Headers("Content-Type: application/json")
    @POST("api/newUser")
    fun newUser(@Body req: Request): Call<Response>

    @Headers("Content-Type: application/json")
    @POST("api/userHRM")
    fun userHRM(@Body req: Request): Call<Response>
}