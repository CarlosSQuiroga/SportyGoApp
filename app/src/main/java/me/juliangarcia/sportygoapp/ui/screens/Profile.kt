package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Definición de paleta de colores para la pantalla Perfil
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    // Valores de perfil inmutables (Stateless)
    nombreUsuario: String = "",
    subtituloUsuario: String = "Mi cuenta",
    // null = sin foto de perfil (cuenta nueva): se muestra un ícono genérico.
    fotoPerfilUri: String? = null,
    tabBottomActual: String = "Perfil",
    // Los dueños no tienen "Mis reservas" (es una opción de jugador)
    esDuenio: Boolean = false,
    // Callbacks para la futura navegación y acciones
    onBackClick: () -> Unit = {},
    // Abre el selector de archivos para elegir la foto de perfil
    onCambiarFotoClick: () -> Unit = {},
    onMisReservasClick: () -> Unit = {},
    onMisGrabacionesClick: () -> Unit = {},
    onMisEquiposClick: () -> Unit = {},
    onMetodosPagoClick: () -> Unit = {},
    onConfiguracionClick: () -> Unit = {},
    onCerrarSesionClick: () -> Unit = {},
    onBottomNavTabClick: (String) -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Contenido principal scrolleable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Barra superior con botón volver y título
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Perfil",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Encabezado del Perfil del Usuario
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    // Foto de perfil circular: al tocarla se elige una imagen de los archivos
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clickable { onCambiarFotoClick() }
                    ) {
                        if (fotoPerfilUri == null) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(DarkCardBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Sin foto de perfil",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        } else {
                            AsyncImage(
                                model = fotoPerfilUri,
                                contentDescription = nombreUsuario,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Insignia de cámara para indicar que la foto se puede cambiar
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = "Cambiar foto de perfil",
                                tint = DarkBackground,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column {
                        Text(
                            text = nombreUsuario,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = subtituloUsuario,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Menú de opciones de Perfil
                if (!esDuenio) {
                    PerfilOpcionItem(
                        titulo = "Mis reservas",
                        icon = Icons.Outlined.CalendarToday,
                        onClick = onMisReservasClick
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                PerfilOpcionItem(
                    titulo = "Mis grabaciones",
                    icon = Icons.Outlined.Videocam,
                    onClick = onMisGrabacionesClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                PerfilOpcionItem(
                    titulo = "Mis equipos",
                    icon = Icons.Outlined.Group,
                    onClick = onMisEquiposClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                PerfilOpcionItem(
                    titulo = "Métodos de pago",
                    icon = Icons.Outlined.CreditCard,
                    onClick = onMetodosPagoClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                PerfilOpcionItem(
                    titulo = "Configuración",
                    icon = Icons.Outlined.Settings,
                    onClick = onConfiguracionClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                PerfilOpcionItem(
                    titulo = "Cerrar sesión",
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    onClick = onCerrarSesionClick
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Barra de Navegación Inferior (Bottom Navigation)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBackground)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    titulo = "Inicio",
                    icon = Icons.Outlined.Home,
                    isSelected = tabBottomActual == "Inicio",
                    onClick = { onBottomNavTabClick("Inicio") }
                )

                BottomNavItem(
                    titulo = "Grabaciones",
                    icon = Icons.Outlined.VideoLibrary,
                    isSelected = tabBottomActual == "Grabaciones",
                    onClick = { onBottomNavTabClick("Grabaciones") }
                )

                BottomNavItem(
                    titulo = "Equipos",
                    icon = Icons.Outlined.EmojiEvents,
                    isSelected = tabBottomActual == "Equipos",
                    onClick = { onBottomNavTabClick("Equipos") }
                )

                BottomNavItem(
                    titulo = "Perfil",
                    icon = Icons.Outlined.Person,
                    isSelected = tabBottomActual == "Perfil",
                    onClick = { onBottomNavTabClick("Perfil") }
                )
            }
        }
    }
}

// Composable para los elementos de lista del menú de perfil
@Composable
private fun PerfilOpcionItem(
    titulo: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = titulo,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = titulo,
            fontSize = 16.sp,
            color = TextWhite,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Composable para los elementos del Bottom Navigation
@Composable
private fun BottomNavItem(
    titulo: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = titulo,
            tint = if (isSelected) PrimaryGreen else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = titulo,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) PrimaryGreen else TextSecondary
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfilePreview() {
    ProfileScreen()
}
