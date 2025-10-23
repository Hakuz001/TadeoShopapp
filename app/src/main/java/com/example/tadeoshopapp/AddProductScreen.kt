package com.example.tadeoshopapp.ui.theme

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tadeoshopapp.AuthViewModel
import com.example.tadeoshopapp.ProductState
import com.example.tadeoshopapp.ProductViewModel
import com.example.tadeoshopapp.R

// Estructura de las categorías
data class Category(
    val name: String,
    val subcategories: List<String> = emptyList()
)


val CATEGORIES = listOf(
    Category("📱 Electrónica", listOf(
        "Celulares y Tablets",
        "Computadoras",
        "Accesorios Electrónicos",
        "Audio y Video",
        "Cámaras y Fotografía",
        "Videojuegos y Consolas"
    )),
    Category("👔 Moda", listOf(
        "Ropa de Hombre",
        "Ropa de Mujer",
        "Zapatos",
        "Accesorios de Moda",
        "Bolsos y Carteras",
        "Relojes y Joyería"
    )),
    Category("🏠 Hogar", listOf(
        "Muebles",
        "Decoración",
        "Cocina y Comedor",
        "Jardín y Terraza",
        "Herramientas",
        "Iluminación"
    )),
    Category("⚽ Deportes", listOf(
        "Ropa Deportiva",
        "Calzado Deportivo",
        "Equipamiento Deportivo",
        "Fitness y Gym",
        "Ciclismo",
        "Camping y Outdoor"
    )),
    Category("📚 Libros y Música", listOf(
        "Libros",
        "Instrumentos Musicales",
        "Vinilos y CDs",
        "Libros Digitales"
    )),
    Category("🎮 Juguetes y Bebés", listOf(
        "Juguetes",
        "Ropa de Bebé",
        "Artículos para Bebé",
        "Juegos de Mesa"
    )),
    Category("🚗 Vehículos", listOf(
        "Autos",
        "Motos",
        "Repuestos",
        "Accesorios para Vehículos"
    )),
    Category("🎨 Arte y Coleccionables", listOf(
        "Arte",
        "Antigüedades",
        "Coleccionables",
        "Manualidades"
    )),
    Category("🐕 Mascotas", listOf(
        "Alimentos para Mascotas",
        "Accesorios para Mascotas",
        "Juguetes para Mascotas"
    )),
    Category("✨ Otros", listOf(
        "Servicios",
        "Otros Productos"
    ))
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AddProductScreen(
    onBackClick: () -> Unit,
    onPublishSuccess: () -> Unit,
    authViewModel: AuthViewModel,
    productViewModel: ProductViewModel
) {
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedSubcategory by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf(false) }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val productState by productViewModel.productState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Launcher para seleccionar imágenes
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            imageUris = (imageUris + uris).take(5)
        }
    }

    // Manejar estados del ViewModel
    LaunchedEffect(productState) {
        when (productState) {
            is ProductState.Success -> {
                showSuccessDialog = true
                productViewModel.resetProductState()
            }
            is ProductState.Error -> {
                errorMessage = (productState as ProductState.Error).message
            }
            else -> {}
        }
    }

    // Diálogo de éxito
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            shape = RoundedCornerShape(20.dp),
            backgroundColor = Color.White,
            title = {
                Text(
                    text = "¡Producto publicado!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.product_published_success),
                    fontSize = 15.sp,
                    color = Color(0xFF666666)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onPublishSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF00ACC1)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.understood_button),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
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
                    Text(
                        text = stringResource(R.string.new_product_title),
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fotos del producto
            Text(
                text = stringResource(R.string.add_photos),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )

            if (imageUris.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = Color.White,
                    elevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = stringResource(R.string.add_photos),
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFF00ACC1)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Toca para agregar fotos",
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(imageUris) { uri ->
                        Box {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Foto del producto",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { imageUris = imageUris.filter { it != uri } },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.6f),
                                        RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    "Eliminar",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (imageUris.size < 5) {
                        item {
                            Card(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(12.dp),
                                backgroundColor = Color.White,
                                elevation = 2.dp
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        "Agregar más fotos",
                                        modifier = Modifier.size(32.dp),
                                        tint = Color(0xFF00ACC1)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Título
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = {
                    Text(stringResource(R.string.product_title_label))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF00ACC1),
                    focusedLabelColor = Color(0xFF00ACC1),
                    cursorColor = Color(0xFF00ACC1)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Descripción
            val wordCount = if (descripcion.isBlank()) 0 else descripcion.trim().split("\\s+".toRegex()).size
            val isDescriptionValid = wordCount <= 150

            OutlinedTextField(
                value = descripcion,
                onValueChange = {
                    val newWordCount = if (it.isBlank()) 0 else it.trim().split("\\s+".toRegex()).size
                    if (newWordCount <= 150) {
                        descripcion = it
                    }
                },
                label = {
                    Text(stringResource(R.string.description_label))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = if (isDescriptionValid) Color(0xFF00ACC1) else Color(0xFFE53935),
                    focusedLabelColor = if (isDescriptionValid) Color(0xFF00ACC1) else Color(0xFFE53935),
                    cursorColor = Color(0xFF00ACC1),
                    errorBorderColor = Color(0xFFE53935)
                ),
                shape = RoundedCornerShape(12.dp),
                isError = !isDescriptionValid
            )

            Text(
                text = "$wordCount / 150 palabras",
                fontSize = 12.sp,
                color = if (isDescriptionValid) Color(0xFF666666) else Color(0xFFE53935),
                modifier = Modifier.padding(start = 4.dp)
            )

            // Precio
            OutlinedTextField(
                value = precio,
                onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("^\\d+\\.?\\d{0,2}$"))) {
                        precio = it
                    }
                },
                label = {
                    Text(stringResource(R.string.price_label))
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = {
                    Text("$", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF00ACC1),
                    focusedLabelColor = Color(0xFF00ACC1),
                    cursorColor = Color(0xFF00ACC1)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Selector de categoría
            Text(
                text = stringResource(R.string.category_label),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )

            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = !expandedCategory }
            ) {
                OutlinedTextField(
                    value = if (selectedSubcategory.isNotEmpty()) {
                        "$selectedCategory > $selectedSubcategory"
                    } else if (selectedCategory.isNotEmpty()) {
                        selectedCategory
                    } else {
                        ""
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text(stringResource(R.string.select_category))
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = if (expandedCategory) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expandir"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF00ACC1),
                        focusedLabelColor = Color(0xFF00ACC1)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                DropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    CATEGORIES.forEach { category ->
                        // Categoría principal
                        DropdownMenuItem(
                            onClick = {
                                selectedCategory = category.name
                                selectedSubcategory = ""
                                expandedCategory = false
                            }
                        ) {
                            Text(
                                text = category.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF212121)
                            )
                        }

                        // Subcategorías
                        category.subcategories.forEach { subcategory ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedCategory = category.name
                                    selectedSubcategory = subcategory
                                    expandedCategory = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 16.dp)
                                ) {
                                    Text(
                                        text = "• $subcategory",
                                        fontSize = 14.sp,
                                        color = Color(0xFF666666)
                                    )
                                }
                            }
                        }

                        if (category != CATEGORIES.last()) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón de publicar
            Button(
                onClick = {
                    if (titulo.isNotBlank() &&
                        descripcion.isNotBlank() &&
                        precio.isNotBlank() &&
                        selectedCategory.isNotBlank() &&
                        imageUris.isNotEmpty() &&
                        isDescriptionValid
                    ) {
                        val categoria = if (selectedSubcategory.isNotEmpty()) {
                            "$selectedCategory > $selectedSubcategory"
                        } else {
                            selectedCategory
                        }

                        productViewModel.createProduct(
                            titulo = titulo,
                            descripcion = descripcion,
                            precio = precio.toDouble(),
                            categoria = categoria,
                            imageUris = imageUris,
                            onSuccess = {
                                // El diálogo se muestra automáticamente
                            },
                            onError = { error ->
                                errorMessage = error
                            }
                        )
                    } else {
                        errorMessage = "Por favor completa todos los campos correctamente"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = productState !is ProductState.Loading,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF00ACC1),
                    disabledBackgroundColor = Color(0xFFCCCCCC)
                ),
                shape = RoundedCornerShape(28.dp),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                if (productState is ProductState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Icon(
                        Icons.Default.Publish,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.publish_button),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}