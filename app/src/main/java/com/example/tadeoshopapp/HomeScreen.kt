package com.example.tadeoshopapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: AuthViewModel,
    productViewModel: ProductViewModel,
    cartViewModel: CartViewModel,
    initialTab: Int = 2
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    Scaffold(
        bottomBar = {
            BottomNavigation(
                backgroundColor = Color.White,
                elevation = 8.dp
            ) {
                // Mensajes
                BottomNavigationItem(
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo_mensaje),
                            contentDescription = stringResource(R.string.nav_messages),
                            modifier = Modifier.size(24.dp),
                            alpha = if (selectedTab == 0) 1f else 0.5f
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_messages),
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    selectedContentColor = Color(0xFF00ACC1),
                    unselectedContentColor = Color(0xFF999999)
                )

                //  Perfil
                BottomNavigationItem(
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo_perfil),
                            contentDescription = stringResource(R.string.nav_profile),
                            modifier = Modifier.size(24.dp),
                            alpha = if (selectedTab == 1) 1f else 0.5f
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_profile),
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    selectedContentColor = Color(0xFF00ACC1),
                    unselectedContentColor = Color(0xFF999999)
                )

                //  Productos (Marketplace)
                BottomNavigationItem(
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo_productos),
                            contentDescription = stringResource(R.string.nav_products),
                            modifier = Modifier.size(24.dp),
                            alpha = if (selectedTab == 2) 1f else 0.5f
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_products),
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    selectedContentColor = Color(0xFF00ACC1),
                    unselectedContentColor = Color(0xFF999999)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> MessagesListScreen(
                    navController = navController,
                    authViewModel = viewModel,
                    productViewModel = productViewModel
                )
                1 -> ProfileScreen(
                    onEditProfileClick = {
                        navController.navigate(Screen.EditProfile.route)
                    },
                    onLogoutClick = {
                        viewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onViewProductsClick = {
                        navController.navigate(Screen.Products.route)
                    },
                    //Callbacks para órdenes
                    onMyPurchasesClick = {
                        navController.navigate(Screen.MyPurchases.route)
                    },
                    onMySalesClick = {
                        navController.navigate(Screen.MySales.route)
                    },
                    viewModel = viewModel
                )
                2 -> MarktekPlaceScreen(
                    navController = navController,
                    onCartClick = {
                        navController.navigate(Screen.Cart.route)
                    },
                    productViewModel = productViewModel,
                    cartViewModel = cartViewModel
                )
            }
        }
    }
}

@Composable
fun MessagesListScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    productViewModel: ProductViewModel
) {
    val messagesViewModel: MessagesViewModel = viewModel()
    val conversations by messagesViewModel.conversations.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Filtrar conversaciones basado en la búsqueda
    val filteredConversations = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) {
            conversations
        } else {
            conversations.filter { conversation ->
                conversation.otherUserName.contains(searchQuery, ignoreCase = true) ||
                        conversation.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        messagesViewModel.loadConversations()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header con búsqueda
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            elevation = 4.dp
        ) {
            Column {
                Text(
                    text = stringResource(R.string.nav_messages),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    color = Color(0xFF212121)
                )

                // Barra de búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = {
                        Text(
                            text = "Buscar conversaciones...",
                            color = Color(0xFFBDBDBD),
                            fontSize = 15.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = Color(0xFF757575)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF00897B),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        cursorColor = Color(0xFF00897B),
                        textColor = Color(0xFF212121)
                    )
                )
            }
        }

        // Lista de conversaciones
        if (filteredConversations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_dog_logo),
                        contentDescription = "Sin mensajes",
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No se encontraron resultados" else "No tienes conversaciones",
                        fontSize = 18.sp,
                        color = Color(0xFF666666)
                    )
                    if (searchQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Intenta con otro término de búsqueda",
                            fontSize = 14.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredConversations) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        onClick = {
                            navController.navigate(
                                Screen.MessagesBase.createRoute(
                                    conversation.otherUserId,
                                    conversation.otherUserName
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        backgroundColor = Color.White,
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto de perfil con borde
            Box(
                modifier = Modifier.size(64.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFF00897B), CircleShape),
                    shape = CircleShape
                ) {
                    if (!conversation.otherUserPhotoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = conversation.otherUserPhotoUrl,
                            contentDescription = "Foto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.ic_dog_logo)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_dog_logo),
                            contentDescription = "Avatar",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Nombre y último mensaje
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = conversation.otherUserName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = conversation.lastMessage,
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            // Timestamp y badge
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatConversationTime(conversation.lastMessageTime),
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E),
                    fontWeight = FontWeight.Medium
                )
                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00897B),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${conversation.unreadCount}",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatConversationTime(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val now = java.util.Date()
    val diff = now.time - timestamp

    return when {
        diff < 60000 -> "Ahora"
        diff < 3600000 -> "${(diff / 60000).toInt()}m"
        diff < 86400000 -> "${(diff / 3600000).toInt()}h"
        diff < 604800000 -> "${(diff / 86400000).toInt()}d"
        else -> java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(date)
    }
}