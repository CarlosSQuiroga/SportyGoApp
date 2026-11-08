package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val DividerColor = Color(0xFF1B2633)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)
private val ErrorRed = Color(0xFFFF6B6B)

/** Pantalla "Configuración" del perfil. Sin estado: todo llega por parámetros. */
@Composable
fun ConfiguracionScreen(
    modifier: Modifier = Modifier,
    nombreUsuario: String = "",
    correo: String = "",
    /** "Jugador" o "Dueño de cancha". */
    tipoCuenta: String = "",
    fechaNacimiento: String = "",
    fotoPerfilUri: String? = null,
    /** Número Nequi oculto (ej. "••• 4567") o null si no tiene. */
    nequi: String? = null,
    esJugador: Boolean = true,
    avisosChat: Boolean = true,
    camaraAutomatica: Boolean = true,
    versionApp: String = "",
    /** Mensaje de resultado (ej. "Te enviamos un correo..."). */
    mensajeInfo: String? = null,
    mensajeError: String? = null,
    enviandoCorreo: Boolean = false,
    onBackClick: () -> Unit = {},
    onCambiarFotoClick: () -> Unit = {},
    onQuitarFotoClick: () -> Unit = {},
    onCambiarContrasenaClick: () -> Unit = {},
    onMetodosPagoClick: () -> Unit = {},
    onAvisosChatChange: (Boolean) -> Unit = {},
    onCamaraAutomaticaChange: (Boolean) -> Unit = {},
    onNotificacionesSistemaClick: () -> Unit = {},
    onPermisosClick: () -> Unit = {},
    onCerrarSesionClick: () -> Unit = {}
) {
    var confirmarContrasena by remember { mutableStateOf(false) }
    var confirmarQuitarFoto by remember { mutableStateOf(false) }
    var confirmarSalir by remember { mutableStateOf(false) }
    var verAcercaDe by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize(), color = DarkBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
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
                Text(
                    text = "Configuración",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Resumen de la cuenta
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCardBackground)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(DividerColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (fotoPerfilUri != null) {
                        AsyncImage(
                            model = fotoPerfilUri,
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nombreUsuario,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(text = correo, fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = listOf(tipoCuenta, fechaNacimiento.takeIf { it.isNotBlank() }
                            ?.let { "Nació el $it" })
                            .filterNotNull()
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        fontSize = 12.sp,
                        color = PrimaryGreen
                    )
                }
            }

            if (mensajeInfo != null || mensajeError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = mensajeError ?: mensajeInfo.orEmpty(),
                    fontSize = 14.sp,
                    color = if (mensajeError != null) ErrorRed else PrimaryGreen
                )
            }

            Seccion("Cuenta") {
                OpcionNavegar(
                    titulo = "Cambiar foto de perfil",
                    icono = Icons.Outlined.PhotoCamera,
                    onClick = onCambiarFotoClick
                )
                if (fotoPerfilUri != null) {
                    Separador()
                    OpcionNavegar(
                        titulo = "Quitar foto de perfil",
                        icono = Icons.Outlined.Delete,
                        onClick = { confirmarQuitarFoto = true }
                    )
                }
                Separador()
                OpcionNavegar(
                    titulo = "Cambiar contraseña",
                    descripcion = if (enviandoCorreo) "Enviando correo..."
                    else "Te enviamos un enlace a tu correo",
                    icono = Icons.Outlined.Lock,
                    onClick = { if (!enviandoCorreo) confirmarContrasena = true }
                )
                Separador()
                OpcionNavegar(
                    titulo = "Métodos de pago",
                    descripcion = nequi?.let { "Nequi $it" } ?: "Sin número Nequi registrado",
                    icono = Icons.Outlined.AccountBalanceWallet,
                    onClick = onMetodosPagoClick
                )
            }

            Seccion("Notificaciones") {
                OpcionInterruptor(
                    titulo = "Mensajes del chat",
                    descripcion = "Avisarme en el teléfono cuando me escriban",
                    icono = Icons.AutoMirrored.Outlined.Chat,
                    activo = avisosChat,
                    onCambio = onAvisosChatChange
                )
                if (esJugador) {
                    Separador()
                    OpcionInterruptor(
                        titulo = "Abrir la cámara al empezar el partido",
                        descripcion = "Si entras a la app durante tu reserva, abrir la grabación",
                        icono = Icons.Outlined.Videocam,
                        activo = camaraAutomatica,
                        onCambio = onCamaraAutomaticaChange
                    )
                }
                Separador()
                OpcionNavegar(
                    titulo = "Notificaciones del sistema",
                    descripcion = "Sonido, vibración y permisos de notificación",
                    icono = Icons.Outlined.Notifications,
                    onClick = onNotificacionesSistemaClick
                )
            }

            Seccion("Privacidad") {
                OpcionNavegar(
                    titulo = "Permisos de la app",
                    descripcion = "Ubicación, cámara, micrófono y fotos",
                    icono = Icons.Outlined.Security,
                    onClick = onPermisosClick
                )
            }

            Seccion("Información") {
                OpcionNavegar(
                    titulo = "Acerca de SportyGo",
                    descripcion = "Versión $versionApp",
                    icono = Icons.Outlined.Info,
                    onClick = { verAcercaDe = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCardBackground)
                    .clickable { confirmarSalir = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Cerrar sesión",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = ErrorRed
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (confirmarContrasena) {
        Confirmacion(
            titulo = "Cambiar contraseña",
            texto = "Te enviaremos un correo a $correo con un enlace para crear una nueva contraseña.",
            confirmar = "Enviar correo",
            onConfirmar = {
                confirmarContrasena = false
                onCambiarContrasenaClick()
            },
            onCancelar = { confirmarContrasena = false }
        )
    }
    if (confirmarQuitarFoto) {
        Confirmacion(
            titulo = "Quitar foto de perfil",
            texto = "Tu perfil se mostrará con el ícono genérico.",
            confirmar = "Quitar",
            onConfirmar = {
                confirmarQuitarFoto = false
                onQuitarFotoClick()
            },
            onCancelar = { confirmarQuitarFoto = false }
        )
    }
    if (confirmarSalir) {
        Confirmacion(
            titulo = "Cerrar sesión",
            texto = "¿Seguro que quieres salir de tu cuenta en este teléfono?",
            confirmar = "Cerrar sesión",
            onConfirmar = {
                confirmarSalir = false
                onCerrarSesionClick()
            },
            onCancelar = { confirmarSalir = false }
        )
    }
    if (verAcercaDe) {
        AlertDialog(
            onDismissRequest = { verAcercaDe = false },
            containerColor = DarkCardBackground,
            title = { Text("SportyGo", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Versión $versionApp\n\nReserva canchas, paga con Nequi, compra en el kiosco, " +
                        "chatea con el dueño y graba tus partidos.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { verAcercaDe = false }) {
                    Text("Cerrar", color = PrimaryGreen)
                }
            }
        )
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = titulo.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCardBackground)
    ) {
        contenido()
    }
}

@Composable
private fun Separador() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 54.dp)
            .height(1.dp)
            .background(DividerColor)
    )
}

@Composable
private fun OpcionNavegar(
    titulo: String,
    icono: ImageVector,
    onClick: () -> Unit,
    descripcion: String? = null
) {
    FilaOpcion(titulo, descripcion, icono, Modifier.clickable { onClick() }) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun OpcionInterruptor(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    activo: Boolean,
    onCambio: (Boolean) -> Unit
) {
    FilaOpcion(titulo, descripcion, icono, Modifier.clickable { onCambio(!activo) }) {
        Switch(
            checked = activo,
            onCheckedChange = onCambio,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DarkBackground,
                checkedTrackColor = PrimaryGreen,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DividerColor,
                uncheckedBorderColor = DividerColor
            )
        )
    }
}

@Composable
private fun FilaOpcion(
    titulo: String,
    descripcion: String?,
    icono: ImageVector,
    modifier: Modifier,
    final: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, fontSize = 15.sp, color = TextWhite)
            if (descripcion != null) {
                Text(text = descripcion, fontSize = 12.sp, color = TextSecondary)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        final()
    }
}

@Composable
private fun Confirmacion(
    titulo: String,
    texto: String,
    confirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = DarkCardBackground,
        title = { Text(titulo, color = TextWhite, fontWeight = FontWeight.Bold) },
        text = { Text(texto, color = TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text(confirmar, color = PrimaryGreen) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar", color = TextSecondary) }
        }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ConfiguracionPreview() {
    ConfiguracionScreen(
        nombreUsuario = "carlos",
        correo = "carlos@correo.com",
        tipoCuenta = "Jugador",
        fechaNacimiento = "01/01/2000",
        nequi = "••• 4567",
        versionApp = "1.0"
    )
}
