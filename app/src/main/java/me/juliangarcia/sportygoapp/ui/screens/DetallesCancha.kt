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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import me.juliangarcia.sportygoapp.data.Cancha
import me.juliangarcia.sportygoapp.data.Equipo
import me.juliangarcia.sportygoapp.data.Producto
import me.juliangarcia.sportygoapp.data.Validaciones

// Definición de paleta de colores para la interfaz
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF131D2A)
private val DarkChipBackground = Color(0xFF182330)
private val SelectedChipBackground = Color(0xFF1B3B32)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)
private val TextDisabled = Color(0xFF4A5666)
private val ErrorRed = Color(0xFFFF6B6B)

@Composable
fun DetallesCanchaScreen(
    modifier: Modifier = Modifier,
    cancha: Cancha = Cancha(),
    /** Etiquetas de los días que se pueden reservar ("Hoy", "Mañana", "23"...). */
    dias: List<String> = emptyList(),
    diaSeleccionado: Int = 0,
    /** Horas de inicio disponibles según el horario de la cancha ("9:00", "10:00"...). */
    horas: List<String> = emptyList(),
    /** Horas ya reservadas el día seleccionado. */
    horasOcupadas: Set<String> = emptySet(),
    horaSeleccionada: String? = null,
    /** Horas seguidas a reservar desde [horaSeleccionada]. */
    duracionHoras: Int = 1,
    /** Máximo de horas seguidas libres desde [horaSeleccionada] (0 = sin hora elegida). */
    maxDuracionHoras: Int = 0,
    onDuracionChange: (Int) -> Unit = {},
    /** Mensajes del dueño sin leer en el chat de esta cancha. */
    mensajesSinLeer: Int = 0,
    onChatClick: () -> Unit = {},
    /** Productos del kiosco que se pueden comprar con la reserva. */
    productos: List<Producto> = emptyList(),
    /** Id del producto -> cantidad elegida. */
    cantidades: Map<String, Int> = emptyMap(),
    /** Equipos del jugador (puede reservar a nombre de uno, opcional). */
    equipos: List<Equipo> = emptyList(),
    equipoSeleccionadoId: String? = null,
    errorMensaje: String? = null,
    onEquipoSelected: (String?) -> Unit = {},
    onCrearEquipoClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onDiaSelected: (Int) -> Unit = {},
    onHoraSelected: (String) -> Unit = {},
    onCantidadChange: (productoId: String, cantidad: Int) -> Unit = { _, _ -> },
    onReservarClick: () -> Unit = {}
) {
    val totalProductos = productos.sumOf { (cantidades[it.id] ?: 0) * it.precio }
    val unidades = productos.sumOf { cantidades[it.id] ?: 0 }
    val duracion = duracionHoras.coerceAtLeast(1)
    val precioCancha = cancha.precioHora * duracion
    // Horas que quedan dentro de la reserva elegida (se resaltan todas).
    val horasElegidas = horaSeleccionada?.let { inicio ->
        val desde = horas.indexOf(inicio)
        if (desde < 0) emptySet() else horas.drop(desde).take(duracion).toSet()
    } ?: emptySet()
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Contenedor scrolleable para evitar recortes en pantallas pequeñas
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Imagen de la cancha y botón para volver
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    ImagenCancha(
                        imagenUri = cancha.imagenUri,
                        contentDescription = cancha.nombre,
                        modifier = Modifier.fillMaxSize()
                    )

                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = cancha.nombre,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                if (cancha.descripcion.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = cancha.descripcion,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                DatoCancha(Icons.Outlined.Groups, "Capacidad: ${cancha.capacidad} jugadores")
                Spacer(modifier = Modifier.height(6.dp))
                DatoCancha(Icons.Outlined.Schedule, "Abierto de ${cancha.horaApertura} a ${cancha.horaCierre}")
                if (cancha.direccion.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    DatoCancha(Icons.Outlined.LocationOn, cancha.direccion)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Chat con el dueño de la cancha
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardBackground)
                        .clickable { onChatClick() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Chat,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chatear con el dueño",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Pregunta por horarios, productos o lo que necesites",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    if (mensajesSinLeer > 0) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(PrimaryGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (mensajesSinLeer > 9) "9+" else mensajesSinLeer.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBackground
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(color = DarkCardBackground, thickness = 1.dp)

                Spacer(modifier = Modifier.height(16.dp))

                // Precio por hora
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Precio por hora",
                        fontSize = 16.sp,
                        color = TextWhite
                    )
                    Text(
                        text = formatearPrecio(cancha.precioHora),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = DarkCardBackground, thickness = 1.dp)

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Horarios disponibles",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Chips para Días
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dias.forEachIndexed { indice, texto ->
                        ChipDia(
                            texto = texto,
                            isSelected = indice == diaSeleccionado,
                            onClick = { onDiaSelected(indice) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (horas.isEmpty()) {
                    Text(
                        text = "Esta cancha no tiene horarios disponibles.",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                // Horas en filas de 4
                horas.chunked(4).forEach { fila ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fila.forEach { hora ->
                            val ocupada = hora in horasOcupadas
                            ChipHora(
                                hora = hora,
                                isSelected = hora in horasElegidas,
                                ocupada = ocupada,
                                onClick = { if (!ocupada) onHoraSelected(hora) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Relleno para que la última fila mantenga el mismo ancho de chips
                        repeat(4 - fila.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Cuántas horas seguidas reservar desde la hora elegida.
                if (horaSeleccionada != null && maxDuracionHoras > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCardBackground)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Duración",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "$horaSeleccionada – ${Validaciones.sumarHoras(horaSeleccionada, duracion)}" +
                                    if (maxDuracionHoras == 1) " · solo hay 1 hora libre seguida" else "",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        BotonDuracion(
                            texto = "−",
                            habilitado = duracion > 1,
                            onClick = { onDuracionChange(duracion - 1) }
                        )
                        Text(
                            text = if (duracion == 1) "1 hora" else "$duracion horas",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(76.dp)
                        )
                        BotonDuracion(
                            texto = "+",
                            habilitado = duracion < maxDuracionHoras,
                            onClick = { onDuracionChange(duracion + 1) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Equipo que hace la reserva (opcional).
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DarkCardBackground, thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "¿Reservas con tu equipo?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Opcional. El equipo quedará registrado en la reserva y en la grabación del partido.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (equipos.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Aún no tienes equipos.",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Crear equipo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCrearEquipoClick() }
                                .padding(4.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChipEquipo(
                            nombre = "Sin equipo",
                            escudoUri = null,
                            isSelected = equipoSeleccionadoId == null,
                            onClick = { onEquipoSelected(null) }
                        )
                        equipos.forEach { equipo ->
                            ChipEquipo(
                                nombre = equipo.nombre,
                                escudoUri = equipo.escudoUri,
                                isSelected = equipo.id == equipoSeleccionadoId,
                                onClick = { onEquipoSelected(equipo.id) }
                            )
                        }
                    }
                }

                // Productos del kiosco (opcional): se pagan con la reserva y se reclaman en la cancha.
                if (productos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = DarkCardBackground, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Agrega productos del kiosco",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (cancha.kiosco.isBlank()) "Reclámalos en el kiosco de la cancha"
                            else "Reclámalos en el kiosco: ${cancha.kiosco}",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    productos.forEach { producto ->
                        FilaProducto(
                            producto = producto,
                            cantidad = cantidades[producto.id] ?: 0,
                            onCantidadChange = { onCantidadChange(producto.id, it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Resumen de lo que se va a pagar
                if (horaSeleccionada != null || unidades > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCardBackground)
                            .padding(16.dp)
                    ) {
                        equipos.firstOrNull { it.id == equipoSeleccionadoId }?.let { equipo ->
                            LineaResumen("Reserva a nombre de", equipo.nombre)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        LineaResumen(
                            if (duracion == 1) "Cancha (1 hora)" else "Cancha ($duracion horas)",
                            formatearPrecio(precioCancha)
                        )
                        if (unidades > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LineaResumen(
                                if (unidades == 1) "1 producto" else "$unidades productos",
                                formatearPrecio(totalProductos)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = DarkChipBackground, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        LineaResumen("Total", formatearPrecio(precioCancha + totalProductos), destacado = true)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Fuera del scroll, junto al botón, para que siempre se vea al tocar "Reservar".
            if (errorMensaje != null) {
                Text(
                    text = errorMensaje,
                    fontSize = 14.sp,
                    color = ErrorRed,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = onReservarClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (horaSeleccionada != null || unidades > 0) {
                        "Reservar y pagar ${formatearPrecio(precioCancha + totalProductos)}"
                    } else {
                        "Reservar ahora"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }
        }
    }
}

@Composable
private fun BotonDuracion(texto: String, habilitado: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (habilitado) SelectedChipBackground else DarkChipBackground)
            .clickable(enabled = habilitado) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (habilitado) PrimaryGreen else TextDisabled
        )
    }
}

/** Chip para elegir el equipo de la reserva ([escudoUri] null = opción "Sin equipo"). */
@Composable
private fun ChipEquipo(
    nombre: String,
    escudoUri: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SelectedChipBackground else DarkChipBackground)
            .border(
                width = 1.dp,
                color = if (isSelected) PrimaryGreen else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (escudoUri != null) {
            EscudoDeEquipo(nombre = nombre, escudoUri = escudoUri, alto = 28.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = nombre,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) PrimaryGreen else TextWhite
        )
    }
}

@Composable
private fun FilaProducto(producto: Producto, cantidad: Int, onCantidadChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (cantidad > 0) SelectedChipBackground else DarkCardBackground)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImagenProducto(
            producto = producto,
            modifier = Modifier
                .size(48.dp)
                .clip(FormaProducto),
            tamanoIcono = 24.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = producto.nombre,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                text = formatearPrecio(producto.precio),
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
        SelectorCantidad(cantidad = cantidad, onCambiar = onCantidadChange)
    }
}

@Composable
private fun LineaResumen(texto: String, valor: String, destacado: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = texto,
            fontSize = if (destacado) 16.sp else 14.sp,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal,
            color = if (destacado) TextWhite else TextSecondary
        )
        Text(
            text = valor,
            fontSize = if (destacado) 16.sp else 14.sp,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Medium,
            color = if (destacado) PrimaryGreen else TextWhite
        )
    }
}

@Composable
private fun DatoCancha(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = PrimaryGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = texto, fontSize = 14.sp, color = TextWhite)
    }
}

@Composable
private fun ChipDia(
    texto: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) SelectedChipBackground else DarkChipBackground)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) PrimaryGreen else TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChipHora(
    hora: String,
    isSelected: Boolean,
    ocupada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) SelectedChipBackground else DarkChipBackground)
            .clickable(enabled = !ocupada) { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = hora,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                ocupada -> TextDisabled
                isSelected -> PrimaryGreen
                else -> TextWhite
            },
            textDecoration = if (ocupada) TextDecoration.LineThrough else null,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DetallesCanchaPreview() {
    DetallesCanchaScreen(
        cancha = Cancha(
            nombre = "Cancha de ejemplo",
            descripcion = "Fútbol 5 · Sintética",
            capacidad = 10,
            horaApertura = "8:00",
            horaCierre = "22:00",
            precioHora = 80000
        ),
        dias = listOf("Hoy", "Mañana", "23", "24"),
        horas = listOf("8:00", "9:00", "10:00", "11:00", "12:00")
    )
}
