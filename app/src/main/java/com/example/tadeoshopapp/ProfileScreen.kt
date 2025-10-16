package com.example.tadeoshopapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    onEditProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onViewProductsClick: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()

    // Mostrar loading si no hay usuario cargado
    if (currentUser == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF00ACC1)
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Card del perfil
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            elevation = 8.dp,
            backgroundColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título
                Text(
                    text = stringResource(R.string.profile_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Foto de perfil
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_dog_logo),
                        contentDescription = stringResource(R.string.logo_description),
                        modifier = Modifier.size(90.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Nombre del usuario
                currentUser?.let { user ->
                    Text(
                        text = "${user.nombres} ${user.apellidos}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Badge de tipo de usuario
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (user.tipoUsuario == "Vendedor") Color(0xFF4CAF50) else Color(0xFF00ACC1)
                    ) {
                        Text(
                            text = user.tipoUsuario,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Divider
                    Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email
                    InfoRow(
                        icon = Icons.Default.Email,
                        text = user.email
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Teléfono
                    InfoRow(
                        icon = Icons.Default.Phone,
                        text = user.telefono.ifEmpty { stringResource(R.string.no_phone) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fecha de registro
                    val dateFormat = SimpleDateFormat("dd / MM / yyyy", Locale.getDefault())
                    val date = Date(user.fechaRegistro)
                    InfoRow(
                        icon = Icons.Default.DateRange,
                        text = dateFormat.format(date)
                    )

                    // Solo mostrar productos publicados si es Vendedor
                    if (user.tipoUsuario == "Vendedor") {
                        Spacer(modifier = Modifier.height(12.dp))

                        InfoRow(
                            icon = Icons.Default.ShoppingBag,
                            text = stringResource(R.string.products_count, user.productosPublicados)
                        )
                    }

                    // Biografía (solo si tiene algo escrito)
                    if (user.biografia.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = stringResource(R.string.biography_label),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF212121)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = user.biografia,
                                fontSize = 14.sp,
                                color = Color(0xFF666666),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón "Ver mis productos" solo para vendedores
                currentUser?.let { user ->
                    if (user.tipoUsuario == "Vendedor") {
                        OutlinedButton(
                            onClick = onViewProductsClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF1E88E5)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.view_my_products),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Botón Editar perfil
                Button(
                    onClick = onEditProfileClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF00ACC1)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.edit_profile_button),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón Cerrar sesión
                OutlinedButton(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF00ACC1)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.logout_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF666666),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
    }
}