package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Definición de colores principales
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)

// Colores para las acciones y escudos
private val IconPink = Color(0xFFE07098)
private val IconBlue = Color(0xFF63A0E8)

// Modelo de datos inmutable para eventos del partido
data class EventoPartidoItem(
    val id: String,
    val minuto: String,
    val descripcion: String
)

@Composable
fun LiveMatchScreen(
    modifier: Modifier = Modifier,
    // Valores pasados por parámetro (Stateless)
    equipoLocalInicial: String = "C",
    equipoVisitanteInicial: String = "B",
    equipoLocalColor: Color = PrimaryGreen,
    equipoVisitanteColor: Color = IconPink,
    marcadorTexto: String = "3 - 2",
    estadoPartidoTexto: String = "Finalizado · 90:00",
    videoImagenUrl: String = "https://images.unsplash.com/photo-1574629810360-7efbbe195018?q=80&w=800&auto=format&fit=crop",
    eventos: List<EventoPartidoItem> = getEventosPartidoPredeterminados(),
    // Callbacks para la futura navegación y eventos
    onBackClick: () -> Unit = {},
    onGolesClick: () -> Unit = {},
    onEstadisticasClick: () -> Unit = {},
    onGuardarClick: () -> Unit = {},
    onCompartirClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    text = "En vivo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Marcador y Escudos del Partido
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Escudo Equipo Local
                EscudoPartido(
                    inicial = equipoLocalInicial,
                    accentColor = equipoLocalColor
                )

                // Marcador y Tiempo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = marcadorTexto,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = estadoPartidoTexto,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                // Escudo Equipo Visitante
                EscudoPartido(
                    inicial = equipoVisitanteInicial,
                    accentColor = equipoVisitanteColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reproductor de Transmisión de la Cancha usando Coil
            AsyncImage(
                model = videoImagenUrl,
                contentDescription = "Transmisión en vivo del partido",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Barra de Acciones del Partido (Goles, Estadísticas, Guardar, Compartir)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccionPartidoItem(
                    titulo = "Goles",
                    icon = Icons.Outlined.SportsSoccer,
                    colorIcono = PrimaryGreen,
                    onClick = onGolesClick
                )

                AccionPartidoItem(
                    titulo = "Estadísticas",
                    icon = Icons.AutoMirrored.Outlined.Assignment,
                    colorIcono = PrimaryGreen,
                    onClick = onEstadisticasClick
                )

                AccionPartidoItem(
                    titulo = "Guardar",
                    icon = Icons.Outlined.BookmarkBorder,
                    colorIcono = IconPink,
                    onClick = onGuardarClick
                )

                AccionPartidoItem(
                    titulo = "Compartir",
                    icon = Icons.Outlined.Share,
                    colorIcono = IconBlue,
                    onClick = onCompartirClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = DarkCardBackground, thickness = 1.dp)

            Spacer(modifier = Modifier.height(20.dp))

            // Título "Eventos del partido"
            Text(
                text = "Eventos del partido",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cronología / Lista de Eventos
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                eventos.forEach { evento ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = evento.minuto,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            modifier = Modifier.width(44.dp),
                            textAlign = TextAlign.Start
                        )

                        Text(
                            text = evento.descripcion,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Composable para cada botón de acción debajo del video
@Composable
private fun AccionPartidoItem(
    titulo: String,
    icon: ImageVector,
    colorIcono: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = titulo,
            tint = colorIcono,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = titulo,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

// Composable que dibuja el escudo del equipo para el marcador
@Composable
private fun EscudoPartido(
    inicial: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(46.dp, 56.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val shieldPath = Path().apply {
                moveTo(width * 0.1f, height * 0.1f)
                lineTo(width * 0.9f, height * 0.1f)
                lineTo(width * 0.9f, height * 0.65f)
                cubicTo(width * 0.9f, height * 0.85f, width * 0.5f, height * 0.98f, width * 0.5f, height * 0.98f)
                cubicTo(width * 0.5f, height * 0.98f, width * 0.1f, height * 0.85f, width * 0.1f, height * 0.65f)
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
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Text(
            text = inicial,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
    }
}

private fun getEventosPartidoPredeterminados(): List<EventoPartidoItem> {
    return listOf(
        EventoPartidoItem("1", "82'", "Gol · Juan Pérez"),
        EventoPartidoItem("2", "67'", "Tarjeta amarilla · Andrés"),
        EventoPartidoItem("3", "45'", "Fin del primer tiempo"),
        EventoPartidoItem("4", "23'", "Gol · Daniel Rojas")
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LiveMatchPreview() {
    LiveMatchScreen()
}
