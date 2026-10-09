package com.example.data.api

import com.example.data.model.MenuItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    const val DEFAULT_HOST = "192.168.2.13"
    const val DEFAULT_BASE_URL = "http://192.168.2.13/"

    val moshi: Moshi = Moshi.Builder()
        .add(FlexibleLongAdapter())
        .add(FlexibleDoubleAdapter())
        .add(FlexibleIntAdapter())
        .add(FlexibleBooleanAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val menuItemListAdapter = moshi.adapter<List<MenuItem>>(
        Types.newParameterizedType(List::class.java, MenuItem::class.java)
    )

    private val menuItemAdapter = moshi.adapter(MenuItem::class.java)

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    fun createService(baseUrl: String = DEFAULT_BASE_URL): MenuApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MenuApiService::class.java)
    }

    /**
     * Parsea una cadena JSON recibida de la API.
     * Puede procesar:
     * 1. Una lista JSON directa: [ { "id": 1, "nombre": ... }, ... ]
     * 2. Un objeto JSON envoltorio con llaves como "menu", "platos", "items", "data", "productos"
     * 3. Un solo objeto individual.
     */
    fun parseMenuItemsJson(jsonString: String): List<MenuItem> {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Si empieza con corchete, es una lista directa
        if (trimmed.startsWith("[")) {
            val parsed = menuItemListAdapter.fromJson(trimmed)
            if (parsed != null) return parsed
        }

        // Si empieza con llave, verificar si contiene una lista anidada
        if (trimmed.startsWith("{")) {
            val jsonObject = JSONObject(trimmed)
            val candidateKeys = listOf("menu", "platos", "items", "productos", "data", "results", "dishes")
            for (key in candidateKeys) {
                if (jsonObject.has(key)) {
                    val candidateArray = jsonObject.optJSONArray(key)
                    if (candidateArray != null) {
                        val parsed = menuItemListAdapter.fromJson(candidateArray.toString())
                        if (parsed != null) return parsed
                    }
                }
            }

            // O buscar cualquier JSONArray dentro del objeto
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val array = jsonObject.optJSONArray(key)
                if (array != null) {
                    val parsed = menuItemListAdapter.fromJson(array.toString())
                    if (parsed != null && parsed.isNotEmpty()) return parsed
                }
            }

            // Si es un único objeto con "nombre" o "id", devolverlo como lista de un elemento
            if (jsonObject.has("nombre") || jsonObject.has("id")) {
                val singleItem = menuItemAdapter.fromJson(trimmed)
                if (singleItem != null) return listOf(singleItem)
            }
        }

        return emptyList()
    }
}
