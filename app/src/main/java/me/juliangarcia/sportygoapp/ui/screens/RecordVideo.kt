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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import me.juliangarcia.sportygoapp.data.Grabacion

// Definición de paleta de colores para la pantalla Grabaciones
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val DarkPillBackground = Color(0xFF131E2B)
private val DarkOverlayCircle = Color(0x990B131D)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)

@Composable
fun RecordVideoScreen(
    modifier: Modifier = Modifier,
    // Valores de estado pasados por parámetro (Stateless)
    tabSeleccionada: String = "Mis partidos",
    // Grabaciones del usuario (una cuenta nueva no tiene ninguna)
    grabaciones: List<Grabacion> = emptyList(),
    notaFooter: String = "Revive tus mejores jugadas y comparte los momentos destacados de tus partidos.",
    tabBottomActual: String = "Grabaciones",
    // Callbacks para la futura navegación y eventos
    onBackClick: () -> Unit = {},
    onTabSelected: (String) -> Unit = {},
    onGrabacionClick: (Grabacion) -> Unit = {},
    // Menú de cada video: compartir completo, compartir un momento clave, renombrar, eliminar
    onCompartirClick: (Grabacion) -> Unit = {},
    onMomentoClaveClick: (Grabacion) -> Unit = {},
    onRenombrarClick: (Grabacion) -> Unit = {},
    onEliminarClick: (Grabacion) -> Unit = {},
    onBottomNavTabClick: (String) -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Contenido principal de la lista
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
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
                        text = "Grabaciones",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selector de pestañas (Mis partidos / Todos)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(DarkPillBackground)
                        .padding(4.dp)
                ) {
                    // Pestaña "Mis partidos"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (tabSeleccionada == "Mis partidos") PrimaryGreen else Color.Transparent)
                            .clickable { onTabSelected("Mis partidos") }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Mis partidos",
                            fontSize = 14.sp,
                            fontWeight = if (tabSeleccionada == "Mis partidos") FontWeight.Bold else FontWeight.Medium,
                            color = if (tabSeleccionada == "Mis partidos") DarkBackground else TextSecondary
                        )
                    }

                    // Pestaña "Todos"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (tabSeleccionada == "Todos") PrimaryGreen else Color.Transparent)
                            .clickable { onTabSelected("Todos") }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Todos",
                            fontSize = 14.sp,
                            fontWeight = if (tabSeleccionada == "Todos") FontWeight.Bold else FontWeight.Medium,
                            color = if (tabSeleccionada == "Todos") DarkBackground else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lista de Grabaciones
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (grabaciones.isEmpty()) {
                        item {
                            EstadoVacio(
                                icono = Icons.Outlined.VideoLibrary,
                                titulo = "Aún no tienes grabaciones",
                                descripcion = "Las grabaciones de tus partidos aparecerán aquí."
                            )
                        }
                    }

                    items(
                        items = grabaciones,
                        key = { item -> item.id }
                    ) { grabacion ->
                        GrabacionCardItem(
                            grabacion = grabacion,
                            onClick = { onGrabacionClick(grabacion) },
                            onCompartirClick = { onCompartirClick(grabacion) },
                            onMomentoClaveClick = { onMomentoClaveClick(grabacion) },
                            onRenombrarClick = { onRenombrarClick(grabacion) },
                            onEliminarClick = { onEliminarClick(grabacion) }
                        )
                    }

                    // Nota motivacional / Footer al final de la lista
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = notaFooter,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        )
                    }
                }
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

// Composable para cada tarjeta de grabación
@Composable
private fun GrabacionCardItem(
    grabacion: Grabacion,
    onClick: () -> Unit,
    onCompartirClick: () -> Unit,
    onMomentoClaveClick: () -> Unit,
    onRenombrarClick: () -> Unit,
    onEliminarClick: () -> Unit
) {
    val titulo = grabacion.titulo
    var menuAbierto by remember { mutableStateOf(false) }
    val fechaHora = listOf(grabacion.fechaGuardado, grabacion.horaReserva)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = DarkCardBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniatura del Video con Icono Play superpuesto
            Box(
                modifier = Modifier
                    .size(105.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkPillBackground),
                contentAlignment = Alignment.Center
            ) {

                // Botón Play superpuesto en el centro
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkOverlayCircle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información del partido grabado
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = titulo,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                // Si el usuario lo renombró, se sigue viendo en qué cancha fue.
                if (grabacion.nombre.isNotBlank() && grabacion.nombreCancha.isNotBlank()) {
                    Text(text = grabacion.nombreCancha, fontSize = 13.sp, color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(4.dp))

                grabacion.enfrentamiento?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryGreen,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = fechaHora,
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Enlace / Acción "Ver grabación"
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayCircleOutline,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Ver grabación",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextWhite
                    )
                }
            }

            // Menú de opciones del video
            Box {
                IconButton(onClick = { menuAbierto = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Opciones del video",
                        tint = TextSecondary
                    )
                }
                DropdownMenu(
                    expanded = menuAbierto,
                    onDismissRequest = { menuAbierto = false },
                    containerColor = DarkPillBackground
                ) {
                    OpcionMenu("Compartir partido completo", Icons.Outlined.Share) {
                        menuAbierto = false
                        onCompartirClick()
                    }
                    OpcionMenu("Compartir momento clave", Icons.Outlined.ContentCut) {
                        menuAbierto = false
                        onMomentoClaveClick()
                    }
                    OpcionMenu("Renombrar", Icons.Outlined.Edit) {
                        menuAbierto = false
                        onRenombrarClick()
                    }
                    OpcionMenu("Eliminar", Icons.Outlined.Delete, color = Color(0xFFFF6B6B)) {
                        menuAbierto = false
                        onEliminarClick()
                    }
                }
            }
        }
    }
}

@Composable
private fun OpcionMenu(
    texto: String,
    icono: ImageVector,
    color: Color = TextWhite,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(texto, color = color, fontSize = 14.sp) },
        leadingIcon = { Icon(icono, contentDescription = null, tint = color) },
        onClick = onClick
    )
}

// Composable auxiliar para la barra de navegación inferior
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
fun RecordVideoPreview() {
    RecordVideoScreen()
}
