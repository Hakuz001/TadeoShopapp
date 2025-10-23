package com.example.tadeoshopapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
fun ProductsScreen(
    onBackClick: () -> Unit = {},
    onAddProductClick: () -> Unit = {},
    onEditProductClick: (Product) -> Unit = {},
    authViewModel: AuthViewModel = viewModel(),
    productViewModel: ProductViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val myProducts by productViewModel.myProducts.collectAsState()
    val productState by productViewModel.productState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    // Cargar productos cuando se muestra la pantalla
    LaunchedEffect(Unit) {
        productViewModel.loadMyProducts()
    }

    // Diálogo de confirmación para eliminar
    if (showDeleteDialog && productToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(20.dp),
            backgroundColor = Color.White,
            title = {
                Text(
                    text = stringResource(R.string.delete_product_confirm),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_product_message),
                    fontSize = 15.sp,
                    color = Color(0xFF666666)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        productToDelete?.let { product ->
                            productViewModel.deleteProduct(
                                productId = product.id,
                                onSuccess = {
                                    showDeleteDialog = false
                                    productToDelete = null
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFE53935)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.delete_button),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteDialog = false
                        productToDelete = null
                    },
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.products_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                },
                backgroundColor = Color.White,
                elevation = 4.dp
            )
        },
        floatingActionButton = {
            // Botón flotante para agregar producto
            FloatingActionButton(
                onClick = onAddProductClick,
                backgroundColor = Color(0xFF00ACC1),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_product_button)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(paddingValues)
        ) {
            when {
                productState is ProductState.Loading -> {
                    // Mostrar loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00ACC1))
                    }
                }
                myProducts.isEmpty() -> {
                    // Mostrar pantalla vacía
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_products),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.no_products_message),
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onAddProductClick,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF00ACC1)
                            ),
                            shape = RoundedCornerShape(25.dp),
                            modifier = Modifier
                                .padding(horizontal = 32.dp)
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.add_product_button),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                else -> {
                    // Mostrar lista de productos
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(myProducts) { product ->
                            ProductCard(
                                product = product,
                                onEditClick = {
                                    productViewModel.selectProduct(product)
                                    onEditProductClick(product)
                                },
                                onDeleteClick = {
                                    productToDelete = product
                                    showDeleteDialog = true
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
fun ProductCard(
    product: Product,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        backgroundColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Imagen del producto
            if (product.imagenesUrls.isNotEmpty()) {
                AsyncImage(
                    model = product.imagenesUrls.first(),
                    contentDescription = product.titulo,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin foto",
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información del producto
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = product.titulo,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                            .format(product.precio),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00ACC1)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Estado del producto
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (product.estado) {
                            "Publicado" -> Color(0xFF4CAF50)
                            "Borrador" -> Color(0xFFFFA726)
                            else -> Color(0xFF9E9E9E)
                        }
                    ) {
                        Text(
                            text = product.estado,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Botones de acción
                    Row {
                        // Botón editar
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_button),
                                tint = Color(0xFF00ACC1),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Botón eliminar
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_button),
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}