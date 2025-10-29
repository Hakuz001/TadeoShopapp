package com.example.tadeoshopapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class OrdersState {
    object Idle : OrdersState()
    object Loading : OrdersState()
    data class Success(val message: String) : OrdersState()
    data class Error(val message: String) : OrdersState()
}

class OrdersViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _ordersState = MutableStateFlow<OrdersState>(OrdersState.Idle)
    val ordersState: StateFlow<OrdersState> = _ordersState

    // Mis compras (como comprador)
    private val _myPurchases = MutableStateFlow<List<Order>>(emptyList())
    val myPurchases: StateFlow<List<Order>> = _myPurchases

    // Mis ventas (como vendedor)
    private val _mySales = MutableStateFlow<List<Order>>(emptyList())
    val mySales: StateFlow<List<Order>> = _mySales

    // Cargar mis compras
    fun loadMyPurchases() {
        viewModelScope.launch {
            try {
                _ordersState.value = OrdersState.Loading

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _ordersState.value = OrdersState.Error("Usuario no autenticado")
                    return@launch
                }

                // Buscar órdenes donde el usuario es el comprador
                val snapshot = firestore.collection("orders")
                    .whereEqualTo("compradorId", userId)
                    .get()
                    .await()

                // Ordenar manualmente por fecha descendente
                val sortedDocuments = snapshot.documents.sortedByDescending { 
                    (it.get("fechaCreacion") as? Number)?.toLong() ?: 0L
                }

                android.util.Log.d("OrdersViewModel", "Found ${snapshot.documents.size} orders for userId: $userId")

                val orders = sortedDocuments.mapNotNull { doc ->
                    try {
                        android.util.Log.d("OrdersViewModel", "Processing order: ${doc.id}")
                        val items = (doc.get("items") as? List<*>)?.mapNotNull { item ->
                            val itemMap = item as? Map<*, *>
                            val productMap = itemMap?.get("product") as? Map<*, *>

                            if (productMap != null) {
                                val product = Product(
                                    id = productMap["id"] as? String ?: "",
                                    titulo = productMap["titulo"] as? String ?: "",
                                    descripcion = productMap["descripcion"] as? String ?: "",
                                    precio = (productMap["precio"] as? Number)?.toDouble() ?: 0.0,
                                    cantidad = (productMap["cantidad"] as? Number)?.toInt() ?: 1,
                                    tipo = productMap["tipo"] as? String ?: "Nuevo",
                                    categoria = productMap["categoria"] as? String ?: "",
                                    imagenesUrls = productMap["imagenesUrls"] as? List<String> ?: emptyList(),
                                    vendedorId = productMap["vendedorId"] as? String ?: "",
                                    vendedorNombre = productMap["vendedorNombre"] as? String ?: "",
                                    fechaPublicacion = (productMap["fechaPublicacion"] as? Number)?.toLong() ?: 0L,
                                    estado = productMap["estado"] as? String ?: "Publicado"
                                )

                                CartItem(
                                    productId = itemMap["productId"] as? String ?: "",
                                    product = product,
                                    quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1
                                )
                            } else null
                        } ?: emptyList()

                        val order = Order(
                            id = doc.id,
                            compradorId = doc.getString("compradorId") ?: "",
                            compradorNombre = doc.getString("compradorNombre") ?: "",
                            compradorEmail = doc.getString("compradorEmail") ?: "",
                            items = items,
                            total = (doc.get("total") as? Number)?.toDouble() ?: 0.0,
                            estado = doc.getString("estado") ?: "Pendiente",
                            fechaCreacion = (doc.get("fechaCreacion") as? Number)?.toLong() ?: 0L,
                            vendedorId = doc.getString("vendedorId") ?: "",
                            vendedorNombre = doc.getString("vendedorNombre") ?: "",
                            vendedorTelefono = doc.getString("vendedorTelefono") ?: ""
                        )
                        android.util.Log.d("OrdersViewModel", "Successfully created order: ${order.id}")
                        order
                    } catch (e: Exception) {
                        android.util.Log.e("OrdersViewModel", "Error parsing order: ${e.message}", e)
                        null
                    }
                }

                android.util.Log.d("OrdersViewModel", "Successfully loaded ${orders.size} orders")
                _myPurchases.value = orders
                _ordersState.value = OrdersState.Idle

            } catch (e: Exception) {
                _ordersState.value = OrdersState.Error("Error al cargar compras: ${e.message}")
            }
        }
    }

    // Cargar mis ventas
    fun loadMySales() {
        viewModelScope.launch {
            try {
                _ordersState.value = OrdersState.Loading

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _ordersState.value = OrdersState.Error("Usuario no autenticado")
                    return@launch
                }

                // Buscar órdenes donde el usuario es el vendedor
                val snapshot = firestore.collection("orders")
                    .whereEqualTo("vendedorId", userId)
                    .get()
                    .await()

                // Ordenar manualmente por fecha descendente
                val sortedDocuments = snapshot.documents.sortedByDescending { 
                    (it.get("fechaCreacion") as? Number)?.toLong() ?: 0L
                }

                android.util.Log.d("OrdersViewModel", "Found ${snapshot.documents.size} sales for userId: $userId")

                val orders = sortedDocuments.mapNotNull { doc ->
                    try {
                        val items = (doc.get("items") as? List<*>)?.mapNotNull { item ->
                            val itemMap = item as? Map<*, *>
                            val productMap = itemMap?.get("product") as? Map<*, *>

                            if (productMap != null) {
                                val product = Product(
                                    id = productMap["id"] as? String ?: "",
                                    titulo = productMap["titulo"] as? String ?: "",
                                    descripcion = productMap["descripcion"] as? String ?: "",
                                    precio = (productMap["precio"] as? Number)?.toDouble() ?: 0.0,
                                    cantidad = (productMap["cantidad"] as? Number)?.toInt() ?: 1,
                                    tipo = productMap["tipo"] as? String ?: "Nuevo",
                                    categoria = productMap["categoria"] as? String ?: "",
                                    imagenesUrls = productMap["imagenesUrls"] as? List<String> ?: emptyList(),
                                    vendedorId = productMap["vendedorId"] as? String ?: "",
                                    vendedorNombre = productMap["vendedorNombre"] as? String ?: "",
                                    fechaPublicacion = (productMap["fechaPublicacion"] as? Number)?.toLong() ?: 0L,
                                    estado = productMap["estado"] as? String ?: "Publicado"
                                )

                                CartItem(
                                    productId = itemMap["productId"] as? String ?: "",
                                    product = product,
                                    quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1
                                )
                            } else null
                        } ?: emptyList()

                        Order(
                            id = doc.id,
                            compradorId = doc.getString("compradorId") ?: "",
                            compradorNombre = doc.getString("compradorNombre") ?: "",
                            compradorEmail = doc.getString("compradorEmail") ?: "",
                            items = items,
                            total = (doc.get("total") as? Number)?.toDouble() ?: 0.0,
                            estado = doc.getString("estado") ?: "Pendiente",
                            fechaCreacion = (doc.get("fechaCreacion") as? Number)?.toLong() ?: 0L,
                            vendedorId = doc.getString("vendedorId") ?: "",
                            vendedorNombre = doc.getString("vendedorNombre") ?: "",
                            vendedorTelefono = doc.getString("vendedorTelefono") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                _mySales.value = orders
                _ordersState.value = OrdersState.Idle

            } catch (e: Exception) {
                _ordersState.value = OrdersState.Error("Error al cargar ventas: ${e.message}")
            }
        }
    }

    // Actualizar estado de una orden (para vendedores)
    fun updateOrderStatus(orderId: String, newStatus: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                _ordersState.value = OrdersState.Loading

                // Obtener la orden actual para verificar el estado anterior
                val orderDoc = firestore.collection("orders")
                    .document(orderId)
                    .get()
                    .await()

                val currentStatus = orderDoc.getString("estado") ?: "Pendiente"
                val items = orderDoc.get("items") as? List<*>

                // Actualizar el estado de la orden
                firestore.collection("orders")
                    .document(orderId)
                    .update("estado", newStatus)
                    .await()

                // Si se cambió a "Entregado" y antes no estaba entregado, reducir stock
                if (newStatus == "Entregado" && currentStatus != "Entregado" && items != null) {
                    android.util.Log.d("OrdersViewModel", "Reduciendo stock para orden entregada: $orderId")
                    
                    // Reducir stock de cada producto en la orden
                    items.forEach { item ->
                        val itemMap = item as? Map<*, *>
                        val productMap = itemMap?.get("product") as? Map<*, *>
                        val quantity = (itemMap?.get("quantity") as? Number)?.toInt() ?: 0
                        val productId = productMap?.get("id") as? String

                        if (productId != null && quantity > 0) {
                            try {
                                // Obtener el producto actual
                                val productDoc = firestore.collection("products")
                                    .document(productId)
                                    .get()
                                    .await()

                                val currentStock = (productDoc.get("cantidad") as? Number)?.toInt() ?: 0
                                val newStock = maxOf(0, currentStock - quantity) // No permitir stock negativo

                                android.util.Log.d("OrdersViewModel", "Producto $productId: stock $currentStock -> $newStock (vendido: $quantity)")

                                // Actualizar el stock del producto
                                firestore.collection("products")
                                    .document(productId)
                                    .update("cantidad", newStock)
                                    .await()

                            } catch (e: Exception) {
                                android.util.Log.e("OrdersViewModel", "Error actualizando stock del producto $productId: ${e.message}", e)
                            }
                        }
                    }
                }
                // Si se revierte de "Entregado" a otro estado, restaurar stock
                else if (currentStatus == "Entregado" && newStatus != "Entregado" && items != null) {
                    android.util.Log.d("OrdersViewModel", "Restaurando stock para orden revertida: $orderId")
                    
                    // Restaurar stock de cada producto en la orden
                    items.forEach { item ->
                        val itemMap = item as? Map<*, *>
                        val productMap = itemMap?.get("product") as? Map<*, *>
                        val quantity = (itemMap?.get("quantity") as? Number)?.toInt() ?: 0
                        val productId = productMap?.get("id") as? String

                        if (productId != null && quantity > 0) {
                            try {
                                // Obtener el producto actual
                                val productDoc = firestore.collection("products")
                                    .document(productId)
                                    .get()
                                    .await()

                                val currentStock = (productDoc.get("cantidad") as? Number)?.toInt() ?: 0
                                val newStock = currentStock + quantity

                                android.util.Log.d("OrdersViewModel", "Producto $productId: stock $currentStock -> $newStock (restaurado: $quantity)")

                                // Actualizar el stock del producto
                                firestore.collection("products")
                                    .document(productId)
                                    .update("cantidad", newStock)
                                    .await()

                            } catch (e: Exception) {
                                android.util.Log.e("OrdersViewModel", "Error restaurando stock del producto $productId: ${e.message}", e)
                            }
                        }
                    }
                }

                _ordersState.value = OrdersState.Success("Estado actualizado")

                // Recargar ventas
                loadMySales()

                onSuccess()

            } catch (e: Exception) {
                _ordersState.value = OrdersState.Error("Error al actualizar: ${e.message}")
            }
        }
    }

    fun resetState() {
        _ordersState.value = OrdersState.Idle
    }
}