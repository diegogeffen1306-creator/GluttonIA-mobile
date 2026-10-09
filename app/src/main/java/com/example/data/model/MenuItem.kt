package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Modelo de datos que representa un ítem del menú gastronómico.
 * Mapea exactamente los campos especificados:
 * - id: Identificador único del plato
 * - nombre: Nombre del plato o bebida
 * - descripcion: Descripción de los ingredientes y preparación
 * - precio: Precio monetario
 * - categoria: Categoría culinaria (e.g., Entradas, Platos Fuertes, Postres)
 * - imageUrl: URL o enlace de la imagen del producto
 * - disponible: Indicador de disponibilidad actual en cocina
 * - tiempoPreparacionMin: Tiempo estimado de preparación en minutos
 * - calorias: Contenido calórico en kcal
 * - destacado: Indicador si el plato es sugerencia destacada del chef
 */
@JsonClass(generateAdapter = true)
data class MenuItem(
    @Json(name = "id")
    val id: Long = 0L,

    @Json(name = "nombre")
    val nombre: String = "",

    @Json(name = "descripcion")
    val descripcion: String = "",

    @Json(name = "precio")
    val precio: Double = 0.0,

    @Json(name = "categoria")
    val categoria: String = "",

    @Json(name = "imageUrl")
    val imageUrl: String? = null,

    @Json(name = "disponible")
    val disponible: Boolean = true,

    @Json(name = "tiempoPreparacionMin")
    val tiempoPreparacionMin: Int = 0,

    @Json(name = "calorias")
    val calorias: Int = 0,

    @Json(name = "destacado")
    val destacado: Boolean = false
)
