package com.example.tadeoshopapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.TextFieldDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessagesScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    productViewModel: ProductViewModel,
    otherUserName: String = "Usuario",
    otherUserId: String
) {
    val messagesViewModel: MessagesViewModel = viewModel()
    val messages by messagesViewModel.messages.collectAsState()
    val isOtherTyping by messagesViewModel.isOtherTyping.collectAsState()
    val otherUserPhotoUrl by messagesViewModel.otherUserPhotoUrl.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var messageText by remember { mutableStateOf("") }
    val context = LocalContext.current

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }

    // Launchers para adjuntar archivos e imágenes
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Enviar imagen automáticamente
            messagesViewModel.sendMessageWithImage(it, otherUserId)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Enviar imagen automáticamente
            messagesViewModel.sendMessageWithImage(it, otherUserId)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // TODO: Enviar archivo (por ahora solo imágenes)
            messageText = "[Archivo adjunto]"
        }
    }

    // Cargar mensajes al iniciar
    LaunchedEffect(otherUserId) {
        messagesViewModel.loadMessages(otherUserId)
    }

    // Auto-scroll al último mensaje
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Foto de perfil del otro usuario
                        if (!otherUserPhotoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = otherUserPhotoUrl,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.ic_logo_mensaje)
                            )
                        } else {
                            // Ícono de perfil vacío cuando no hay foto
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = Color(0xFFE0E0E0)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Perfil sin foto",
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxSize(),
                                    tint = Color(0xFF9E9E9E)
                                )
                            }
                        }
                        Text(
                            text = otherUserName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                backgroundColor = Color.White,
                elevation = 4.dp
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // Área de mensajes
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isFromCurrentUser = message.senderId == currentUserId,
                        onImageClick = { imageUrl -> expandedImageUrl = imageUrl }
                    )
                }

                // Indicador de "escribiendo..."
                if (isOtherTyping) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // Input area
            MessageInputBar(
                messageText = messageText,
                onMessageTextChange = { text ->
                    messageText = text
                    if (text.isNotEmpty()) {
                        messagesViewModel.startTyping(otherUserId)
                    } else {
                        messagesViewModel.stopTyping()
                    }
                },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        messagesViewModel.sendMessage(messageText, otherUserId)
                        messageText = ""
                        messagesViewModel.stopTyping()
                    }
                },
                onAttachFileClick = { filePickerLauncher.launch("*/*") },
                onCameraClick = { imagePickerLauncher.launch("image/*") }
            )
        }

        // Diálogo de imagen expandida
        expandedImageUrl?.let { imageUrl ->
            ImageExpandDialog(
                imageUrl = imageUrl,
                onDismiss = { expandedImageUrl = null }
            )
        }
    }
}

@Composable
fun MessageBubble(message: Message, isFromCurrentUser: Boolean, onImageClick: (String) -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        // Espacio para el avatar o para mejor alineación
        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f, false),
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
        ) {
            // Burbuja de mensaje
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isFromCurrentUser) 18.dp else 4.dp,
                    bottomEnd = if (isFromCurrentUser) 4.dp else 18.dp
                ),
                color = if (message.messageType == "image")
                    Color.Transparent
                else if (isFromCurrentUser)
                    Color(0xFF00897B)
                else
                    Color(0xFFF5F5F5),
                elevation = 1.dp,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                if (message.messageType == "image" && !message.imageUrl.isNullOrEmpty()) {
                    // Mostrar imagen
                    Box(modifier = Modifier.width(280.dp)) {
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Imagen del chat",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onImageClick(message.imageUrl) },
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = android.R.drawable.ic_menu_camera),
                            error = painterResource(id = android.R.drawable.ic_dialog_alert)
                        )
                    }

                    // Mostrar caption si existe
                    if (message.text.isNotBlank() && message.text != "Imagen") {
                        Text(
                            text = message.text,
                            color = if (isFromCurrentUser) Color.White else Color(0xFF212121),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                } else {
                    // Mostrar texto normal
                    Text(
                        text = message.text,
                        color = if (isFromCurrentUser) Color.White else Color(0xFF212121),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp
                    )
                }
            }

            // Timestamp
            Text(
                text = formatTimestamp(message.timestamp),
                fontSize = 11.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontWeight = FontWeight.Light
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFF5F5F5),
            modifier = Modifier.padding(horizontal = 4.dp),
            elevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TypingDot(delay = 0)
                TypingDot(delay = 200)
                TypingDot(delay = 400)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
fun TypingDot(delay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = delay, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .scale(scale)
            .background(Color(0xFF757575), shape = androidx.compose.foundation.shape.CircleShape)
    )
}

@Composable
fun MessageInputBar(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachFileClick: () -> Unit = {},
    onCameraClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botón adjuntar archivo
            IconButton(
                onClick = onAttachFileClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Adjuntar",
                    tint = Color(0xFF00897B),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Campo de texto
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Escribe un mensaje...",
                        color = Color(0xFFBDBDBD),
                        fontSize = 15.sp
                    )
                },
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF00897B),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    cursorColor = Color(0xFF00897B),
                    textColor = Color(0xFF212121)
                ),
                trailingIcon = {
                    IconButton(onClick = onCameraClick) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Cámara",
                            tint = Color(0xFF00897B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )

            // Botón enviar
            FloatingActionButton(
                onClick = onSendClick,
                backgroundColor = if (messageText.isNotBlank()) Color(0xFF00897B) else Color(0xFFBDBDBD),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Enviar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ImageExpandDialog(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Imagen expandida",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Botón de cerrar
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - timestamp

    return when {
        diff < 60000 -> "Ahora"
        diff < 3600000 -> "${(diff / 60000).toInt()} min"
        diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(date)
    }
}