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
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.util.*

data class Message(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val chatId: String = "",
    val imageUrl: String? = null,
    val messageType: String = "text" // "text" o "image"
)

data class Conversation(
    val chatId: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserPhotoUrl: String? = null,
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0
)

sealed class MessagesState {
    object Idle : MessagesState()
    object Loading : MessagesState()
    data class Success(val message: String) : MessagesState()
    data class Error(val message: String) : MessagesState()
}

class MessagesViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private val _messagesState = MutableStateFlow<MessagesState>(MessagesState.Idle)
    val messagesState: StateFlow<MessagesState> = _messagesState

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isOtherTyping = MutableStateFlow(false)
    val isOtherTyping: StateFlow<Boolean> = _isOtherTyping

    private val _otherUserPhotoUrl = MutableStateFlow<String?>(null)
    val otherUserPhotoUrl: StateFlow<String?> = _otherUserPhotoUrl

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    private var chatId: String? = null
    private var otherUserId: String? = null

    fun loadMessages(otherUserId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("MessagesVM", "Loading messages for otherUserId: $otherUserId")
                _messagesState.value = MessagesState.Loading

                val currentUserId = auth.currentUser?.uid
                if (currentUserId == null) {
                    android.util.Log.e("MessagesVM", "No current user")
                    _messagesState.value = MessagesState.Error("Usuario no autenticado")
                    return@launch
                }

                this@MessagesViewModel.otherUserId = otherUserId

                // Crear ID único para el chat entre dos usuarios
                chatId = getChatId(currentUserId, otherUserId)
                android.util.Log.d("MessagesVM", "ChatId: $chatId")

                // Cargar datos del otro usuario (nombre, foto, etc.)
                loadOtherUserData(otherUserId)

                // Cargar mensajes
                val snapshot = firestore.collection("messages")
                    .whereEqualTo("chatId", chatId)
                    .get()
                    .await()

                android.util.Log.d("MessagesVM", "Messages snapshot size: ${snapshot.documents.size}")

                val messagesList = snapshot.documents.mapNotNull { doc ->
                    val message = doc.toObject(Message::class.java)?.copy(id = doc.id)
                    android.util.Log.d("MessagesVM", "Loaded message: id=${message?.id}, type=${message?.messageType}, text=${message?.text}, imageUrl=${message?.imageUrl}")
                    message
                }
                    .sortedBy { it.timestamp }

                _messages.value = messagesList
                android.util.Log.d("MessagesVM", "Loaded ${messagesList.size} messages")
                _messagesState.value = MessagesState.Success("Mensajes cargados")

                // Escuchar nuevos mensajes en tiempo real
                setupMessagesListener()

                // Escuchar indicador de typing
                setupTypingListener()

            } catch (e: Exception) {
                android.util.Log.e("MessagesVM", "Error loading messages", e)
                _messagesState.value = MessagesState.Error("Error al cargar mensajes: ${e.message}")
            }
        }
    }

    private fun loadOtherUserData(otherUserId: String) {
        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users")
                    .document(otherUserId)
                    .get()
                    .await()

                if (userDoc.exists()) {
                    val photoUrl = userDoc.getString("photoUrl")
                    _otherUserPhotoUrl.value = photoUrl
                }
            } catch (e: Exception) {
                // Error silencioso
            }
        }
    }

    private fun setupMessagesListener() {
        viewModelScope.launch {
            try {
                chatId?.let { id ->
                    firestore.collection("messages")
                        .whereEqualTo("chatId", id)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                android.util.Log.e("MessagesVM", "Error in listener: ${error.message}")
                                return@addSnapshotListener
                            }

                            snapshot?.let {
                                val messagesList = it.documents.mapNotNull { doc ->
                                    val message = doc.toObject(Message::class.java)?.copy(id = doc.id)
                                    android.util.Log.d("MessagesVM", "Loaded message: id=${message?.id}, type=${message?.messageType}, text=${message?.text}, imageUrl=${message?.imageUrl}")
                                    message
                                }
                                    .sortedBy { it.timestamp }
                                android.util.Log.d("MessagesVM", "Listener: Updated ${messagesList.size} messages")
                                _messages.value = messagesList
                            }
                        }
                }
            } catch (e: Exception) {
                _messagesState.value = MessagesState.Error("Error al configurar listener")
            }
        }
    }

    private fun setupTypingListener() {
        viewModelScope.launch {
            try {
                chatId?.let { id ->
                    val currentUserId = auth.currentUser?.uid
                    firestore.collection("typing")
                        .document(id)
                        .addSnapshotListener { snapshot, _ ->
                            snapshot?.let { doc ->
                                if (doc.exists()) {
                                    val data = doc.data
                                    // Verificar si el otro usuario está escribiendo
                                    val otherUserTyping = data?.get(otherUserId) as? Boolean ?: false
                                    _isOtherTyping.value = otherUserTyping && currentUserId != otherUserId
                                } else {
                                    _isOtherTyping.value = false
                                }
                            }
                        }
                }
            } catch (e: Exception) {
                // Error silencioso para typing
            }
        }
    }

    fun sendMessage(text: String, otherUserId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid
                if (currentUserId == null) {
                    _messagesState.value = MessagesState.Error("Usuario no autenticado")
                    return@launch
                }

                if (text.isBlank()) {
                    return@launch
                }

                this@MessagesViewModel.otherUserId = otherUserId
                chatId = getChatId(currentUserId, otherUserId)

                val messageMap = mapOf(
                    "text" to text.trim(),
                    "senderId" to currentUserId,
                    "receiverId" to otherUserId,
                    "timestamp" to System.currentTimeMillis(),
                    "chatId" to chatId!!,
                    "isRead" to false
                )

                firestore.collection("messages").add(messageMap).await()

                // Actualizar timestamp del chat
                firestore.collection("chats")
                    .document(chatId!!)
                    .set(
                        mapOf(
                            "lastMessage" to text.trim(),
                            "lastMessageTime" to System.currentTimeMillis(),
                            "participants" to listOf(currentUserId, otherUserId)
                        )
                    )

                // Detener typing
                stopTyping()

                _messagesState.value = MessagesState.Success("Mensaje enviado")

            } catch (e: Exception) {
                _messagesState.value = MessagesState.Error("Error al enviar mensaje: ${e.message}")
            }
        }
    }

    fun sendMessageWithImage(imageUri: Uri, otherUserId: String, caption: String = "") {
        viewModelScope.launch {
            try {
                android.util.Log.d("MessagesVM", "=== START: sendMessageWithImage ===")
                android.util.Log.d("MessagesVM", "URI: $imageUri")
                _messagesState.value = MessagesState.Loading
                
                val currentUserId = auth.currentUser?.uid
                if (currentUserId == null) {
                    android.util.Log.e("MessagesVM", "No current user")
                    _messagesState.value = MessagesState.Error("Usuario no autenticado")
                    return@launch
                }

                this@MessagesViewModel.otherUserId = otherUserId
                chatId = getChatId(currentUserId, otherUserId)
                android.util.Log.d("MessagesVM", "ChatId: $chatId")

                // Subir imagen a Firebase Storage
                val imageFileName = "chat_${chatId}_${System.currentTimeMillis()}.jpg"
                val imageRef = storage.reference.child("chat_images").child(imageFileName)
                android.util.Log.d("MessagesVM", "Uploading to: chat_images/$imageFileName")
                
                withTimeout(60000) {
                    imageRef.putFile(imageUri).await()
                    android.util.Log.d("MessagesVM", "Upload successful, getting download URL...")
                }
                
                val imageUrl = imageRef.downloadUrl.await().toString()
                android.util.Log.d("MessagesVM", "Download URL: $imageUrl")

                // Crear mensaje con imagen
                val messageMap = mapOf(
                    "text" to if (caption.isNotBlank()) caption else "Imagen",
                    "senderId" to currentUserId,
                    "receiverId" to otherUserId,
                    "timestamp" to System.currentTimeMillis(),
                    "chatId" to chatId!!,
                    "isRead" to false,
                    "imageUrl" to imageUrl,
                    "messageType" to "image"
                )
                android.util.Log.d("MessagesVM", "Message map created, adding to Firestore...")

                firestore.collection("messages").add(messageMap).await()
                android.util.Log.d("MessagesVM", "Message added to Firestore")

                // Actualizar timestamp del chat
                firestore.collection("chats")
                    .document(chatId!!)
                    .set(
                        mapOf(
                            "lastMessage" to if (caption.isNotBlank()) caption else "📷 Imagen",
                            "lastMessageTime" to System.currentTimeMillis(),
                            "participants" to listOf(currentUserId, otherUserId)
                        )
                    )
                android.util.Log.d("MessagesVM", "Chat updated")

                // Detener typing
                stopTyping()

                android.util.Log.d("MessagesVM", "=== SUCCESS: Image sent ===")
                _messagesState.value = MessagesState.Success("Imagen enviada")

            } catch (e: TimeoutCancellationException) {
                android.util.Log.e("MessagesVM", "=== ERROR: Timeout uploading image ===")
                _messagesState.value = MessagesState.Error("La imagen es muy grande o la conexión es lenta")
            } catch (e: Exception) {
                android.util.Log.e("MessagesVM", "=== ERROR: Error sending image ===")
                android.util.Log.e("MessagesVM", "Exception: ${e.javaClass.simpleName}")
                android.util.Log.e("MessagesVM", "Message: ${e.message}")
                android.util.Log.e("MessagesVM", "Stack trace:", e)
                _messagesState.value = MessagesState.Error("Error al enviar imagen: ${e.message}")
            }
        }
    }

    fun startTyping(otherUserId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                chatId = getChatId(currentUserId, otherUserId)

                firestore.collection("typing")
                    .document(chatId!!)
                    .set(mapOf(currentUserId to true), com.google.firebase.firestore.SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                // Error silencioso
            }
        }
    }

    fun stopTyping() {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                chatId?.let { id ->
                    firestore.collection("typing")
                        .document(id)
                        .update(currentUserId, false)
                }
            } catch (e: Exception) {
                // Error silencioso
            }
        }
    }

    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "${userId1}_$userId2"
        } else {
            "${userId2}_$userId1"
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid
                if (currentUserId == null) return@launch

                // Cargar conversaciones (chats)
                val chatsSnapshot = firestore.collection("chats")
                    .whereArrayContains("participants", currentUserId)
                    .get()
                    .await()

                val conversationsList = mutableListOf<Conversation>()
                
                for (doc in chatsSnapshot.documents) {
                    try {
                        val data = doc.data ?: continue
                        val participants = data["participants"] as? List<String> ?: continue
                        
                        // Encontrar el ID del otro usuario
                        val otherUserIdInChat = participants.find { it != currentUserId }
                            ?: continue
                        
                        // Cargar datos del otro usuario
                        val userDoc = firestore.collection("users")
                            .document(otherUserIdInChat)
                            .get()
                            .await()
                        
                        if (!userDoc.exists()) continue
                        
                        val userName = userDoc.getString("nombres") ?: "Usuario"
                        val userPhotoUrl = userDoc.getString("photoUrl")
                        val lastMessage = data["lastMessage"] as? String ?: ""
                        val lastMessageTime = (data["lastMessageTime"] as? Long) ?: 0L
                        
                        conversationsList.add(
                            Conversation(
                                chatId = doc.id,
                                otherUserId = otherUserIdInChat,
                                otherUserName = userName,
                                otherUserPhotoUrl = userPhotoUrl,
                                lastMessage = lastMessage,
                                lastMessageTime = lastMessageTime
                            )
                        )
                    } catch (e: Exception) {
                        // Saltar esta conversación si hay error
                        continue
                    }
                }
                
                // Ordenar por fecha descendente
                _conversations.value = conversationsList.sortedByDescending { it.lastMessageTime }
                
            } catch (e: Exception) {
                // Error silencioso
            }
        }
    }

    fun resetState() {
        _messagesState.value = MessagesState.Idle
        chatId = null
        otherUserId = null
    }
}

