package com.example.tadeoshopapp

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

data class Product(
    val id: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val precio: Double = 0.0,
    val cantidad: Int = 1,
    val tipo: String = "Nuevo", // "Nuevo" o "Usado"
    val categoria: String = "",
    val estado: String = "Publicado",
    val vendedorId: String = "",
    val vendedorNombre: String = "",
    val vendedorTelefono: String = "",
    val vendedorEmail: String = "",
    val imagenesUrls: List<String> = emptyList(),
    val fechaPublicacion: Long = System.currentTimeMillis(),
    val vistas: Int = 0
)

sealed class ProductState {
    object Idle : ProductState()
    object Loading : ProductState()
    data class Success(val message: String) : ProductState()
    data class Error(val message: String) : ProductState()
}

class ProductViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private val _productState = MutableStateFlow<ProductState>(ProductState.Idle)
    val productState: StateFlow<ProductState> = _productState

    private val _myProducts = MutableStateFlow<List<Product>>(emptyList())
    val myProducts: StateFlow<List<Product>> = _myProducts

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct

    // Seleccionar un producto para editar
    fun selectProduct(product: Product) {
        _selectedProduct.value = product
    }

    // Limpiar producto seleccionado
    fun clearSelectedProduct() {
        _selectedProduct.value = null
    }

    // Cargar productos del usuario actual
    fun loadMyProducts() {
        viewModelScope.launch {
            try {
                _productState.value = ProductState.Loading

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _productState.value = ProductState.Error("Usuario no autenticado")
                    return@launch
                }

                val documents = firestore.collection("products")
                    .whereEqualTo("vendedorId", userId)
                    .get()
                    .await()

                val products = documents.documents
                    .mapNotNull { doc ->
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    }
                    .sortedByDescending { it.fechaPublicacion }

                _myProducts.value = products
                _productState.value = ProductState.Idle

            } catch (e: Exception) {
                _productState.value = ProductState.Error("Error al cargar productos: ${e.message}")
            }
        }
    }

    // Cargar TODOS los productos del marketplace
    fun loadAllProducts() {
        viewModelScope.launch {
            try {
                _productState.value = ProductState.Loading

                val documents = firestore.collection("products")
                    .whereEqualTo("estado", "Publicado")
                    .get()
                    .await()

                val products = documents.documents
                    .mapNotNull { doc ->
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    }
                    .sortedByDescending { it.fechaPublicacion }

                _allProducts.value = products
                _productState.value = ProductState.Idle

            } catch (e: Exception) {
                _productState.value = ProductState.Error("Error al cargar productos: ${e.message}")
            }
        }
    }

    // Crear producto
    fun createProduct(
        titulo: String,
        descripcion: String,
        precio: Double,
        cantidad: Int,
        tipo: String,
        categoria: String,
        imageUris: List<Uri>,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                _productState.value = ProductState.Loading

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    onError("Usuario no autenticado")
                    _productState.value = ProductState.Error("Usuario no autenticado")
                    return@launch
                }

                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                val vendedorNombres = userDoc.getString("nombres") ?: ""
                val vendedorApellidos = userDoc.getString("apellidos") ?: ""
                val vendedorNombre = "$vendedorNombres $vendedorApellidos".trim().ifEmpty { "Usuario" }
                val vendedorTelefono = userDoc.getString("telefono") ?: "573001234567"
                val vendedorEmail = userDoc.getString("email") ?: "vendedor@ejemplo.com"

                val imageUrls = mutableListOf<String>()
                imageUris.forEachIndexed { index, uri ->
                    try {
                        val storageRef = storage.reference
                            .child("product_photos")
                            .child("${userId}_${System.currentTimeMillis()}_$index.jpg")

                        withTimeout(30000) {
                            storageRef.putFile(uri).await()
                        }

                        val downloadUrl = storageRef.downloadUrl.await().toString()
                        imageUrls.add(downloadUrl)
                    } catch (e: Exception) {
                        // Si falla una imagen, continuar
                    }
                }

                if (imageUrls.isEmpty()) {
                    onError("Error al subir las imágenes")
                    _productState.value = ProductState.Error("Error al subir las imágenes")
                    return@launch
                }

                val product = Product(
                    titulo = titulo,
                    descripcion = descripcion,
                    precio = precio,
                    cantidad = cantidad,
                    tipo = tipo,
                    categoria = categoria,
                    estado = "Publicado",
                    vendedorId = userId,
                    vendedorNombre = vendedorNombre,
                    vendedorTelefono = vendedorTelefono,
                    vendedorEmail = vendedorEmail,
                    imagenesUrls = imageUrls,
                    fechaPublicacion = System.currentTimeMillis()
                )

                withTimeout(10000) {
                    firestore.collection("products")
                        .add(product)
                        .await()
                }

                updateProductCount(userId)

                _productState.value = ProductState.Success("Producto publicado exitosamente")
                onSuccess()

            } catch (e: TimeoutCancellationException) {
                _productState.value = ProductState.Error("Tiempo de espera agotado")
                onError("Tiempo de espera agotado")
            } catch (e: Exception) {
                _productState.value = ProductState.Error("Error al publicar producto: ${e.message}")
                onError(e.message ?: "Error desconocido")
            }
        }
    }

    // Editar producto
    fun updateProduct(
        productId: String,
        titulo: String,
        descripcion: String,
        precio: Double,
        cantidad: Int,
        tipo: String,
        categoria: String,
        existingImageUrls: List<String>,
        newImageUris: List<Uri>,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                _productState.value = ProductState.Loading

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    onError("Usuario no autenticado")
                    _productState.value = ProductState.Error("Usuario no autenticado")
                    return@launch
                }

                // Subir nuevas imágenes si existen
                val newImageUrls = mutableListOf<String>()
                newImageUris.forEachIndexed { index, uri ->
                    try {
                        val storageRef = storage.reference
                            .child("product_photos")
                            .child("${userId}_${System.currentTimeMillis()}_$index.jpg")

                        withTimeout(30000) {
                            storageRef.putFile(uri).await()
                        }

                        val downloadUrl = storageRef.downloadUrl.await().toString()
                        newImageUrls.add(downloadUrl)
                    } catch (e: Exception) {
                        // Si falla una imagen, continuar
                    }
                }

                // Combinar URLs existentes con las nuevas
                val allImageUrls = existingImageUrls + newImageUrls

                if (allImageUrls.isEmpty()) {
                    onError("Debes tener al menos una imagen")
                    _productState.value = ProductState.Error("Debes tener al menos una imagen")
                    return@launch
                }

                // Actualizar en Firestore
                val updateData = hashMapOf<String, Any>(
                    "titulo" to titulo,
                    "descripcion" to descripcion,
                    "precio" to precio,
                    "cantidad" to cantidad,
                    "tipo" to tipo,
                    "categoria" to categoria,
                    "imagenesUrls" to allImageUrls
                )

                withTimeout(10000) {
                    firestore.collection("products")
                        .document(productId)
                        .update(updateData)
                        .await()
                }

                _productState.value = ProductState.Success("Producto actualizado exitosamente")
                onSuccess()

            } catch (e: TimeoutCancellationException) {
                _productState.value = ProductState.Error("Tiempo de espera agotado")
                onError("Tiempo de espera agotado")
            } catch (e: Exception) {
                _productState.value = ProductState.Error("Error al actualizar producto: ${e.message}")
                onError(e.message ?: "Error desconocido")
            }
        }
    }

    // 🆕 Eliminar producto CON sus imágenes
    fun deleteProduct(
        productId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                _productState.value = ProductState.Loading

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    onError("Usuario no autenticado")
                    return@launch
                }

                // 1. Obtener el producto para conseguir las URLs de las imágenes
                val productDoc = firestore.collection("products")
                    .document(productId)
                    .get()
                    .await()

                val product = productDoc.toObject(Product::class.java)

                // 2. Eliminar las imágenes de Storage
                product?.imagenesUrls?.forEach { imageUrl ->
                    try {
                        // Obtener la referencia de Storage desde la URL
                        val storageRef = storage.getReferenceFromUrl(imageUrl)
                        storageRef.delete().await()
                    } catch (e: Exception) {
                        // Si falla eliminar una imagen, continuar con las demás
                        // No queremos que falle todo el proceso si una imagen ya fue eliminada
                    }
                }

                // 3. Eliminar el documento de Firestore
                withTimeout(10000) {
                    firestore.collection("products")
                        .document(productId)
                        .delete()
                        .await()
                }

                // 4. Actualizar contador
                updateProductCount(userId)

                // 5. Recargar productos
                loadMyProducts()

                _productState.value = ProductState.Success("Producto eliminado exitosamente")
                onSuccess()

            } catch (e: Exception) {
                _productState.value = ProductState.Error("Error al eliminar producto: ${e.message}")
                onError(e.message ?: "Error desconocido")
            }
        }
    }

    // Actualizar contador de productos del vendedor
    private fun updateProductCount(userId: String) {
        viewModelScope.launch {
            try {
                val count = firestore.collection("products")
                    .whereEqualTo("vendedorId", userId)
                    .whereEqualTo("estado", "Publicado")
                    .get()
                    .await()
                    .size()

                firestore.collection("users")
                    .document(userId)
                    .update("productosPublicados", count)
                    .await()

            } catch (e: Exception) {
                // Ignorar errores al actualizar contador
            }
        }
    }

    fun resetProductState() {
        _productState.value = ProductState.Idle
    }
}