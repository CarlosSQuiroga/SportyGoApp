package me.juliangarcia.sportygoapp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.text.style.TextOverflow
import me.juliangarcia.sportygoapp.data.Equipo
import me.juliangarcia.sportygoapp.data.Reserva
import java.io.File
import java.util.concurrent.TimeUnit

private val DarkBackground = Color(0xFF0B131D)
private val DarkCard = Color(0xCC0B131D)
private val PrimaryGreen = Color(0xFF64F089)
private val RecordRed = Color(0xFFE5484D)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)

/**
 * Cámara para grabar el partido de [reserva]. Se abre sola cuando el anfitrión entra a la app
 * durante la hora reservada. Al detener la grabación el video queda en "Grabaciones".
 *
 * @param onVideoGuardado guarda el video (se llama aunque el usuario ya haya salido de la pantalla).
 * @param onTerminado la grabación terminó y se guardó: ir a "Grabaciones".
 * @param onAhoraNo el anfitrión no quiere grabar este partido.
 * @param onSalir salir sin grabar (la cámara volverá a abrirse al entrar de nuevo a la app).
 */
@Composable
fun GrabarPartidoScreen(
    reserva: Reserva,
    /** Equipos que se pueden elegir como contrincante. */
    rivales: List<Equipo> = emptyList(),
    /** Escudo del equipo que reservó (vacío = escudo con la inicial). */
    escudoEquipo: String = "",
    rivalSeleccionadoId: String? = null,
    onRivalSelected: (String?) -> Unit = {},
    onVideoGuardado: (videoUri: String) -> Unit,
    onTerminado: () -> Unit,
    onAhoraNo: () -> Unit,
    onSalir: () -> Unit
) {
    val context = LocalContext.current

    var permisoCamara by remember { mutableStateOf(tienePermiso(context, Manifest.permission.CAMERA)) }
    var permisoPedido by remember { mutableStateOf(false) }
    val pedirPermisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        permisoPedido = true
        permisoCamara = resultado[Manifest.permission.CAMERA] == true ||
            tienePermiso(context, Manifest.permission.CAMERA)
    }
    LaunchedEffect(Unit) {
        val faltan = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .filterNot { tienePermiso(context, it) }
        if (faltan.isNotEmpty()) pedirPermisos.launch(faltan.toTypedArray())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (permisoCamara) {
            CamaraGrabacion(
                reserva = reserva,
                enfrentamiento = {
                    Enfrentamiento(
                        reserva = reserva,
                        escudoEquipo = escudoEquipo,
                        rivales = rivales,
                        rivalSeleccionadoId = rivalSeleccionadoId,
                        onRivalSelected = onRivalSelected
                    )
                },
                onVideoGuardado = onVideoGuardado,
                onTerminado = onTerminado,
                onAhoraNo = onAhoraNo
            )
        } else {
            SinPermisoCamara(
                mostrarBoton = permisoPedido,
                onPedirPermiso = {
                    pedirPermisos.launch(
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                    )
                },
                onAhoraNo = onAhoraNo,
                onSalir = onSalir
            )
        }
    }
}

@SuppressLint("MissingPermission") // El audio solo se activa si el permiso está concedido.
@Composable
private fun CamaraGrabacion(
    reserva: Reserva,
    /** Equipo que reservó vs. contrincante (con el selector del rival). */
    enfrentamiento: @Composable () -> Unit,
    onVideoGuardado: (String) -> Unit,
    onTerminado: () -> Unit,
    onAhoraNo: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val guardar by rememberUpdatedState(onVideoGuardado)
    val terminar by rememberUpdatedState(onTerminado)

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val videoCapture = remember {
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.HD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                )
            )
            .build()
        VideoCapture.withOutput(recorder)
    }

    var grabacion by remember { mutableStateOf<Recording?>(null) }
    var duracionNanos by remember { mutableLongStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }
    // false cuando la pantalla ya no está visible: no se navega desde callbacks tardíos.
    val estado = remember { EstadoPantalla() }

    // Abre la cámara trasera ligada al ciclo de vida de la pantalla.
    DisposableEffect(lifecycleOwner) {
        val futuro = ProcessCameraProvider.getInstance(context)
        var proveedor: ProcessCameraProvider? = null
        futuro.addListener({
            if (!estado.activa) return@addListener
            try {
                val p = futuro.get()
                proveedor = p
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                p.unbindAll()
                p.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    videoCapture
                )
            } catch (e: Exception) {
                error = "No se pudo abrir la cámara."
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            estado.activa = false
            // Si sale de la pantalla grabando, el video se detiene y se guarda igual.
            grabacion?.stop()
            proveedor?.unbindAll()
        }
    }

    val grabando = grabacion != null

    // Que la pantalla no se apague mientras graba el partido.
    DisposableEffect(grabando) {
        previewView.keepScreenOn = grabando
        onDispose { previewView.keepScreenOn = false }
    }

    fun iniciar() {
        error = null
        val carpeta = File(context.filesDir, "grabaciones").apply { mkdirs() }
        val archivo = File(carpeta, "partido-${reserva.id}-${System.currentTimeMillis()}.mp4")
        var pendiente = videoCapture.output.prepareRecording(
            context,
            FileOutputOptions.Builder(archivo).build()
        )
        if (tienePermiso(context, Manifest.permission.RECORD_AUDIO)) {
            pendiente = pendiente.withAudioEnabled()
        }
        duracionNanos = 0L
        grabacion = pendiente.start(ContextCompat.getMainExecutor(context)) { evento ->
            when (evento) {
                is VideoRecordEvent.Status ->
                    duracionNanos = evento.recordingStats.recordedDurationNanos

                is VideoRecordEvent.Finalize -> {
                    grabacion = null
                    if (videoUtil(evento, archivo)) {
                        guardar(Uri.fromFile(archivo).toString())
                        if (estado.activa) terminar()
                    } else {
                        archivo.delete()
                        error = "No se pudo guardar la grabación. Intenta de nuevo."
                    }
                }
            }
        }
    }

    // Atrás mientras graba = detener y guardar.
    BackHandler(enabled = grabando) { grabacion?.stop() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Encabezado: datos de la reserva y estado.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkCard)
                .padding(16.dp)
        ) {
            Text(
                text = reserva.nombreCancha.ifBlank { "Tu partido" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                text = "${reserva.fecha} · ${reserva.hora} – ${reserva.horaFin}",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            enfrentamiento()
            Spacer(modifier = Modifier.height(10.dp))
            if (grabando) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(RecordRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Grabando  ${formatoDuracion(duracionNanos)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
            } else {
                Text(
                    text = "Eres el anfitrión: posiciona el teléfono para que se vea toda la " +
                        "cancha y toca el botón para empezar a grabar.",
                    fontSize = 14.sp,
                    color = TextWhite,
                    lineHeight = 19.sp
                )
            }
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, fontSize = 13.sp, color = RecordRed)
            }
        }

        // Controles inferiores.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BotonGrabar(
                grabando = grabando,
                onClick = { if (grabando) grabacion?.stop() else iniciar() }
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (grabando) {
                Text(
                    text = "Toca para detener y guardar",
                    fontSize = 13.sp,
                    color = TextWhite,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DarkCard)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            } else {
                TextButton(onClick = onAhoraNo) {
                    Text(
                        text = "No grabar este partido",
                        color = TextWhite,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(DarkCard)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * "Equipo que reservó  vs  Contrincante". El contrincante se puede elegir (o cambiar) antes
 * o durante la grabación; se guarda junto con el video.
 */
@Composable
private fun Enfrentamiento(
    reserva: Reserva,
    escudoEquipo: String,
    rivales: List<Equipo>,
    rivalSeleccionadoId: String?,
    onRivalSelected: (String?) -> Unit
) {
    var menuAbierto by remember { mutableStateOf(false) }
    val rival = rivales.firstOrNull { it.id == rivalSeleccionadoId }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Equipo que reservó
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (reserva.nombreEquipo.isNotBlank()) {
                EscudoDeEquipo(nombre = reserva.nombreEquipo, escudoUri = escudoEquipo, alto = 32.dp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = reserva.nombreEquipo.ifBlank { "Sin equipo" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (reserva.nombreEquipo.isBlank()) TextSecondary else TextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = "vs",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryGreen,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Contrincante (selector)
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x33FFFFFF))
                    .clickable { menuAbierto = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (rival != null) {
                    EscudoDeEquipo(nombre = rival.nombre, escudoUri = rival.escudoUri, alto = 26.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = rival?.nombre ?: "Elegir rival",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (rival == null) PrimaryGreen else TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Elegir equipo contrincante",
                    tint = TextWhite
                )
            }

            DropdownMenu(
                expanded = menuAbierto,
                onDismissRequest = { menuAbierto = false }
            ) {
                if (rivales.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No hay otros equipos registrados") },
                        enabled = false,
                        onClick = {}
                    )
                }
                if (rival != null) {
                    DropdownMenuItem(
                        text = { Text("Sin rival") },
                        onClick = {
                            onRivalSelected(null)
                            menuAbierto = false
                        }
                    )
                }
                rivales.forEach { equipo ->
                    DropdownMenuItem(
                        text = { Text(equipo.nombre) },
                        leadingIcon = {
                            EscudoDeEquipo(
                                nombre = equipo.nombre,
                                escudoUri = equipo.escudoUri,
                                alto = 26.dp
                            )
                        },
                        onClick = {
                            onRivalSelected(equipo.id)
                            menuAbierto = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BotonGrabar(grabando: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(CircleShape)
            .border(4.dp, TextWhite, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (grabando) 30.dp else 60.dp)
                .clip(if (grabando) RoundedCornerShape(6.dp) else CircleShape)
                .background(RecordRed)
        )
    }
}

@Composable
private fun SinPermisoCamara(
    mostrarBoton: Boolean,
    onPedirPermiso: () -> Unit,
    onAhoraNo: () -> Unit,
    onSalir: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Videocam,
            contentDescription = null,
            tint = PrimaryGreen,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Es hora de grabar tu partido",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Para grabar, SportyGo necesita permiso para usar la cámara " +
                "(y el micrófono para el sonido).",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (mostrarBoton) {
            Button(
                onClick = onPedirPermiso,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = DarkBackground
                )
            ) { Text("Dar permiso", fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(8.dp))
        }
        TextButton(onClick = onSalir) { Text("Más tarde", color = TextWhite) }
        TextButton(onClick = onAhoraNo) { Text("No grabar este partido", color = TextSecondary) }
    }
}

private class EstadoPantalla {
    var activa = true
}

private fun tienePermiso(context: Context, permiso: String): Boolean =
    ContextCompat.checkSelfPermission(context, permiso) == PackageManager.PERMISSION_GRANTED

/**
 * El video sirve si el archivo tiene datos y terminó bien o por una causa que igual lo cierra
 * correctamente (p. ej. la cámara se cerró al salir de la app).
 */
private fun videoUtil(evento: VideoRecordEvent.Finalize, archivo: File): Boolean {
    if (!archivo.exists() || archivo.length() == 0L) return false
    return !evento.hasError() || evento.error in setOf(
        VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE,
        VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED,
        VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED,
        VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE
    )
}

private fun formatoDuracion(nanos: Long): String {
    val total = TimeUnit.NANOSECONDS.toSeconds(nanos)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
