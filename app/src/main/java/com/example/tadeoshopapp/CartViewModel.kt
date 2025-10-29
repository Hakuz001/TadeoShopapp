package com.example.tadeoshopapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

// Item del carrito
data class CartItem(
    val productId: String,
    val product: Product,
    val quantity: Int = 1,
    val addedAt: Long = System.currentTimeMillis()
)

// Orden de compra
data class Order(
    val id: String = "",
    val compradorId: String = "",
    val compradorNombre: String = "",
    val compradorEmail: String = "",
    val items: List<CartItem> = emptyList(),
    val total: Double = 0.0,
    val estado: String = "Pendiente", // Pendiente, Confirmado, Entregado, Cancelado
    val fechaCreacion: Long = System.currentTimeMillis(),
    val vendedorId: String = "",
    val vendedorNombre: String = "",
    val vendedorTelefono: String = ""
)

sealed class CartState {
    object Idle : CartState()
    object Loading : CartState()
    data class Success(val message: String) : CartState()
    data class Error(val message: String) : CartState()
}

class CartViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _cartState = MutableStateFlow<CartState>(CartState.Idle)
    val cartState: StateFlow<CartState> = _cartState

    // Lista de items en el carrito (en memoria)
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    // Total del carrito
    private val _cartTotal = MutableStateFlow(0.0)
    val cartTotal: StateFlow<Double> = _cartTotal

    // Órdenes creadas después de finalizar compra
    private val _createdOrders = MutableStateFlow<List<Order>>(emptyList())
    val createdOrders: StateFlow<List<Order>> = _createdOrders

    // Agregar producto al carrito
    fun addToCart(product: Product) {
        val currentItems = _cartItems.value.toMutableList()

        // Verificar si el producto ya está en el carrito
        val existingItemIndex = currentItems.indexOfFirst { it.productId == product.id }

        if (existingItemIndex != -1) {
            // Si ya existe, verificar stock antes de incrementar
            val existingItem = currentItems[existingItemIndex]
            val newQuantity = existingItem.quantity + 1
            
            if (newQuantity > product.cantidad) {
                _cartState.value = CartState.Error("No hay suficiente stock. Solo hay ${product.cantidad} unidades disponibles.")
                return
            }
            
            currentItems[existingItemIndex] = existingItem.copy(
                quantity = newQuantity
            )
        } else {
            // Si no existe, verificar que haya stock disponible
            if (product.cantidad <= 0) {
                _cartState.value = CartState.Error("Este producto no está disponible.")
                return
            }
            
            // Agregarlo con cantidad 1
            currentItems.add(
                CartItem(
                    productId = product.id,
                    product = product,
                    quantity = 1
                )
            )
        }

        _cartItems.value = currentItems
        calculateTotal()
        _cartState.value = CartState.Success("Producto agregado al carrito")
    }

    // Remover producto del carrito
    fun removeFromCart(productId: String) {
        _cartItems.value = _cartItems.value.filter { it.productId != productId }
        calculateTotal()
    }

    // Actualizar cantidad de un producto
    fun updateQuantity(productId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(productId)
            return
        }

        val currentItems = _cartItems.value.toMutableList()
        val itemIndex = currentItems.indexOfFirst { it.productId == productId }

        if (itemIndex != -1) {
            val item = currentItems[itemIndex]
            
            // Verificar stock disponible
            if (newQuantity > item.product.cantidad) {
                _cartState.value = CartState.Error("No hay suficiente stock. Solo hay ${item.product.cantidad} unidades disponibles.")
                return
            }
            
            currentItems[itemIndex] = item.copy(quantity = newQuantity)
            _cartItems.value = currentItems
            calculateTotal()
        }
    }

    // Incrementar cantidad
    fun incrementQuantity(productId: String) {
        val item = _cartItems.value.find { it.productId == productId }
        item?.let {
            val newQuantity = it.quantity + 1
            
            // Verificar stock disponible
            if (newQuantity > it.product.cantidad) {
                _cartState.value = CartState.Error("No hay suficiente stock. Solo hay ${it.product.cantidad} unidades disponibles.")
                return
            }
            
            updateQuantity(productId, newQuantity)
        }
    }

    // Decrementar cantidad
    fun decrementQuantity(productId: String) {
        val item = _cartItems.value.find { it.productId == productId }
        item?.let {
            val newQuantity = it.quantity - 1
            updateQuantity(productId, newQuantity)
        }
    }

    // Calcular total del carrito
    private fun calculateTotal() {
        val total = _cartItems.value.sumOf { it.product.precio * it.quantity }
        _cartTotal.value = total
    }

    // Limpiar carrito
    fun clearCart() {
        _cartItems.value = emptyList()
        _cartTotal.value = 0.0
    }

    // Finalizar compra
    fun checkout(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                _cartState.value = CartState.Loading

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    onError("Usuario no autenticado")
                    _cartState.value = CartState.Error("Usuario no autenticado")
                    return@launch
                }

                // Verificar que el carrito no esté vacío
                if (_cartItems.value.isEmpty()) {
                    onError("El carrito está vacío")
                    _cartState.value = CartState.Error("El carrito está vacío")
                    return@launch
                }

                // Obtener datos del comprador
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                val compradorNombres = userDoc.getString("nombres") ?: ""
                val compradorApellidos = userDoc.getString("apellidos") ?: ""
                val compradorNombre = "$compradorNombres $compradorApellidos".trim().ifEmpty { "Usuario" }
                val compradorEmail = userDoc.getString("email") ?: ""

                // Agrupar items por vendedor
                val itemsByVendor = _cartItems.value.groupBy { it.product.vendedorId }

                val createdOrders = mutableListOf<Order>()

                // Crear una orden por cada vendedor
                itemsByVendor.forEach { (vendedorId, items) ->
                    // Calcular total de esta orden
                    val orderTotal = items.sumOf { it.product.precio * it.quantity }

                    // Obtener info del vendedor
                    val vendorDoc = firestore.collection("users")
                        .document(vendedorId)
                        .get()
                        .await()

                    val vendedorNombres = vendorDoc.getString("nombres") ?: ""
                    val vendedorApellidos = vendorDoc.getString("apellidos") ?: ""
                    val vendedorNombre = "$vendedorNombres $vendedorApellidos".trim().ifEmpty { "Vendedor" }
                    val vendedorTelefono = vendorDoc.getString("telefono") ?: ""

                    // Crear la orden en memoria
                    val order = Order(
                        compradorId = userId,
                        compradorNombre = compradorNombre,
                        compradorEmail = compradorEmail,
                        items = items,
                        total = orderTotal,
                        estado = "Pendiente",
                        fechaCreacion = System.currentTimeMillis(),
                        vendedorId = vendedorId,
                        vendedorNombre = vendedorNombre,
                        vendedorTelefono = vendedorTelefono
                    )

                    // Convertir items a Map para Firestore
                    val itemsMap = items.map { item ->
                        mapOf(
                            "productId" to item.productId,
                            "quantity" to item.quantity,
                            "product" to mapOf(
                                "id" to item.product.id,
                                "titulo" to item.product.titulo,
                                "descripcion" to item.product.descripcion,
                                "precio" to item.product.precio,
                                "cantidad" to item.product.cantidad,
                                "tipo" to item.product.tipo,
                                "categoria" to item.product.categoria,
                                "estado" to item.product.estado,
                                "vendedorId" to item.product.vendedorId,
                                "vendedorNombre" to item.product.vendedorNombre,
                                "vendedorTelefono" to item.product.vendedorTelefono,
                                "vendedorEmail" to item.product.vendedorEmail,
                                "imagenesUrls" to item.product.imagenesUrls,
                                "fechaPublicacion" to item.product.fechaPublicacion,
                                "vistas" to item.product.vistas
                            )
                        )
                    }

                    // Crear la orden con datos serializables para Firestore
                    val orderData = hashMapOf(
                        "compradorId" to userId,
                        "compradorNombre" to compradorNombre,
                        "compradorEmail" to compradorEmail,
                        "items" to itemsMap,
                        "total" to orderTotal,
                        "estado" to "Pendiente",
                        "fechaCreacion" to System.currentTimeMillis(),
                        "vendedorId" to vendedorId,
                        "vendedorNombre" to vendedorNombre,
                        "vendedorTelefono" to vendedorTelefono
                    )

                    // Guardar en Firestore
                    android.util.Log.d("CartViewModel", "Saving order to Firestore: compradorId=$userId, vendedorId=$vendedorId, total=$orderTotal")
                    withTimeout(10000) {
                        val docRef = firestore.collection("orders")
                            .add(orderData)
                            .await()

                        android.util.Log.d("CartViewModel", "Order saved successfully with ID: ${docRef.id}")
                        createdOrders.add(order.copy(id = docRef.id))
                    }
                }

                // Guardar órdenes creadas
                _createdOrders.value = createdOrders

                // Limpiar carrito
                clearCart()

                _cartState.value = CartState.Success("Compra realizada exitosamente")
                onSuccess()

            } catch (e: Exception) {
                _cartState.value = CartState.Error("Error al procesar compra: ${e.message}")
                onError(e.message ?: "Error desconocido")
            }
        }
    }

    // Obtener número de items en el carrito
    fun getCartItemCount(): Int {
        return _cartItems.value.sumOf { it.quantity }
    }

    // Verificar si un producto está en el carrito
    fun isInCart(productId: String): Boolean {
        return _cartItems.value.any { it.productId == productId }
    }

    // Resetear estado
    fun resetCartState() {
        _cartState.value = CartState.Idle
    }

    // Limpiar órdenes creadas
    fun clearCreatedOrders() {
        _createdOrders.value = emptyList()
    }
}