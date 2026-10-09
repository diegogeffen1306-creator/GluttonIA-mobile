package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MenuItem
import com.example.data.repository.MenuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption(val label: String) {
    DESTACADOS_PRIMERO("Destacados primero"),
    PRECIO_MENOR("Menor precio"),
    PRECIO_MAYOR("Mayor precio"),
    TIEMPO_MENOR("Más rápido"),
    CALORIAS_MENOR("Menos calorías")
}

data class MenuUiState(
    val allItems: List<MenuItem> = emptyList(),
    val filteredItems: List<MenuItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "Todos",
    val searchQuery: String = "",
    val onlyAvailable: Boolean = false,
    val sortOption: SortOption = SortOption.DESTACADOS_PRIMERO,
    val isLoading: Boolean = false,
    val isUsingFallback: Boolean = false,
    val errorMessage: String? = null,
    val currentApiUrl: String = MenuRepository.DEFAULT_BASE_URL,
    val currentEndpoint: String = "menu",
    val selectedItem: MenuItem? = null,
    val cartItems: Map<Long, Int> = emptyMap(), // Item ID -> Cantidad
    val isConfigDialogOpen: Boolean = false,
    val isCartSheetOpen: Boolean = false
) {
    val cartTotalCount: Int get() = cartItems.values.sum()

    val cartTotalPrice: Double get() = cartItems.entries.sumOf { (id, qty) ->
        val item = allItems.find { it.id == id }
        (item?.precio ?: 0.0) * qty
    }
}

class MenuViewModel(
    private val repository: MenuRepository = MenuRepository()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("Todos")
    private val _onlyAvailable = MutableStateFlow(false)
    private val _sortOption = MutableStateFlow(SortOption.DESTACADOS_PRIMERO)
    private val _isLoading = MutableStateFlow(false)
    private val _selectedItem = MutableStateFlow<MenuItem?>(null)
    private val _cartItems = MutableStateFlow<Map<Long, Int>>(emptyMap())
    private val _isConfigDialogOpen = MutableStateFlow(false)
    private val _isCartSheetOpen = MutableStateFlow(false)
    private val _currentEndpoint = MutableStateFlow("menu")

    val uiState: StateFlow<MenuUiState> = combine(
        repository.itemsFlow,
        repository.lastError,
        repository.isUsingFallback,
        _searchQuery,
        _selectedCategory,
        _onlyAvailable,
        _sortOption,
        _isLoading,
        _selectedItem,
        _cartItems,
        _isConfigDialogOpen,
        _isCartSheetOpen,
        _currentEndpoint
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val items = args[0] as List<MenuItem>
        val lastError = args[1] as String?
        val isFallback = args[2] as Boolean
        val query = args[3] as String
        val category = args[4] as String
        val onlyAvail = args[5] as Boolean
        val sort = args[6] as SortOption
        val loading = args[7] as Boolean
        val selected = args[8] as MenuItem?
        @Suppress("UNCHECKED_CAST")
        val cart = args[9] as Map<Long, Int>
        val configOpen = args[10] as Boolean
        val cartOpen = args[11] as Boolean
        val endpoint = args[12] as String

        // Extraer categorías únicas
        val rawCategories = items.map { it.categoria.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        val categories = listOf("Todos", "⭐ Destacados") + rawCategories

        // Aplicar filtros
        var result = items

        // 1. Filtro de búsqueda
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { item ->
                item.nombre.lowercase().contains(q) ||
                item.descripcion.lowercase().contains(q) ||
                item.categoria.lowercase().contains(q)
            }
        }

        // 2. Filtro de categoría
        when (category) {
            "Todos" -> { /* Sin filtro */ }
            "⭐ Destacados" -> {
                result = result.filter { it.destacado }
            }
            else -> {
                result = result.filter { it.categoria.equals(category, ignoreCase = true) }
            }
        }

        // 3. Filtro de disponibilidad
        if (onlyAvail) {
            result = result.filter { it.disponible }
        }

        // 4. Ordenamiento
        result = when (sort) {
            SortOption.DESTACADOS_PRIMERO -> result.sortedWith(
                compareByDescending<MenuItem> { it.destacado }
                    .thenByDescending { it.disponible }
                    .thenBy { it.nombre }
            )
            SortOption.PRECIO_MENOR -> result.sortedBy { it.precio }
            SortOption.PRECIO_MAYOR -> result.sortedByDescending { it.precio }
            SortOption.TIEMPO_MENOR -> result.sortedBy { it.tiempoPreparacionMin }
            SortOption.CALORIAS_MENOR -> result.sortedBy { it.calorias }
        }

        MenuUiState(
            allItems = items,
            filteredItems = result,
            categories = categories,
            selectedCategory = category,
            searchQuery = query,
            onlyAvailable = onlyAvail,
            sortOption = sort,
            isLoading = loading,
            isUsingFallback = isFallback,
            errorMessage = lastError,
            currentApiUrl = repository.getCurrentBaseUrl(),
            currentEndpoint = endpoint,
            selectedItem = selected,
            cartItems = cart,
            isConfigDialogOpen = configOpen,
            isCartSheetOpen = cartOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MenuUiState(isLoading = true)
    )

    init {
        fetchMenu()
    }

    fun fetchMenu(endpoint: String = _currentEndpoint.value) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentEndpoint.value = endpoint
            repository.fetchFromApi(endpoint)
            _isLoading.value = false
        }
    }

    fun updateApiConfig(newUrl: String, newEndpoint: String) {
        repository.updateBaseUrl(newUrl)
        _currentEndpoint.value = newEndpoint
        fetchMenu(newEndpoint)
    }

    fun loadSampleData() {
        repository.loadSampleData()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleOnlyAvailable() {
        _onlyAvailable.value = !_onlyAvailable.value
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun selectItem(item: MenuItem?) {
        _selectedItem.value = item
    }

    fun addToCart(item: MenuItem) {
        val current = _cartItems.value.toMutableMap()
        val count = current[item.id] ?: 0
        current[item.id] = count + 1
        _cartItems.value = current
    }

    fun removeFromCart(item: MenuItem) {
        val current = _cartItems.value.toMutableMap()
        val count = current[item.id] ?: 0
        if (count > 1) {
            current[item.id] = count - 1
        } else {
            current.remove(item.id)
        }
        _cartItems.value = current
    }

    fun clearCart() {
        _cartItems.value = emptyMap()
    }

    fun openConfigDialog() {
        _isConfigDialogOpen.value = true
    }

    fun closeConfigDialog() {
        _isConfigDialogOpen.value = false
    }

    fun openCartSheet() {
        _isCartSheetOpen.value = true
    }

    fun closeCartSheet() {
        _isCartSheetOpen.value = false
    }
}
