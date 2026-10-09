package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.ApiClient
import com.example.data.model.MenuItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Menú Digital", appName)
  }

  @Test
  fun `parse json with all 10 fields correctly`() {
    val sampleJson = """
      [
        {
          "id": 101,
          "nombre": "Ceviche Mixto",
          "descripcion": "Pescado fresco y mariscos en zumo de limón con ají limo",
          "precio": 18.5,
          "categoria": "Entradas",
          "imageUrl": "http://192.168.2.13/images/ceviche.jpg",
          "disponible": true,
          "tiempoPreparacionMin": 15,
          "calorias": 320,
          "destacado": true
        },
        {
          "id": 102,
          "nombre": "Lomo Saltado",
          "descripcion": "Trozos de lomo fino salteados al wok con cebolla y tomate",
          "precio": 22.0,
          "categoria": "Platos Fuertes",
          "imageUrl": null,
          "disponible": false,
          "tiempoPreparacionMin": 20,
          "calorias": 750,
          "destacado": false
        }
      ]
    """.trimIndent()

    val parsedList = ApiClient.parseMenuItemsJson(sampleJson)

    assertEquals(2, parsedList.size)

    val item1 = parsedList[0]
    assertEquals(101L, item1.id)
    assertEquals("Ceviche Mixto", item1.nombre)
    assertEquals("Pescado fresco y mariscos en zumo de limón con ají limo", item1.descripcion)
    assertEquals(18.5, item1.precio, 0.001)
    assertEquals("Entradas", item1.categoria)
    assertEquals("http://192.168.2.13/images/ceviche.jpg", item1.imageUrl)
    assertTrue(item1.disponible)
    assertEquals(15, item1.tiempoPreparacionMin)
    assertEquals(320, item1.calorias)
    assertTrue(item1.destacado)

    val item2 = parsedList[1]
    assertEquals(102L, item2.id)
    assertEquals("Lomo Saltado", item2.nombre)
    assertEquals(22.0, item2.precio, 0.001)
    assertEquals("Platos Fuertes", item2.categoria)
    assertEquals(null, item2.imageUrl)
    assertFalse(item2.disponible)
    assertEquals(20, item2.tiempoPreparacionMin)
    assertEquals(750, item2.calorias)
    assertFalse(item2.destacado)
  }

  @Test
  fun `parse json wrapped in object with menu key`() {
    val wrappedJson = """
      {
        "menu": [
          {
            "id": 1,
            "nombre": "Ensalada César",
            "descripcion": "Lechuga romana, crutones, parmesano y aderezo césar",
            "precio": 9.99,
            "categoria": "Ensaladas",
            "imageUrl": "http://192.168.2.13/images/cesar.jpg",
            "disponible": true,
            "tiempoPreparacionMin": 8,
            "calorias": 280,
            "destacado": false
          }
        ]
      }
    """.trimIndent()

    val parsedList = ApiClient.parseMenuItemsJson(wrappedJson)
    assertEquals(1, parsedList.size)
    assertEquals("Ensalada César", parsedList[0].nombre)
    assertEquals(9.99, parsedList[0].precio, 0.001)
  }
}
