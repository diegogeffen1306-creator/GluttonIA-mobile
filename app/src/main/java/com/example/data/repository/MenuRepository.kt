package com.example.data.repository

import android.util.Log
import com.example.data.api.ApiClient
import com.example.data.api.MenuApiService
import com.example.data.model.MenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Repositorio encargado de obtener los ítems del menú.
 * Contiene el método fetchFromApi que apunta a la API local en 192.168.2.13.
 */
class MenuRepository(
    private var currentBaseUrl: String = DEFAULT_BASE_URL
) {
    companion object {
        const val TAG = "MenuRepository"
        const val DEFAULT_IP = "192.168.2.13"
        const val DEFAULT_BASE_URL = "http://192.168.2.13/"
    }

    private var apiService: MenuApiService = ApiClient.createService(currentBaseUrl)

    private val _itemsFlow = MutableStateFlow<List<MenuItem>>(emptyList())
    val itemsFlow: StateFlow<List<MenuItem>> = _itemsFlow.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _isUsingFallback = MutableStateFlow(false)
    val isUsingFallback: StateFlow<Boolean> = _isUsingFallback.asStateFlow()

    /**
     * Actualiza la URL base de la API local (por ejemplo si el usuario cambia el puerto).
     */
    fun updateBaseUrl(newUrl: String) {
        val sanitized = if (newUrl.startsWith("http://") || newUrl.startsWith("https://")) {
            newUrl
        } else {
            "http://$newUrl"
        }
        currentBaseUrl = if (sanitized.endsWith("/")) sanitized else "$sanitized/"
        apiService = ApiClient.createService(currentBaseUrl)
    }

    fun getCurrentBaseUrl(): String = currentBaseUrl

    /**
     * Método fetchFromApi:
     * Conecta a la API local en 192.168.2.13 y parsea los campos del JSON:
     * id, nombre, descripcion, precio, categoria, imageUrl, disponible,
     * tiempoPreparacionMin, calorias y destacado.
     */
    suspend fun fetchFromApi(
        customEndpoint: String? = null
    ): Result<List<MenuItem>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Conectando a la API local en: $currentBaseUrl")
            _lastError.value = null

            // Si se especificó un endpoint particular, intentarlo primero
            val targetUrl = when {
                !customEndpoint.isNullOrBlank() -> {
                    if (customEndpoint.startsWith("http")) customEndpoint
                    else "$currentBaseUrl${customEndpoint.removePrefix("/")}"
                }
                else -> currentBaseUrl
            }

            // Realizamos la solicitud con cliente flexible para capturar el cuerpo JSON
            var jsonBody: String? = null

            // Intentar 1: Endpoint objetivo directo
            try {
                val response = apiService.getRawFromUrl(targetUrl)
                if (response.isSuccessful) {
                    jsonBody = response.body()?.string()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Intento directo a $targetUrl falló: ${e.message}")
            }

            // Intento 2: Si era la raíz o falló, probar rutas habituales como /menu o /platos
            if (jsonBody.isNullOrBlank()) {
                val fallbackEndpoints = listOf("menu", "api/menu", "platos", "productos")
                for (ep in fallbackEndpoints) {
                    try {
                        val testUrl = "$currentBaseUrl$ep"
                        val response = apiService.getRawFromUrl(testUrl)
                        if (response.isSuccessful) {
                            val body = response.body()?.string()
                            if (!body.isNullOrBlank()) {
                                jsonBody = body
                                Log.d(TAG, "Respuesta exitosa encontrada en: $testUrl")
                                break
                            }
                        }
                    } catch (_: Exception) {
                        // Continuar con el siguiente endpoint
                    }
                }
            }

            if (!jsonBody.isNullOrBlank()) {
                val parsedItems = ApiClient.parseMenuItemsJson(jsonBody)
                if (parsedItems.isNotEmpty()) {
                    Log.d(TAG, "Éxito parseando ${parsedItems.size} ítems de la API local")
                    _itemsFlow.value = parsedItems
                    _isUsingFallback.value = false
                    _lastError.value = null
                    return@withContext Result.success(parsedItems)
                }
            }

            // Si no se obtuvo cuerpo de respuesta exitosa
            val errorMsg = "No se pudo recibir datos válidos de $currentBaseUrl. Comprueba que tu servidor local en $DEFAULT_IP esté activo."
            Log.e(TAG, errorMsg)
            _lastError.value = errorMsg
            _isUsingFallback.value = true
            val sampleData = getSampleMenuItems()
            _itemsFlow.value = sampleData
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            val friendlyMsg = when (e) {
                is ConnectException -> "No se pudo conectar a $currentBaseUrl. Asegúrate de que tu PC/servidor en $DEFAULT_IP esté en la misma red Wi-Fi."
                is SocketTimeoutException -> "Tiempo de espera agotado al conectar a $currentBaseUrl."
                else -> "Error de red: ${e.localizedMessage ?: e.message}"
            }
            Log.e(TAG, "Error en fetchFromApi: $friendlyMsg", e)
            _lastError.value = friendlyMsg
            _isUsingFallback.value = true
            val sampleData = getSampleMenuItems()
            _itemsFlow.value = sampleData
            Result.failure(e)
        }
    }

    /**
     * Carga explícita de datos de ejemplo basados en el esquema de 10 campos.
     */
    fun loadSampleData() {
        _itemsFlow.value = getSampleMenuItems()
        _isUsingFallback.value = true
        _lastError.value = null
    }

    /**
     * Datos de muestra fieles a la estructura de 10 campos solicitada por el usuario.
     */
    fun getSampleMenuItems(): List<MenuItem> = listOf(
        MenuItem(
            id = 1L,
            nombre = "Hamburguesa Artesanal Angus",
            descripcion = "Carne angus seleccionada 200g, queso cheddar fundido, cebolla caramelizada, tocineta crocante y salsa de la casa en pan brioche tostado.",
            precio = 14.50,
            categoria = "Platos Fuertes",
            imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600",
            disponible = true,
            tiempoPreparacionMin = 18,
            calorias = 820,
            destacado = true
        ),
        MenuItem(
            id = 2L,
            nombre = "Pizza Napolitana Margherita",
            descripcion = "Masa madre de fermentación lenta (48h), salsa pomodoro San Marzano, mozzarella di bufala y albahaca fresca con aceite de oliva virgen extra.",
            precio = 12.00,
            categoria = "Platos Fuertes",
            imageUrl = "https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?w=600",
            disponible = true,
            tiempoPreparacionMin = 15,
            calorias = 680,
            destacado = true
        ),
        MenuItem(
            id = 3L,
            nombre = "Tacos de Birria Dorados (3 uds)",
            descripcion = "Tres tacos de maíz pasados por el adobo de birria y dorados a la plancha con queso fundido, cebolla, cilantro y consomé caliente para chopear.",
            precio = 11.50,
            categoria = "Entradas",
            imageUrl = "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=600",
            disponible = true,
            tiempoPreparacionMin = 12,
            calorias = 540,
            destacado = false
        ),
        MenuItem(
            id = 4L,
            nombre = "Bowl Mediterráneo con Salmón",
            descripcion = "Salmón a la plancha glaseado con limón y eneldo, mix de quinoa y verdes, aguacate hass, tomates cherry y vinagreta cítrica.",
            precio = 15.90,
            categoria = "Ensaladas",
            imageUrl = "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600",
            disponible = true,
            tiempoPreparacionMin = 14,
            calorias = 430,
            destacado = false
        ),
        MenuItem(
            id = 5L,
            nombre = "Ceviche Clásico Peruano",
            descripcion = "Cubos de corvina fresca marinados al momento en leche de tigre de ají amarillo, cebolla morada crujiente, choclo desgranado y camote dulce.",
            precio = 16.00,
            categoria = "Entradas",
            imageUrl = "https://images.unsplash.com/photo-1535400255456-984241443b29?w=600",
            disponible = true,
            tiempoPreparacionMin = 10,
            calorias = 320,
            destacado = true
        ),
        MenuItem(
            id = 6L,
            nombre = "Costillas BBQ Ahumadas",
            descripcion = "Costillas de cerdo cocinadas lentamente a baja temperatura durante 6 horas, bañadas en salsa barbacoa de miel y servidas con papas rústicas.",
            precio = 18.50,
            categoria = "Platos Fuertes",
            imageUrl = "https://images.unsplash.com/photo-1544025162-d76694265947?w=600",
            disponible = false,
            tiempoPreparacionMin = 22,
            calorias = 950,
            destacado = false
        ),
        MenuItem(
            id = 7L,
            nombre = "Tiramisú Tradicional de Mascarpone",
            descripcion = "Savoiardi empapados en café espresso fuerte con licor Amaretto, delicada crema de mascarpone batida y espolvoreado de cacao amargo.",
            precio = 6.50,
            categoria = "Postres",
            imageUrl = "https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=600",
            disponible = true,
            tiempoPreparacionMin = 5,
            calorias = 390,
            destacado = true
        ),
        MenuItem(
            id = 8L,
            nombre = "Limonada Natural de Hierbabuena",
            descripcion = "Jugo recién exprimido de limones criollos, macerado con abundante hierbabuena fresca y hielo frappé.",
            precio = 4.00,
            categoria = "Bebidas",
            imageUrl = "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=600",
            disponible = true,
            tiempoPreparacionMin = 4,
            calorias = 110,
            destacado = false
        ),
        MenuItem(
            id = 9L,
            nombre = "Café Latte de Especialidad",
            descripcion = "Espresso doble de granos 100% arábica de origen único y leche finamente vaporizada con arte latte.",
            precio = 3.90,
            categoria = "Bebidas",
            imageUrl = "https://images.unsplash.com/photo-1577968897966-3d4325b36b61?w=600",
            disponible = true,
            tiempoPreparacionMin = 3,
            calorias = 120,
            destacado = false
        )
    )
}
