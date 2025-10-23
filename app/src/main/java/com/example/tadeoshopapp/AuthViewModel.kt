package com.example.tadeoshopapp

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class User(
    val uid: String = "",
    val nombres: String = "",
    val apellidos: String = "",
    val email: String = "",
    val tipoUsuario: String = "Comprador",
    val fechaRegistro: Long = System.currentTimeMillis(),
    val biografia: String = "",
    val telefono: String = "",
    val productosPublicados: Int = 0,
    val photoUrl: String = "" // URL de la foto de perfil
)

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                loadUserData(firebaseUser.uid)
            }
        }
    }

    fun registerUser(
        nombres: String,
        apellidos: String,
        email: String,
        password: String,
        tipoUsuario: String
    ) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user

                if (firebaseUser != null) {
                    val user = User(
                        uid = firebaseUser.uid,
                        nombres = nombres,
                        apellidos = apellidos,
                        email = email,
                        tipoUsuario = tipoUsuario,
                        fechaRegistro = System.currentTimeMillis()
                    )

                    try {
                        withTimeout(30000) {
                            firestore.collection("users")
                                .document(firebaseUser.uid)
                                .set(user)
                                .await()
                        }
                    } catch (e: TimeoutCancellationException) {
                        // Usuario creado en Auth pero con timeout en Firestore
                    }

                    _currentUser.value = user
                    _authState.value = AuthState.Success("Usuario registrado exitosamente")
                } else {
                    _authState.value = AuthState.Error("Error al crear usuario")
                }

            } catch (e: FirebaseAuthException) {
                _authState.value = AuthState.Error(
                    when (e.errorCode) {
                        "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo ya está registrado"
                        "ERROR_WEAK_PASSWORD" -> "La contraseña debe tener al menos 6 caracteres"
                        "ERROR_INVALID_EMAIL" -> "Correo electrónico inválido"
                        else -> "Error de registro: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error: ${e.message}")
            }
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                _currentUser.value = null

                val result = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user

                if (firebaseUser != null) {
                    loadUserData(firebaseUser.uid)
                    _authState.value = AuthState.Success("Inicio de sesión exitoso")
                } else {
                    _authState.value = AuthState.Error("Error al iniciar sesión")
                }

            } catch (e: FirebaseAuthException) {
                _authState.value = AuthState.Error(
                    when (e.errorCode) {
                        "ERROR_USER_NOT_FOUND" -> "Usuario no encontrado"
                        "ERROR_WRONG_PASSWORD" -> "Contraseña incorrecta"
                        "ERROR_INVALID_EMAIL" -> "Correo electrónico inválido"
                        "ERROR_USER_DISABLED" -> "Usuario deshabilitado"
                        else -> "Error de inicio de sesión: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error: ${e.message}")
            }
        }
    }

    private fun loadUserData(uid: String) {
        viewModelScope.launch {
            try {
                val document = firestore.collection("users")
                    .document(uid)
                    .get()
                    .await()

                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    _currentUser.value = user
                } else {
                    _authState.value = AuthState.Error("Usuario no encontrado en la base de datos")
                }

            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error al cargar datos: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            auth.signOut()
            _currentUser.value = null
            _authState.value = AuthState.Idle
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                auth.sendPasswordResetEmail(email).await()

                _authState.value = AuthState.Success("Correo de recuperación enviado")

            } catch (e: FirebaseAuthException) {
                _authState.value = AuthState.Error(
                    when (e.errorCode) {
                        "ERROR_USER_NOT_FOUND" -> "No existe una cuenta con este correo"
                        "ERROR_INVALID_EMAIL" -> "Correo electrónico inválido"
                        else -> "Error al enviar correo: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error: ${e.message}")
            }
        }
    }

    fun updateUserProfile(
        nombres: String,
        apellidos: String,
        telefono: String,
        biografia: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    onError("Usuario no autenticado")
                    _authState.value = AuthState.Error("Usuario no autenticado")
                    return@launch
                }

                val updates = hashMapOf<String, Any>(
                    "nombres" to nombres,
                    "apellidos" to apellidos,
                    "telefono" to telefono,
                    "biografia" to biografia
                )

                withTimeout(10000) {
                    firestore.collection("users")
                        .document(userId)
                        .update(updates)
                        .await()
                }

                // Recargar datos del usuario
                loadUserData(userId)

                _authState.value = AuthState.Success("Perfil actualizado exitosamente")
                onSuccess()

            } catch (e: TimeoutCancellationException) {
                _authState.value = AuthState.Error("Tiempo de espera agotado")
                onError("Tiempo de espera agotado")
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error al actualizar perfil: ${e.message}")
                onError(e.message ?: "Error desconocido")
            }
        }
    }

    // función:Subir foto de perfil
    fun uploadProfilePhoto(
        imageUri: Uri,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    onError("Usuario no autenticado")
                    _authState.value = AuthState.Error("Usuario no autenticado")
                    return@launch
                }

                // Referencia al Storage
                val storageRef = storage.reference
                    .child("profile_photos")
                    .child("$userId.jpg")

                // Subir imagen
                withTimeout(30000) {
                    storageRef.putFile(imageUri).await()
                }

                // Obtener URL de descarga
                val downloadUrl = storageRef.downloadUrl.await().toString()

                // Actualizar Firestore con la URL
                withTimeout(10000) {
                    firestore.collection("users")
                        .document(userId)
                        .update("photoUrl", downloadUrl)
                        .await()
                }

                // Recargar datos del usuario
                loadUserData(userId)

                _authState.value = AuthState.Success("Foto actualizada exitosamente")
                onSuccess()

            } catch (e: TimeoutCancellationException) {
                _authState.value = AuthState.Error("Tiempo de espera agotado")
                onError("Tiempo de espera agotado")
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error al subir foto: ${e.message}")
                onError(e.message ?: "Error desconocido")
            }
        }
    }
}