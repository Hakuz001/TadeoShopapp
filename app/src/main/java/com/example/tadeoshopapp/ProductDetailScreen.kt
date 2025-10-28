package com.example.tadeoshopapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProductDetailScreen(
    product: Product,
    onBackClick: () -> Unit,
    onContactSeller: (String) -> Unit,
    onNavigateToMessages: () -> Unit,
    authViewModel: AuthViewModel,
    cartViewModel: CartViewModel // ⭐ RECIBIR como parámetro, no crear con viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val cartItems by cartViewModel.cartItems.collectAsState()

    // Verificar si está en el carrito
    val isInCart = cartItems.any { it.productId == product.id }

    val scrollState = rememberScrollState()
    var showAddedToCartSnackbar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detalles del Producto",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                elevation = 4.dp
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                // Carrusel de imágenes con LazyRow
                if (product.imagenesUrls.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(product.imagenesUrls) { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Imagen del producto",
                                modifier = Modifier
                                    .fillParentMaxWidth()
                                    .height(350.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                            .background(Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_photo),
                            fontSize = 16.sp,
                            color = Color(0xFF999999)
                        )
                    }
                }

                // Información del producto
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Título
                    Text(
                        text = product.titulo,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Precio
                    Text(
                        text = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                            .format(product.precio),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00ACC1)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Categoría
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF5F5F5)
                    ) {
                        Text(
                            text = product.categoria,
                            fontSize = 13.sp,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Descripción
                    Text(
                        text = "Descripción",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = product.descripcion,
                        fontSize = 15.sp,
                        color = Color(0xFF666666),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Divider(color = Color(0xFFE0E0E0))

                    Spacer(modifier = Modifier.height(20.dp))

                    // Información del vendedor
                    Text(
                        text = "Vendedor",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = product.vendedorNombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00ACC1)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CO"))
                    Text(
                        text = "Publicado el ${dateFormat.format(Date(product.fechaPublicacion))}",
                        fontSize = 13.sp,
                        color = Color(0xFF999999)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botones de acción
                    if (currentUser?.uid != product.vendedorId) {
                        // Usuario NO es el vendedor - Mostrar botones

                        // Botón Agregar al Carrito
                        Button(
                            onClick = {
                                if (!isInCart) {
                                    cartViewModel.addToCart(product)
                                    showAddedToCartSnackbar = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (isInCart) Color(0xFF4CAF50) else Color(0xFF00ACC1)
                            ),
                            shape = RoundedCornerShape(28.dp),
                            elevation = ButtonDefaults.elevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Icon(
                                imageVector = if (isInCart) Icons.Default.Check else Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isInCart) "✓ Agregado al carrito" else stringResource(R.string.add_to_cart_button),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Botón Contactar Vendedor
                        OutlinedButton(
                            onClick = { onContactSeller(product.vendedorId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF00ACC1)
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.contact_seller_button),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Usuario ES el vendedor - Mostrar mensaje
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF5F5F5)
                        ) {
                            Text(
                                text = "Este es tu producto",
                                fontSize = 15.sp,
                                color = Color(0xFF666666),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Snackbar cuando se agrega al carrito
            if (showAddedToCartSnackbar) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showAddedToCartSnackbar = false
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF4CAF50),
                        elevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Producto agregado al carrito",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}