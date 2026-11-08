package me.juliangarcia.sportygoapp.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import me.juliangarcia.sportygoapp.data.AuthState
import me.juliangarcia.sportygoapp.data.Grabacion
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

    RecordVideoScreen(
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
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Se lee de la lista en sesión: así el título cambia al renombrar.
    val grabacion = auth.misGrabaciones.firstOrNull { it.id == grabacionId }
        ?: auth.grabacionPorId(grabacionId)
    if (grabacion == null) {
        LaunchedEffect(Unit) { onBackClick() }
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
        onEliminarClick = { aEliminar = grabacion }
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
