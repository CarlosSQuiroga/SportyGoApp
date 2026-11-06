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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Definición de paleta de colores para la pantalla
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF131E2B)
private val DarkIconContainer = Color(0xFF182433)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)
private val TextTime = Color(0xFF6E7A8A)

// Tonalidades para los iconos de cada tipo de notificación
private val IconYellow = Color(0xFFE5B83B)
private val IconOrange = Color(0xFFE07048)
private val IconGreen = Color(0xFF64F089)
private val IconBlue = Color(0xFF63A0E8)
private val IconPurple = Color(0xFFA78BFA)
private val IconGrey = Color(0xFF8C9BAE)

// Modelo de datos inmutable para las notificaciones
data class NotificacionItem(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val tiempo: String,
    val icon: ImageVector,
    val iconTint: Color,
    val leida: Boolean = true
)

@Composable
fun NotificationsScreen(
    modifier: Modifier = Modifier,
    tabSeleccionada: String = "Todas",
    // Una cuenta nueva no tiene notificaciones.
    notificaciones: List<NotificacionItem> = emptyList(),
    // Callbacks para la futura navegación y eventos
    onBackClick: () -> Unit = {},
    onTabSelected: (String) -> Unit = {},
    onNotificacionClick: (NotificacionItem) -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Barra superior con botón de navegación y título
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
                    text = "Notificaciones",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de filtro (Pills: Todas / No leídas)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(DarkCardBackground)
                    .padding(4.dp)
            ) {
                // Pestaña "Todas"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (tabSeleccionada == "Todas") PrimaryGreen else Color.Transparent)
                        .clickable { onTabSelected("Todas") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Todas",
                        fontSize = 14.sp,
                        fontWeight = if (tabSeleccionada == "Todas") FontWeight.Bold else FontWeight.Medium,
                        color = if (tabSeleccionada == "Todas") DarkBackground else TextSecondary
                    )
                }

                // Pestaña "No leídas"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (tabSeleccionada == "No leídas") PrimaryGreen else Color.Transparent)
                        .clickable { onTabSelected("No leídas") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No leídas",
                        fontSize = 14.sp,
                        fontWeight = if (tabSeleccionada == "No leídas") FontWeight.Bold else FontWeight.Medium,
                        color = if (tabSeleccionada == "No leídas") DarkBackground else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (notificaciones.isEmpty()) {
                EstadoVacio(
                    icono = Icons.Outlined.Notifications,
                    titulo = "No tienes notificaciones",
                    descripcion = "Aquí verás avisos de tus reservas, equipos y grabaciones."
                )
            }

            // Lista de notificaciones
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = notificaciones,
                    key = { _, item -> item.id }
                ) { index, item ->
                    NotificacionRowItem(
                        item = item,
                        onClick = { onNotificacionClick(item) }
                    )

                    // Línea divisoria entre elementos
                    if (index < notificaciones.size - 1) {
                        HorizontalDivider(
                            color = DarkCardBackground,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificacionRowItem(
    item: NotificacionItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Contenedor del icono con fondo oscuro
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkIconContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.titulo,
                tint = item.iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Contenido de la notificación (Título, Descripción y Tiempo)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.titulo,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.descripcion,
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.tiempo,
                fontSize = 12.sp,
                color = TextTime
            )
        }

        // Punto verde = notificación sin leer
        if (!item.leida) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(PrimaryGreen)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NotificationsPreview() {
    NotificationsScreen()
}
