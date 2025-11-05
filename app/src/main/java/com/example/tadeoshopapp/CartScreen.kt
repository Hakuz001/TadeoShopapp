package com.example.tadeoshopapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
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
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.util.*

@Composable
fun CartScreen(
    onBackClick: () -> Unit,
    onCheckoutSuccess: () -> Unit,
    cartViewModel: CartViewModel = viewModel()
) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    val cartTotal by cartViewModel.cartTotal.collectAsState()
    val cartState by cartViewModel.cartState.collectAsState()

    var showCheckoutDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    //Resetear estado al entrar a la pantalla
    LaunchedEffect(Unit) {
        cartViewModel.resetCartState()
    }

    // Manejar estados
    LaunchedEffect(cartState) {
        when (cartState) {
            is CartState.CheckoutSuccess -> { // Solo navegar cuando es CheckoutSuccess
                onCheckoutSuccess()
                cartViewModel.resetCartState()
            }
            is CartState.ItemAdded -> { //Cuando se agrega item, solo resetear
                // No hacer nada, el mensaje se muestra en ProductDetailScreen
                cartViewModel.resetCartState()
            }
            is CartState.Error -> {
                errorMessage = (cartState as CartState.Error).message
            }
            else -> {}
        }
    }

    // Diálogo de confirmación de compra
    if (showCheckoutDialog) {
        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            shape = RoundedCornerShape(20.dp),
            backgroundColor = Color.White,
            title = {
                Text(
                    text = stringResource(R.string.checkout_confirm_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.checkout_confirm_message),
                    fontSize = 15.sp,
                    color = Color(0xFF666666)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCheckoutDialog = false
                        cartViewModel.checkout(
                            onSuccess = {
                                // Se maneja en LaunchedEffect
                            },
                            onError = { error ->
                                errorMessage = error
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF00ACC1)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.confirm_button),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCheckoutDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cancel_button),
                        color = Color(0xFF666666)
                    )
                }
            }
        )
    }

    // Diálogo de error
    errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            shape = RoundedCornerShape(20.dp),
            backgroundColor = Color.White,
            title = {
                Text(
                    text = stringResource(R.string.error_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53935)
                )
            },
            text = {
                Text(
                    text = error,
                    fontSize = 15.sp,
                    color = Color(0xFF666666)
                )
            },
            confirmButton = {
                Button(
                    onClick = { errorMessage = null },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF00ACC1)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.understood_button),
                        color = Color.White
                    )
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.cart_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total de productos ${cartItems.sumOf { it.quantity }}",
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back_button),
                            tint = Color(0xFF00ACC1)
                        )
                    }
                },
                backgroundColor = Color.White,
                elevation = 0.dp
            )
        }
    ) { paddingValues ->
        if (cartItems.isEmpty()) {
            // Carrito vacío
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Círculo con ícono de carrito
                    Surface(
                        modifier = Modifier.size(240.dp),
                        shape = RoundedCornerShape(120.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(4.dp, Color(0xFF00ACC1))
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(40.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_dog_empty),  // ⭐ TU IMAGEN
                                contentDescription = stringResource(R.string.empty_cart_message),
                                modifier = Modifier.size(140.dp)
                                // SIN tint para que se vea en sus colores originales
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(140.dp))

                    // Info de entrega
                    Text(
                        text = stringResource(R.string.delivery_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.delivery_info),
                        fontSize = 13.sp,
                        color = Color(0xFF666666)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Total $0
                    Text(
                        text = stringResource(R.string.cart_total_label),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    Text(
                        text = "$0",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Botón VOLVER AL MENÚ
                    Button(
                        onClick = onBackClick,
                        modifier = Modifier
                            .width(280.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF00ACC1)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = "VOLVER AL MENÚ",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Carrito con productos
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.White)
            ) {
                // Lista de productos
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(cartItems) { cartItem ->
                        CartItemCard(
                            cartItem = cartItem,
                            onIncrement = { cartViewModel.incrementQuantity(cartItem.productId) },
                            onDecrement = { cartViewModel.decrementQuantity(cartItem.productId) },
                            onRemove = { cartViewModel.removeFromCart(cartItem.productId) }
                        )
                    }

                    // Botón eliminar todos
                    item {
                        TextButton(
                            onClick = { cartViewModel.clearCart() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.remove_all_button),
                                color = Color(0xFF00ACC1),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Sección de entrega y total
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(20.dp)
                ) {
                    // Información de entrega
                    Text(
                        text = stringResource(R.string.delivery_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.delivery_info),
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFFE0E0E0))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.cart_total_label),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                        Text(
                            text = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                                .format(cartTotal),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Botón finalizar compra
                    Button(
                        onClick = { showCheckoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = cartState !is CartState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF00ACC1),
                            disabledBackgroundColor = Color(0xFFCCCCCC)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        if (cartState is CartState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.checkout_button),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    cartItem: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Imagen del producto
        if (cartItem.product.imagenesUrls.isNotEmpty()) {
            AsyncImage(
                model = cartItem.product.imagenesUrls.first(),
                contentDescription = cartItem.product.titulo,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_photo),
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Información del producto
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = cartItem.product.titulo,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Información de tipo y stock
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Badge de tipo (Nuevo/Usado)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (cartItem.product.tipo == "Nuevo") Color(0xFFE3F2FD) else Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = cartItem.product.tipo,
                        fontSize = 11.sp,
                        color = if (cartItem.product.tipo == "Nuevo") Color(0xFF1976D2) else Color(0xFFF57C00),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Información de stock disponible
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (cartItem.product.cantidad > 0) Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (cartItem.product.cantidad > 0) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = if (cartItem.product.cantidad > 0) "Disponible" else "Agotado",
                            tint = if (cartItem.product.cantidad > 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (cartItem.product.cantidad > 0) "${cartItem.product.cantidad} disponibles" else "Agotado",
                            fontSize = 11.sp,
                            color = if (cartItem.product.cantidad > 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                    .format(cartItem.product.precio),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Controles de cantidad
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón menos
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = stringResource(R.string.decrease_quantity),
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Cantidad con indicador de límite
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cartItem.quantity.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cartItem.quantity >= cartItem.product.cantidad) Color(0xFFE53935) else Color(0xFF212121),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    // Indicador de límite alcanzado
                    if (cartItem.quantity >= cartItem.product.cantidad) {
                        Text(
                            text = "(máx)",
                            fontSize = 12.sp,
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Botón más (deshabilitado si alcanzó el límite)
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(32.dp),
                    enabled = cartItem.quantity < cartItem.product.cantidad
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.increase_quantity),
                        tint = if (cartItem.quantity < cartItem.product.cantidad) Color(0xFF00ACC1) else Color(0xFFCCCCCC),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}