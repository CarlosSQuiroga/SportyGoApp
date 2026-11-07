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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import me.juliangarcia.sportygoapp.data.Equipo

// Definición de paleta de colores para la interfaz de Equipos
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val DarkPillBackground = Color(0xFF131E2B)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)

// Colores de acento para los escudos sin imagen (se elige uno según el nombre)
private val ColoresEscudo = listOf(
    Color(0xFF64F089),
    Color(0xFF51D88A),
    Color(0xFFE58A2B),
    Color(0xFF7DAE60),
    Color(0xFF63A0E8),
    Color(0xFFA78BFA)
)

@Composable
fun TeamsScreen(
    modifier: Modifier = Modifier,
    tabSeleccionada: String = "Mis equipos",
    /** Equipos de la pestaña actual (una cuenta nueva no tiene equipos). */
    equipos: List<Equipo> = emptyList(),
    /** Id del creador -> nombre de usuario (para "Descubrir"). */
    creadores: Map<String, String> = emptyMap(),
    onBackClick: () -> Unit = {},
    onTabSelected: (String) -> Unit = {},
    onEquipoClick: (Equipo) -> Unit = {},
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
                listOf("Mis equipos", "Descubrir").forEach { tab ->
                    val seleccionada = tabSeleccionada == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (seleccionada) PrimaryGreen else Color.Transparent)
                            .clickable { onTabSelected(tab) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontSize = 14.sp,
                            fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Medium,
                            color = if (seleccionada) DarkBackground else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (equipos.isEmpty()) {
                EstadoVacio(
                    icono = Icons.Outlined.Groups,
                    titulo = if (tabSeleccionada == "Mis equipos") {
                        "Aún no has creado ningún equipo"
                    } else {
                        "No hay equipos para descubrir"
                    },
                    descripcion = if (tabSeleccionada == "Mis equipos") {
                        "Crea un equipo para reservar a su nombre y registrarlo en tus partidos."
                    } else {
                        "Aquí verás los equipos que creen otros jugadores."
                    }
                )
            }

            // Lista de tarjetas de equipos
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = equipos, key = { it.id }) { equipo ->
                    EquipoCardItem(
                        equipo = equipo,
                        subtitulo = if (tabSeleccionada == "Mis equipos") {
                            "Tu equipo"
                        } else {
                            creadores[equipo.creadorId]?.let { "Creado por $it" } ?: "Equipo"
                        },
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
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
    equipo: Equipo,
    subtitulo: String,
    onClick: () -> Unit
) {
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EscudoDeEquipo(nombre = equipo.nombre, escudoUri = equipo.escudoUri)

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = equipo.nombre,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitulo,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Escudo de un equipo: la imagen elegida por el creador o, si no tiene, un escudo dibujado
 * con la inicial del nombre. [alto] define el tamaño (el ancho es proporcional).
 */
@Composable
fun EscudoDeEquipo(
    nombre: String,
    escudoUri: String,
    modifier: Modifier = Modifier,
    alto: Dp = 62.dp
) {
    val ancho = alto * (52f / 62f)
    if (escudoUri.isNotBlank()) {
        AsyncImage(
            model = escudoUri,
            contentDescription = "Escudo de $nombre",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(ancho, alto)
                .clip(RoundedCornerShape(alto * 0.18f))
                .background(DarkPillBackground)
        )
        return
    }

    val accentColor = ColoresEscudo[Math.floorMod(nombre.lowercase().hashCode(), ColoresEscudo.size)]
    Box(
        modifier = modifier.size(ancho, alto),
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

            drawPath(path = shieldPath, color = Color(0xFF13222A))

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

            drawPath(
                path = shieldPath,
                color = accentColor,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        Text(
            text = nombre.trim().take(1).uppercase().ifEmpty { "?" },
            fontSize = (alto.value * 0.32f).sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TeamsPreview() {
    TeamsScreen(
        equipos = listOf(
            Equipo(id = "1", nombre = "Los Halcones"),
            Equipo(id = "2", nombre = "Real Barrio")
        )
    )
}
