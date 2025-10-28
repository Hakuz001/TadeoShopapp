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
                    .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val orders = snapshot.documents.mapNotNull { doc ->
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
                    .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val orders = snapshot.documents.mapNotNull { doc ->
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

                firestore.collection("orders")
                    .document(orderId)
                    .update("estado", newStatus)
                    .await()

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