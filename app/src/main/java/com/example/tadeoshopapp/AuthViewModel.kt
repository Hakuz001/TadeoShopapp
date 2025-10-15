package com.example.tadeoshopapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
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
    val tipoUsuario: String = "Comprador"
)

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            loadUserData(firebaseUser.uid)
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
                android.util.Log.d("AuthViewModel", "Starting registration...")
                _authState.value = AuthState.Loading

                android.util.Log.d("AuthViewModel", "Creating user with email: $email")
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                android.util.Log.d("AuthViewModel", "User created: ${firebaseUser?.uid}")

                if (firebaseUser != null) {
                    val user = User(
                        uid = firebaseUser.uid,
                        nombres = nombres,
                        apellidos = apellidos,
                        email = email,
                        tipoUsuario = tipoUsuario
                    )

                    try {
                        android.util.Log.d("AuthViewModel", "Saving user to Firestore...")

                        withTimeout(30000) {
                            firestore.collection("users")
                                .document(firebaseUser.uid)
                                .set(user)
                                .await()
                        }

                        android.util.Log.d("AuthViewModel", "User saved to Firestore successfully")
                    } catch (e: TimeoutCancellationException) {
                        android.util.Log.w("AuthViewModel", "Timeout saving to Firestore, but user was created in Auth")
                    }

                    _currentUser.value = user
                    // NOTA: Este string no se puede usar aquí directamente porque necesitarías Context
                    // Lo dejaremos así por ahora, o puedes crear una versión que acepte Context
                    _authState.value = AuthState.Success("Usuario registrado exitosamente")
                    android.util.Log.d("AuthViewModel", "State set to Success")
                } else {
                    android.util.Log.e("AuthViewModel", "Firebase user is null")
                    _authState.value = AuthState.Error("Error al crear usuario")
                }

            } catch (e: FirebaseAuthException) {
                android.util.Log.e("AuthViewModel", "FirebaseAuthException: ${e.errorCode} - ${e.message}")
                _authState.value = AuthState.Error(
                    when (e.errorCode) {
                        "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo ya está registrado"
                        "ERROR_WEAK_PASSWORD" -> "La contraseña debe tener al menos 6 caracteres"
                        "ERROR_INVALID_EMAIL" -> "Correo electrónico inválido"
                        else -> "Error de registro: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Exception: ${e.message}", e)
                _authState.value = AuthState.Error("Error: ${e.message}")
            }
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

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

                val user = document.toObject(User::class.java)
                _currentUser.value = user

            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error al cargar datos: ${e.message}")
            }
        }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("AuthViewModel", "Sending password reset email to: $email")
                _authState.value = AuthState.Loading

                auth.sendPasswordResetEmail(email).await()

                android.util.Log.d("AuthViewModel", "Password reset email sent successfully")
                _authState.value = AuthState.Success("Correo de recuperación enviado")

            } catch (e: FirebaseAuthException) {
                android.util.Log.e("AuthViewModel", "FirebaseAuthException: ${e.errorCode} - ${e.message}")
                _authState.value = AuthState.Error(
                    when (e.errorCode) {
                        "ERROR_USER_NOT_FOUND" -> "No existe una cuenta con este correo"
                        "ERROR_INVALID_EMAIL" -> "Correo electrónico inválido"
                        else -> "Error al enviar correo: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Exception: ${e.message}", e)
                _authState.value = AuthState.Error("Error: ${e.message}")
            }
        }
    }
}