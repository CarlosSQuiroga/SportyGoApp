package me.juliangarcia.sportygoapp.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import me.juliangarcia.sportygoapp.data.AuthState
import me.juliangarcia.sportygoapp.data.DetectorGoles
import me.juliangarcia.sportygoapp.data.Grabacion
import me.juliangarcia.sportygoapp.data.UserRole
import me.juliangarcia.sportygoapp.ui.screens.ConfirmarGolesScreen
import me.juliangarcia.sportygoapp.data.VideosCompartidos
import me.juliangarcia.sportygoapp.ui.screens.EliminarGrabacionDialog
import me.juliangarcia.sportygoapp.ui.screens.RecordVideoScreen
import me.juliangarcia.sportygoapp.ui.screens.RenombrarGrabacionDialog
import me.juliangarcia.sportygoapp.ui.screens.ReproducirGrabacionScreen

/** Lista de grabaciones con el menú de cada video (compartir, momento clave, renombrar, eliminar). */
@Composable
fun GrabacionesRoute(
    auth: AuthState,
    onBackClick: () -> Unit,
    onVerGrabacion: (Grabacion) -> Unit,
    onMomentoClave: (Grabacion) -> Unit,
    onBottomNavTabClick: (String) -> Unit
) {
    val context = LocalContext.current
    var tabActual by rememberSaveable { mutableStateOf("Mis partidos") }
    var aRenombrar by remember { mutableStateOf<Grabacion?>(null) }
    var aEliminar by remember { mutableStateOf<Grabacion?>(null) }

    // El dueño ve los partidos que los anfitriones de las reservas grabaron en su cancha.
    if (auth.usuario?.rol == UserRole.DUENIO) {
        RecordVideoScreen(
            soloLectura = true,
            titulo = "Grabaciones de tu cancha",
            grabaciones = auth.grabacionesDeMiCancha,
            textoVacio = "Aún no hay grabaciones",
            descripcionVacio = "Cuando un jugador grabe su partido en tu cancha, el video te llegará aquí.",
            notaFooter = "Los anfitriones de las reservas graban sus partidos y el video te llega automáticamente.",
            autorDe = { g -> auth.nombreDeUsuario(g.usuarioId) },
            onBackClick = onBackClick,
            onGrabacionClick = onVerGrabacion,
            onBottomNavTabClick = onBottomNavTabClick
        )
        return
    }

    RecordVideoScreen(
        notaFooter = "Revive tus mejores jugadas y comparte los momentos destacados de tus " +
            "partidos. Los videos de tus reservas también le llegan al dueño de la cancha.",
        tabSeleccionada = tabActual,
        grabaciones = auth.misGrabaciones,
        onBackClick = onBackClick,
        onTabSelected = { tabActual = it },
        onGrabacionClick = onVerGrabacion,
        onCompartirClick = { compartirCompleto(context, it) },
        onMomentoClaveClick = { g ->
            if (AuthState.archivoDeVideo(g) == null) avisarSinVideo(context) else onMomentoClave(g)
        },
        onRenombrarClick = { aRenombrar = it },
        onEliminarClick = { aEliminar = it },
        onBottomNavTabClick = onBottomNavTabClick
    )

    DialogosGrabacion(
        auth = auth,
        aRenombrar = aRenombrar,
        aEliminar = aEliminar,
        onCerrarRenombrar = { aRenombrar = null },
        onCerrarEliminar = { aEliminar = null },
        onEliminada = {}
    )
}

/** Reproductor de una grabación: compartir completo o un momento clave, renombrar y eliminar. */
@Composable
fun ReproducirGrabacionRoute(
    auth: AuthState,
    grabacionId: String,
    abrirEnMomentoClave: Boolean,
    onBackClick: () -> Unit,
    /** Revisar o corregir los goles (marcador) de la grabación. */
    onGolesClick: (grabacionId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Se lee de la lista en sesión: así el título cambia al renombrar.
    val grabacion = auth.misGrabaciones.firstOrNull { it.id == grabacionId }
        ?: auth.grabacionPorId(grabacionId)
    if (grabacion == null) {
        // Dueño de la cancha: solo puede ver el video que le llegó desde la nube.
        val deMiCancha = auth.grabacionDeMiCanchaPorId(grabacionId)
        if (deMiCancha == null) {
            LaunchedEffect(Unit) { onBackClick() }
        } else {
            ReproducirGrabacionScreen(
                grabacion = deMiCancha,
                onBackClick = onBackClick,
                videoDisponible = false,
                fuenteVideo = deMiCancha.videoUrl,
                soloLectura = true
            )
        }
        return
    }

    val archivo = remember(grabacion.videoUri) { AuthState.archivoDeVideo(grabacion) }
    val duracion = remember(archivo) { archivo?.let { VideosCompartidos.duracionMs(it) } ?: 0L }
    var preparando by remember { mutableStateOf(false) }
    var aRenombrar by remember { mutableStateOf<Grabacion?>(null) }
    var aEliminar by remember { mutableStateOf<Grabacion?>(null) }

    ReproducirGrabacionScreen(
        grabacion = grabacion,
        onBackClick = onBackClick,
        videoDisponible = archivo != null,
        // Si se grabó en otro teléfono, se ve desde la nube.
        fuenteVideo = grabacion.videoUri.takeIf { archivo != null }
            ?: grabacion.videoUrl.ifBlank { null },
        duracionInicialMs = duracion,
        abrirEnMomentoClave = abrirEnMomentoClave,
        preparandoClip = preparando,
        onCompartirCompletoClick = { compartirCompleto(context, grabacion) },
        onCompartirMomentoClick = { inicio, fin ->
            val origen = archivo ?: return@ReproducirGrabacionScreen avisarSinVideo(context)
            preparando = true
            scope.launch {
                val clip = VideosCompartidos.recortar(context, origen, inicio, fin)
                preparando = false
                if (clip != null) {
                    VideosCompartidos.compartir(context, clip, "${grabacion.titulo} · momento clave")
                } else {
                    Toast.makeText(
                        context,
                        "No se pudo recortar el video. Intenta con otro momento.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        },
        onRenombrarClick = { aRenombrar = grabacion },
        onEliminarClick = { aEliminar = grabacion },
        onGolesClick = { onGolesClick(grabacion.id) }
    )

    DialogosGrabacion(
        auth = auth,
        aRenombrar = aRenombrar,
        aEliminar = aEliminar,
        onCerrarRenombrar = { aRenombrar = null },
        onCerrarEliminar = { aEliminar = null },
        // Al desaparecer la grabación, esta misma ruta vuelve a la lista (ver arriba).
        onEliminada = {}
    )
}

/**
 * Confirmar los goles de una grabación. La primera vez busca en el audio los gritos de gol
 * (queda guardado, para no repetir el análisis) y luego el usuario elige cuáles fueron gol.
 */
@Composable
fun ConfirmarGolesRoute(
    auth: AuthState,
    grabacionId: String,
    onTerminado: () -> Unit
) {
    val grabacion = auth.misGrabaciones.firstOrNull { it.id == grabacionId }
        ?: auth.grabacionPorId(grabacionId)
    if (grabacion == null) {
        LaunchedEffect(Unit) { onTerminado() }
        return
    }
    val archivo = remember(grabacion.videoUri) { AuthState.archivoDeVideo(grabacion) }
    var progreso by remember { mutableFloatStateOf(0f) }
    var sinAudio by remember { mutableStateOf(false) }
    val analizar = archivo != null && !grabacion.audioAnalizado

    LaunchedEffect(grabacion.id, analizar) {
        if (!analizar || archivo == null) return@LaunchedEffect
        val marcas = DetectorGoles.buscar(archivo) { p -> progreso = p }
        sinAudio = marcas == null
        auth.guardarMarcasPosibles(grabacion.id, marcas.orEmpty())
    }

    ConfirmarGolesScreen(
        grabacion = grabacion,
        videoDisponible = archivo != null,
        marcasPosibles = grabacion.marcasPosibles,
        analizando = analizar,
        progreso = progreso,
        sinAudio = sinAudio,
        onGuardar = { goles ->
            auth.guardarMarcador(grabacion.id, goles)
            onTerminado()
        },
        onOmitir = onTerminado
    )
}

@Composable
private fun DialogosGrabacion(
    auth: AuthState,
    aRenombrar: Grabacion?,
    aEliminar: Grabacion?,
    onCerrarRenombrar: () -> Unit,
    onCerrarEliminar: () -> Unit,
    onEliminada: () -> Unit
) {
    val context = LocalContext.current
    var error by remember(aRenombrar) { mutableStateOf<String?>(null) }

    aRenombrar?.let { g ->
        RenombrarGrabacionDialog(
            nombreActual = g.nombre,
            error = error,
            onGuardar = { nuevo ->
                error = auth.renombrarGrabacion(g.id, nuevo)
                if (error == null) onCerrarRenombrar()
            },
            onCancelar = onCerrarRenombrar
        )
    }
    aEliminar?.let { g ->
        EliminarGrabacionDialog(
            titulo = g.titulo,
            onEliminar = {
                auth.eliminarGrabacion(g.id)
                onCerrarEliminar()
                Toast.makeText(context, "Video eliminado.", Toast.LENGTH_SHORT).show()
                onEliminada()
            },
            onCancelar = onCerrarEliminar
        )
    }
}

private fun compartirCompleto(context: Context, g: Grabacion) {
    val archivo = AuthState.archivoDeVideo(g) ?: return avisarSinVideo(context)
    VideosCompartidos.compartir(context, archivo, g.titulo)
}

private fun avisarSinVideo(context: Context) {
    Toast.makeText(
        context,
        "Este video no está en este teléfono: se guarda en el teléfono con el que se grabó.",
        Toast.LENGTH_LONG
    ).show()
}
