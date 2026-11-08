package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Schedule
import me.juliangarcia.sportygoapp.data.EstadoReserva
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.juliangarcia.sportygoapp.data.Reserva

// Paleta de colores
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val PendingYellow = Color(0xFFE5B83B)
private val RejectedRed = Color(0xFFFF6B6B)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)

/** "Mis reservas": reservas del usuario en sesión (vacía en una cuenta nueva). */
@Composable
fun MyReserveScreen(
    modifier: Modifier = Modifier,
    reservas: List<Reserva> = emptyList(),
    /** Id de la cancha -> ubicación de su kiosco (para saber dónde reclamar los productos). */
    kioscos: Map<String, String> = emptyMap(),
    /** Abrir Nequi para aceptar el pago de una reserva pendiente. */
    onPagarClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onReservaClick: (Reserva) -> Unit = {},
    onBuscarCanchasClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    text = "Mis reservas",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (reservas.isEmpty()) {
                    item {
                        EstadoVacio(
                            icono = Icons.Outlined.CalendarToday,
                            titulo = "No tienes reservas",
                            descripcion = "Busca una cancha y reserva tu próximo partido."
                        )
                    }
                }

                items(items = reservas, key = { it.id }) { reserva ->
                    ReservaCard(
                        reserva = reserva,
                        kiosco = kioscos[reserva.canchaId].orEmpty(),
                        onClick = { onReservaClick(reserva) },
                        onPagarClick = onPagarClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onBuscarCanchasClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Buscar canchas",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }
        }
    }
}

@Composable
private fun ReservaCard(
    reserva: Reserva,
    kiosco: String,
    onClick: () -> Unit,
    onPagarClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        ReservaResumen(reserva)
        if (reserva.productos.isNotEmpty() && reserva.estado != EstadoReserva.RECHAZADA) {
            PedidoKiosco(reserva = reserva, kiosco = kiosco)
        }
        if (reserva.estado == EstadoReserva.PENDIENTE_PAGO) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Text(
                    text = "Acepta el cobro de ${formatearPrecio(reserva.total)} en tu Nequi " +
                        "para confirmar la reserva.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onPagarClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PendingYellow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Pagar en Nequi", fontWeight = FontWeight.Bold, color = DarkBackground)
                }
            }
        }
    }
}

/** Productos comprados con la reserva y dónde reclamarlos. */
@Composable
private fun PedidoKiosco(reserva: Reserva, kiosco: String) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        HorizontalDivider(color = DarkBackground, thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Storefront,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Reclama en el kiosco" + if (kiosco.isNotBlank()) ": $kiosco" else "",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        reserva.productos.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.cantidad}× ${item.nombre}",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = formatearPrecio(item.subtotal),
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (reserva.estado == EstadoReserva.CONFIRMADA) "Total pagado" else "Total a pagar",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Text(
                text = formatearPrecio(reserva.total),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        }
    }
}

@Composable
private fun ReservaResumen(reserva: Reserva) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reserva.nombreCancha,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${reserva.fecha} · ${reserva.rangoHoras}",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                if (reserva.nombreEquipo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Equipo: ${reserva.nombreEquipo}",
                        fontSize = 14.sp,
                        color = PrimaryGreen
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (reserva.productos.isEmpty()) formatearPrecio(reserva.precio)
                    else "Cancha ${formatearPrecio(reserva.precio)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextWhite
                )
            }

            val (icono, texto, color) = when (reserva.estado) {
                EstadoReserva.CONFIRMADA -> Triple(Icons.Outlined.CheckCircle, "Confirmada", PrimaryGreen)
                EstadoReserva.PENDIENTE_PAGO -> Triple(Icons.Outlined.Schedule, "Pago pendiente", PendingYellow)
                EstadoReserva.RECHAZADA -> Triple(Icons.Outlined.Cancel, "Pago rechazado", RejectedRed)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = texto, fontSize = 12.sp, color = color)
            }
        }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MyReservePreview() {
    MyReserveScreen()
}
