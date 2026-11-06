package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.juliangarcia.sportygoapp.pagos.EstadoPago
import me.juliangarcia.sportygoapp.pagos.SolicitudPago
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val DarkPillBackground = Color(0xFF182330)
private val PrimaryGreen = Color(0xFF64F089)
private val WarningYellow = Color(0xFFE5B83B)
private val ErrorRed = Color(0xFFFF6B6B)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)

/**
 * "Métodos de pago": registrar el número Nequi.
 * Jugador: desde ese número paga sus reservas. Dueño: ahí recibe los pagos de su cancha.
 */
@Composable
fun MetodoPagoScreen(
    esDuenio: Boolean,
    /** Número guardado (null = sin registrar). */
    nequiRegistrado: String?,
    /** Lo que el usuario está escribiendo. */
    numero: String,
    errorMensaje: String? = null,
    mensajeExito: String? = null,
    onBackClick: () -> Unit = {},
    onNumeroChange: (String) -> Unit = {},
    onGuardarClick: () -> Unit = {},
    onQuitarClick: () -> Unit = {},
    onVerSolicitudesClick: () -> Unit = {}
) {
    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            BarraTitulo("Métodos de pago", onBackClick)
            Spacer(modifier = Modifier.height(16.dp))

            // Estado actual
            val registrado = nequiRegistrado != null
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DarkPillBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = if (registrado) PrimaryGreen else TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Nequi", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text(
                            text = nequiRegistrado?.let { formatoCelular(it) } ?: "Sin número registrado",
                            fontSize = 14.sp,
                            color = if (registrado) TextWhite else TextSecondary
                        )
                    }
                    if (registrado) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Registrado",
                            tint = PrimaryGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Qué significa para cada rol
            Aviso(
                icono = if (esDuenio && !registrado) Icons.Outlined.ErrorOutline else Icons.Outlined.Info,
                color = if (esDuenio && !registrado) WarningYellow else TextSecondary,
                texto = when {
                    esDuenio && !registrado ->
                        "Tu cancha no aparece activa para los jugadores hasta que registres tu Nequi."
                    esDuenio ->
                        "Tu cancha está activa. Cuando un jugador pague una reserva o productos, " +
                            "el dinero llega a este número."
                    else ->
                        "Cada vez que reserves una cancha o compres productos te llegará una " +
                            "solicitud de pago a este Nequi. La reserva se confirma solo cuando la aceptes."
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (registrado) "Cambiar número" else "Registrar número",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = numero,
                onValueChange = onNumeroChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("300 123 4567", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Outlined.PhoneAndroid, contentDescription = null, tint = TextSecondary)
                },
                prefix = { Text("+57 ", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = DarkCardBackground,
                    unfocusedContainerColor = DarkCardBackground,
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = PrimaryGreen
                )
            )

            errorMensaje?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, fontSize = 14.sp, color = ErrorRed)
            }
            mensajeExito?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, fontSize = 14.sp, color = PrimaryGreen)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onGuardarClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Guardar número Nequi", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
            }

            if (!esDuenio && registrado) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onVerSolicitudesClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = PrimaryGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver solicitudes de pago", color = TextWhite)
                }
            }

            if (registrado) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onQuitarClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Quitar número", color = ErrorRed)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Aviso(
                icono = Icons.Outlined.Info,
                color = WarningYellow,
                texto = "Modo prueba: los cobros se aceptan o rechazan dentro de SportyGo y no " +
                    "mueven dinero real. Falta conectar la app con Nequi."
            )
        }
    }
}

/**
 * Bandeja de un Nequi DE PRUEBA: muestra las solicitudes de cobro que le llegaron al número
 * del jugador para aceptarlas o rechazarlas (lo que haría en la app de Nequi).
 */
@Composable
fun NequiPruebaScreen(
    numero: String?,
    solicitudes: List<SolicitudPago>,
    onBackClick: () -> Unit = {},
    onRegistrarNequiClick: () -> Unit = {},
    onAceptar: (SolicitudPago) -> Unit = {},
    onRechazar: (SolicitudPago) -> Unit = {}
) {
    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            BarraTitulo("Nequi · solicitudes de pago", onBackClick)
            Spacer(modifier = Modifier.height(8.dp))
            Aviso(
                icono = Icons.Outlined.Info,
                color = WarningYellow,
                texto = "Modo prueba: así se verá el cobro en Nequi. Aceptar o rechazar aquí " +
                    "no mueve dinero real."
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (numero == null) {
                EstadoVacio(
                    icono = Icons.Outlined.AccountBalanceWallet,
                    titulo = "No tienes Nequi registrado",
                    descripcion = "Registra tu número para recibir las solicitudes de pago."
                )
                Button(
                    onClick = onRegistrarNequiClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) { Text("Registrar Nequi", color = DarkBackground, fontWeight = FontWeight.Bold) }
                return@Column
            }

            Text(
                text = "Número ${formatoCelular(numero)}",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (solicitudes.isEmpty()) {
                    item {
                        EstadoVacio(
                            icono = Icons.Outlined.ReceiptLong,
                            titulo = "Sin solicitudes de pago",
                            descripcion = "Cuando reserves una cancha, el cobro aparecerá aquí."
                        )
                    }
                }
                items(solicitudes, key = { it.id }) { s ->
                    SolicitudCard(s, onAceptar = { onAceptar(s) }, onRechazar = { onRechazar(s) })
                }
            }
        }
    }
}

@Composable
private fun SolicitudCard(s: SolicitudPago, onAceptar: () -> Unit, onRechazar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatearPrecio(s.monto),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    modifier = Modifier.weight(1f)
                )
                val (texto, color) = when (s.estado) {
                    EstadoPago.PENDIENTE -> "Pendiente" to WarningYellow
                    EstadoPago.APROBADO -> "Pagado" to PrimaryGreen
                    EstadoPago.RECHAZADO -> "Rechazado" to ErrorRed
                }
                Text(
                    text = texto,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(s.descripcion, fontSize = 14.sp, color = TextWhite)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Para Nequi ${formatoCelular(s.nequiDestino)} · " +
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(s.creadaEn)),
                fontSize = 12.sp,
                color = TextSecondary
            )
            if (s.estado == EstadoPago.PENDIENTE) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onRechazar,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Rechazar", color = ErrorRed) }
                    Button(
                        onClick = onAceptar,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Aceptar pago", color = DarkBackground, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun BarraTitulo(titulo: String, onBackClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = TextWhite,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(titulo, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
    }
}

@Composable
private fun Aviso(icono: ImageVector, color: Color, texto: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(12.dp)
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(texto, fontSize = 13.sp, color = TextWhite, lineHeight = 18.sp)
    }
}

/** "3001234567" -> "300 123 4567" */
fun formatoCelular(numero: String): String =
    if (numero.length == 10) "${numero.take(3)} ${numero.substring(3, 6)} ${numero.takeLast(4)}"
    else numero

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MetodoPagoPreview() {
    MetodoPagoScreen(esDuenio = true, nequiRegistrado = null, numero = "")
}
