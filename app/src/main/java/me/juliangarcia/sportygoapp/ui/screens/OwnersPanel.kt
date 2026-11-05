package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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

// Definición de paleta de colores para el Panel del Dueño
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val DarkPillBackground = Color(0xFF131E2B)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)

// Modelo de datos inmutable para las reservas de hoy
data class ReservaHoyItem(
    val hora: String,
    val estado: String = "Confirmada"
)

@Composable
fun OwnersPanelScreen(
    modifier: Modifier = Modifier,
    // Valores de estado pasados por parámetro (Stateless)
    nombreCancha: String = "",
    reservasHoy: List<ReservaHoyItem> = emptyList(),
    ingresosMonto: String = "$0",
    ingresosCambio: String = "",
    reservasCount: String = "0",
    reservasHoySubtexto: String = "0 hoy",
    // Proporción (0..1) de ingresos de cada día de la semana actual, de lunes a domingo.
    ingresosSemana: List<Pair<String, Float>> = listOf("L", "M", "M", "J", "V", "S", "D").map { it to 0f },
    filtroIngresosTab: String = "Semana",
    tabBottomActual: String = "Inicio",
    // Callbacks para eventos e interacción futura
    onFiltroIngresosTabClick: (String) -> Unit = {},
    onInventarioClick: () -> Unit = {},
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
                // Título principal
                Text(
                    text = "Panel del dueño",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtítulo con indicador verde de estado de la cancha
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = nombreCancha,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryGreen
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tarjeta "Reservas de hoy"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkCardBackground
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Reservas de hoy",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (reservasHoy.isEmpty()) {
                            Text(
                                text = "No hay reservas para hoy.",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        reservasHoy.forEachIndexed { index, reserva ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = "Hora reserva",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = reserva.hora,
                                        fontSize = 15.sp,
                                        color = TextWhite
                                    )
                                }

                                Text(
                                    text = reserva.estado,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryGreen
                                )
                            }

                            if (index < reservasHoy.size - 1) {
                                HorizontalDivider(
                                    color = DarkPillBackground,
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fila de Métricas (Ingresos / Reservas)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tarjeta de Ingresos
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkCardBackground
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Ingresos",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = ingresosMonto,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = ingresosCambio,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                    }

                    // Tarjeta de Reservas
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkCardBackground
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Reservas",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = reservasCount,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = reservasHoySubtexto,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tarjeta "Ingresos por semana" (Gráfico de Barras)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkCardBackground
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Ingresos por semana",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Selector de pestañas del gráfico (Semana / Mes)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .background(DarkPillBackground)
                                .padding(4.dp)
                        ) {
                            // Pestaña "Semana"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (filtroIngresosTab == "Semana") PrimaryGreen else Color.Transparent)
                                    .clickable { onFiltroIngresosTabClick("Semana") }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Semana",
                                    fontSize = 14.sp,
                                    fontWeight = if (filtroIngresosTab == "Semana") FontWeight.Bold else FontWeight.Medium,
                                    color = if (filtroIngresosTab == "Semana") DarkBackground else TextSecondary
                                )
                            }

                            // Pestaña "Mes"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (filtroIngresosTab == "Mes") PrimaryGreen else Color.Transparent)
                                    .clickable { onFiltroIngresosTabClick("Mes") }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Mes",
                                    fontSize = 14.sp,
                                    fontWeight = if (filtroIngresosTab == "Mes") FontWeight.Bold else FontWeight.Medium,
                                    color = if (filtroIngresosTab == "Mes") DarkBackground else TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Gráfico de Barras de Ingresos
                        IngresosBarChart(
                            diasConValores = ingresosSemana
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Acceso al inventario de la tienda de la cancha
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInventarioClick() },
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
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "Inventario de la tienda",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
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

// Composable que dibuja el gráfico de barras verdes
@Composable
private fun IngresosBarChart(
    diasConValores: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        diasConValores.forEach { (dia, porcentaje) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .fillMaxHeight(porcentaje.coerceIn(0f, 1.0f))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(PrimaryGreen)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = dia,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

// Composable auxiliar para los elementos de la barra inferior
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
fun OwnersPanelPreview() {
    OwnersPanelScreen()
}
