package com.example.tadeoshopapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
fun MarketplaceScreen(
    productViewModel: ProductViewModel = viewModel()
) {
    val allProducts by productViewModel.allProducts.collectAsState()
    val productState by productViewModel.productState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Cargar productos cuando se muestra la pantalla
    LaunchedEffect(Unit) {
        productViewModel.loadAllProducts()
    }

    // Filtrar productos por búsqueda
    val filteredProducts = if (searchQuery.isBlank()) {
        allProducts
    } else {
        allProducts.filter {
            it.titulo.contains(searchQuery, ignoreCase = true) ||
                    it.descripcion.contains(searchQuery, ignoreCase = true) ||
                    it.categoria.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                // Header
                TopAppBar(
                    title = {
                        Text(
                            text = "TadeoShop",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                    },
                    backgroundColor = Color.White,
                    elevation = 0.dp
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
                            text = "Buscar productos...",
                            color = Color(0xFF999999)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = Color(0xFF666666)
                        )
                    },
                    shape = RoundedCornerShape(25.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF00ACC1),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        backgroundColor = Color(0xFFF5F5F5),
                        cursorColor = Color(0xFF00ACC1)
                    ),
                    singleLine = true
                )

                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
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
                productState is ProductState.Loading && allProducts.isEmpty() -> {
                    // Mostrar carga inicial
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00ACC1))
                    }
                }
                filteredProducts.isEmpty() -> {
                    // Mostrar mensaje de vacío
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "No hay productos disponibles"
                            } else {
                                "No se encontraron productos"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF666666)
                        )
                        if (searchQuery.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Intenta con otra búsqueda",
                                fontSize = 14.sp,
                                color = Color(0xFF999999)
                            )
                        }
                    }
                }
                else -> {
                    // Mostrar grid de productos
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredProducts) { product ->
                            MarketplaceProductCard(product = product)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarketplaceProductCard(product: Product) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clickable {
                //  Navegar a detalles del producto
            },
        shape = RoundedCornerShape(12.dp),
        elevation = 3.dp,
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Imagen del producto
            if (product.imagenesUrls.isNotEmpty()) {
                AsyncImage(
                    model = product.imagenesUrls.first(),
                    contentDescription = product.titulo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
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

            // Información del producto
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Título
                Text(
                    text = product.titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(36.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Precio
                Text(
                    text = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                        .format(product.precio),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00ACC1)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Vendedor
                Text(
                    text = "Por: ${product.vendedorNombre}",
                    fontSize = 11.sp,
                    color = Color(0xFF999999),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}