package com.example.tadeoshopapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MyPurchasesScreen(
    onBackClick: () -> Unit,
    ordersViewModel: OrdersViewModel // ⭐ RECIBIR como parámetro, no crear con viewModel()
) {
    val myPurchases by ordersViewModel.myPurchases.collectAsState()
    val ordersState by ordersViewModel.ordersState.collectAsState()

    LaunchedEffect(Unit) {
        ordersViewModel.loadMyPurchases()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mis Compras",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF00ACC1)
                        )
                    }
                },
                backgroundColor = Color.White,
                elevation = 4.dp
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(paddingValues)
        ) {
            when {
                ordersState is OrdersState.Loading && myPurchases.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00ACC1))
                    }
                }
                myPurchases.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_dog_logo),
                            contentDescription = "Sin compras",
                            modifier = Modifier.size(120.dp),
                            tint = Color(0xFF00ACC1)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No tienes compras",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tus compras aparecerán aquí",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(myPurchases) { order ->
                            PurchaseOrderCard(order = order)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseOrderCard(order: Order) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CO"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 3.dp,
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // fecha y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Orden #${order.id.take(8)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Text(
                        text = dateFormat.format(Date(order.fechaCreacion)),
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (order.estado) {
                        "Pendiente" -> Color(0xFFFFF3E0)
                        "Confirmado" -> Color(0xFFE8F5E9)
                        "Entregado" -> Color(0xFF4CAF50)
                        "Cancelado" -> Color(0xFFFFEBEE)
                        else -> Color(0xFFF5F5F5)
                    }
                ) {
                    Text(
                        text = order.estado,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (order.estado) {
                            "Pendiente" -> Color(0xFFF57C00)
                            "Confirmado" -> Color(0xFF4CAF50)
                            "Entregado" -> Color.White
                            "Cancelado" -> Color(0xFFE53935)
                            else -> Color(0xFF666666)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // Vendedor
            Text(
                text = "Vendedor: ${order.vendedorNombre}",
                fontSize = 14.sp,
                color = Color(0xFF00ACC1),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de productos
            order.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Imagen
                    if (item.product.imagenesUrls.isNotEmpty()) {
                        AsyncImage(
                            model = item.product.imagenesUrls.first(),
                            contentDescription = item.product.titulo,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE0E0E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sin foto",
                                fontSize = 10.sp,
                                color = Color(0xFF999999)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Info del producto
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.product.titulo,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF212121),
                            maxLines = 2
                        )
                        Text(
                            text = "Cantidad: ${item.quantity}",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }

                    // Precio
                    Text(
                        text = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                            .format(item.product.precio * item.quantity),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                Text(
                    text = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                        .format(order.total),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00ACC1)
                )
            }
        }
    }
}