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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Definición de paleta de colores para la interfaz de Equipos
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val DarkPillBackground = Color(0xFF131E2B)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)

// Colores de acento para los escudos de equipos
private val ShieldGreen = Color(0xFF64F089)
private val ShieldTeal = Color(0xFF51D88A)
private val ShieldAmber = Color(0xFFE58A2B)
private val ShieldOlive = Color(0xFF7DAE60)

// Modelo de datos inmutable para representar un Equipo
data class EquipoItem(
    val id: String,
    val nombre: String,
    val jugadores: String,
    val infoCategoria: String = "Fútbol · Ver estadísticas",
    val inicial: String,
    val colorEscudo: Color
)

@Composable
fun TeamsScreen(
    modifier: Modifier = Modifier,
    tabSeleccionada: String = "Mis equipos",
    // Una cuenta nueva no tiene equipos registrados.
    equipos: List<EquipoItem> = emptyList(),
    // Callbacks para la futura navegación y eventos
    onBackClick: () -> Unit = {},
    onTabSelected: (String) -> Unit = {},
    onEquipoClick: (EquipoItem) -> Unit = {},
    onCrearEquipoClick: () -> Unit = {}
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
                    text = "Equipos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de pestañas (Mis equipos / Descubrir)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(DarkPillBackground)
                    .padding(4.dp)
            ) {
                // Pestaña "Mis equipos"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (tabSeleccionada == "Mis equipos") PrimaryGreen else Color.Transparent)
                        .clickable { onTabSelected("Mis equipos") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mis equipos",
                        fontSize = 14.sp,
                        fontWeight = if (tabSeleccionada == "Mis equipos") FontWeight.Bold else FontWeight.Medium,
                        color = if (tabSeleccionada == "Mis equipos") DarkBackground else TextSecondary
                    )
                }

                // Pestaña "Descubrir"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (tabSeleccionada == "Descubrir") PrimaryGreen else Color.Transparent)
                        .clickable { onTabSelected("Descubrir") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Descubrir",
                        fontSize = 14.sp,
                        fontWeight = if (tabSeleccionada == "Descubrir") FontWeight.Bold else FontWeight.Medium,
                        color = if (tabSeleccionada == "Descubrir") DarkBackground else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (equipos.isEmpty()) {
                EstadoVacio(
                    icono = Icons.Outlined.Groups,
                    titulo = if (tabSeleccionada == "Mis equipos") {
                        "Aún no perteneces a ningún equipo"
                    } else {
                        "No hay equipos para descubrir"
                    },
                    descripcion = "Crea un equipo o únete a uno para verlo aquí."
                )
            }

            // Lista de tarjetas de equipos
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = equipos,
                    key = { item -> item.id }
                ) { equipo ->
                    EquipoCardItem(
                        equipo = equipo,
                        onClick = { onEquipoClick(equipo) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de acción principal "Crear equipo"
            Button(
                onClick = onCrearEquipoClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Crear equipo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// Composable para cada tarjeta de equipo
@Composable
private fun EquipoCardItem(
    equipo: EquipoItem,
    onClick: () -> Unit
) {
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Escudo personalizado del equipo
            EscudoEquipo(
                inicial = equipo.inicial,
                accentColor = equipo.colorEscudo
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Información del equipo
            Column {
                Text(
                    text = equipo.nombre,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = equipo.jugadores,
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = equipo.infoCategoria,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryGreen
                )
            }
        }
    }
}

// Composable que dibuja el escudo del equipo usando Canvas
@Composable
private fun EscudoEquipo(
    inicial: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(52.dp, 62.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Trayectoria del escudo deportivo
            val shieldPath = Path().apply {
                moveTo(width * 0.1f, height * 0.1f)
                lineTo(width * 0.9f, height * 0.1f)
                lineTo(width * 0.9f, height * 0.65f)
                cubicTo(
                    width * 0.9f, height * 0.85f,
                    width * 0.5f, height * 0.98f,
                    width * 0.5f, height * 0.98f
                )
                cubicTo(
                    width * 0.5f, height * 0.98f,
                    width * 0.1f, height * 0.85f,
                    width * 0.1f, height * 0.65f
                )
                close()
            }

            // Fondo oscuro interno del escudo
            drawPath(
                path = shieldPath,
                color = Color(0xFF13222A)
            )

            // Rayas / Franjas verticales del escudo
            clipPath(shieldPath) {
                val stripeWidth = width * 0.22f
                drawRect(
                    color = accentColor.copy(alpha = 0.25f),
                    topLeft = Offset(width * 0.28f, 0f),
                    size = Size(stripeWidth, height)
                )
                drawRect(
                    color = accentColor.copy(alpha = 0.25f),
                    topLeft = Offset(width * 0.72f, 0f),
                    size = Size(stripeWidth, height)
                )
            }

            // Borde exterior del escudo
            drawPath(
                path = shieldPath,
                color = accentColor,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        Text(
            text = inicial,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TeamsPreview() {
    TeamsScreen()
}
