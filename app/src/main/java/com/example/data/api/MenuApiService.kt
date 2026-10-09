package com.example.data.api

import com.example.data.model.MenuItem
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Interfaz de Retrofit para consumir la API de menú.
 */
interface MenuApiService {

    @GET
    suspend fun getRawFromUrl(@Url url: String): Response<ResponseBody>

    @GET("menu")
    suspend fun getMenu(): List<MenuItem>

    @GET("api/menu")
    suspend fun getApiMenu(): List<MenuItem>

    @GET("platos")
    suspend fun getPlatos(): List<MenuItem>

    @GET(".")
    suspend fun getRootMenu(): List<MenuItem>
}
