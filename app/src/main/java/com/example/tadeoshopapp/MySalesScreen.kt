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
fun MySalesScreen(
    onBackClick: () -> Unit,
    ordersViewModel: OrdersViewModel
) {
    val mySales by ordersViewModel.mySales.collectAsState()
    val ordersState by ordersViewModel.ordersState.collectAsState()

    LaunchedEffect(Unit) {
        ordersViewModel.loadMySales()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mis Ventas",
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
                ordersState is OrdersState.Loading && mySales.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00ACC1))
                    }
                }
                mySales.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_dog_logo),
                            contentDescription = "Sin ventas",
                            modifier = Modifier.size(120.dp),
                            tint = Color(0xFF00ACC1)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No tienes ventas",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tus ventas aparecerán aquí",
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
                        items(mySales) { order ->
                            SaleOrderCard(
                                order = order,
                                onUpdateStatus = { newStatus ->
                                    ordersViewModel.updateOrderStatus(order.id, newStatus)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaleOrderCard(
    order: Order,
    onUpdateStatus: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CO"))
    var showStatusMenu by remember { mutableStateOf(false) }

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

                // Botón para cambiar estado
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (order.estado) {
                            "Pendiente" -> Color(0xFFFFF3E0)
                            "Confirmado" -> Color(0xFFE8F5E9)
                            "Entregado" -> Color(0xFF4CAF50)
                            "Cancelado" -> Color(0xFFFFEBEE)
                            else -> Color(0xFFF5F5F5)
                        },
                        modifier = Modifier
                    ) {
                        TextButton(onClick = { showStatusMenu = true }) {
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
                                }
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            onUpdateStatus("Pendiente")
                            showStatusMenu = false
                        }) {
                            Text("Pendiente")
                        }
                        DropdownMenuItem(onClick = {
                            onUpdateStatus("Confirmado")
                            showStatusMenu = false
                        }) {
                            Text("Confirmado")
                        }
                        DropdownMenuItem(onClick = {
                            onUpdateStatus("Entregado")
                            showStatusMenu = false
                        }) {
                            Text("Entregado")
                        }
                        DropdownMenuItem(onClick = {
                            onUpdateStatus("Cancelado")
                            showStatusMenu = false
                        }) {
                            Text("Cancelado")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // Comprador
            Text(
                text = "Comprador: ${order.compradorNombre}",
                fontSize = 14.sp,
                color = Color(0xFF00ACC1),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = order.compradorEmail,
                fontSize = 12.sp,
                color = Color(0xFF666666)
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
                    text = "Total a recibir",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                Text(
                    text = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                        .format(order.total),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}