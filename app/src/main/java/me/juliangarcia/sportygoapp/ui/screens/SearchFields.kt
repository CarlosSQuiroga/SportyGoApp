package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import me.juliangarcia.sportygoapp.data.Cancha

// Definición de colores para la pantalla de Búsqueda de Canchas
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val DarkPillBackground = Color(0xFF131E2B)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)
private val StarYellow = Color(0xFFFFC107)

@Composable
fun SearchFieldsScreen(
    modifier: Modifier = Modifier,
    // Estado/Valores inmutables pasados por parámetro
    ubicacionTexto: String = "Buscando tu ubicación...",
    busquedaTexto: String = "",
    // Canchas registradas por los dueños (opciones para reservar), las más cercanas primero
    canchas: List<Cancha> = emptyList(),
    // Id de la cancha -> distancia en metros desde el jugador (vacío = ubicación desconocida)
    distancias: Map<String, Double> = emptyMap(),
    tabBottomActual: String = "Inicio",
    // Mapa de la ciudad (se inyecta para que la pantalla siga siendo previsualizable)
    mapa: @Composable (Modifier) -> Unit = { m -> Box(m.background(DarkCardBackground)) },
    // Callbacks para la futura navegación y eventos
    onNotificacionesClick: () -> Unit = {},
    onUbicacionClick: () -> Unit = {},
    onBusquedaChange: (String) -> Unit = {},
    onMapTargetClick: () -> Unit = {},
    onCanchaClick: (Cancha) -> Unit = {},
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
                // Encabezado superior: Logo SportyGo y Campana de Notificaciones
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo SportyGo
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallSportyGoPin(modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = "SPORTY",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite
                        )
                        Text(
                            text = "GO",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryGreen
                        )
                    }

                    // Botón de Notificaciones
                    IconButton(onClick = onNotificacionesClick) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notificaciones",
                            tint = TextWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Campo de Ubicación
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardBackground)
                        .clickable { onUbicacionClick() }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = ubicacionTexto,
                        fontSize = 15.sp,
                        color = TextWhite,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Barra de Búsqueda
                OutlinedTextField(
                    value = busquedaTexto,
                    onValueChange = onBusquedaChange,
                    placeholder = {
                        Text(
                            text = "Buscar cancha o dirección...",
                            color = TextSecondary,
                            fontSize = 15.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Buscar",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkCardBackground,
                        unfocusedContainerColor = DarkCardBackground,
                        disabledContainerColor = DarkCardBackground,
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Encabezado del mapa: canchas disponibles cerca del jugador
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(DarkPillBackground)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(PrimaryGreen)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Canchas Disponibles Cerca",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sección del Mapa Interactivo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    mapa(Modifier.fillMaxSize())

                    // Botón flotante de Ubicación Actual en la esquina inferior derecha
                    IconButton(
                        onClick = onMapTargetClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkBackground)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MyLocation,
                            contentDescription = "Ubicación actual",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Título "Canchas disponibles" (registradas por los dueños)
                Text(
                    text = "Canchas disponibles",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (canchas.isEmpty()) {
                    Text(
                        text = if (busquedaTexto.isBlank()) {
                            "Aún no hay canchas registradas."
                        } else {
                            "No hay canchas que coincidan con \"$busquedaTexto\"."
                        },
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                canchas.forEach { cancha ->
                    CanchaCard(
                        cancha = cancha,
                        distanciaMetros = distancias[cancha.id],
                        onClick = { onCanchaClick(cancha) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))
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
                // Inicio
                BottomNavItem(
                    titulo = "Inicio",
                    icon = Icons.Outlined.Home,
                    isSelected = tabBottomActual == "Inicio",
                    onClick = { onBottomNavTabClick("Inicio") }
                )

                // Grabaciones
                BottomNavItem(
                    titulo = "Grabaciones",
                    icon = Icons.Outlined.VideoLibrary,
                    isSelected = tabBottomActual == "Grabaciones",
                    onClick = { onBottomNavTabClick("Grabaciones") }
                )

                // Equipos
                BottomNavItem(
                    titulo = "Equipos",
                    icon = Icons.Outlined.EmojiEvents,
                    isSelected = tabBottomActual == "Equipos",
                    onClick = { onBottomNavTabClick("Equipos") }
                )

                // Perfil
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

// Tarjeta de una cancha registrada
@Composable
private fun CanchaCard(cancha: Cancha, distanciaMetros: Double?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImagenCancha(
                imagenUri = cancha.imagenUri,
                contentDescription = cancha.nombre,
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cancha.nombre,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                if (cancha.descripcion.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = cancha.descripcion,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${formatearPrecio(cancha.precioHora)} / hora · ${cancha.horaApertura} – ${cancha.horaCierre}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryGreen
                )

                if (distanciaMetros != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "A ${formatearDistancia(distanciaMetros)} de ti",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

/** 850.0 -> "850 m"; 2340.0 -> "2,3 km" */
private fun formatearDistancia(metros: Double): String =
    if (metros < 1000) "${metros.toInt()} m"
    else String.format(java.util.Locale.forLanguageTag("es-CO"), "%.1f km", metros / 1000)

// Composable para cada elemento del Bottom Navigation
@Composable
private fun BottomNavItem(
    titulo: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

// Pin pequeño para el encabezado del logo
@Composable
private fun SmallSportyGoPin(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(26.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val pinPath = Path().apply {
                moveTo(w * 0.5f, h * 0.92f)
                cubicTo(w * 0.15f, h * 0.60f, w * 0.10f, h * 0.40f, w * 0.10f, h * 0.32f)
                cubicTo(w * 0.10f, h * 0.12f, w * 0.28f, 0f, w * 0.5f, 0f)
                cubicTo(w * 0.72f, 0f, w * 0.90f, h * 0.12f, w * 0.90f, h * 0.32f)
                cubicTo(w * 0.90f, h * 0.40f, w * 0.85f, h * 0.60f, w * 0.5f, h * 0.92f)
                close()
            }

            drawPath(path = pinPath, color = Color.White)

            // Mini cancha adentro
            drawRect(
                color = Color.Black,
                topLeft = Offset(w * 0.28f, h * 0.20f),
                size = Size(w * 0.44f, h * 0.28f),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SearchFieldsPreview() {
    SearchFieldsScreen()
}
