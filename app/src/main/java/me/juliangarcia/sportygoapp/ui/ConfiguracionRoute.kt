package me.juliangarcia.sportygoapp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import me.juliangarcia.sportygoapp.data.ArchivosLocales
import me.juliangarcia.sportygoapp.data.AuthState
import me.juliangarcia.sportygoapp.data.UserRole
import me.juliangarcia.sportygoapp.ui.screens.ConfiguracionScreen

/** Pantalla "Configuración" del perfil (cuenta, notificaciones, permisos y sesión). */
@Composable
fun ConfiguracionRoute(
    auth: AuthState,
    onBackClick: () -> Unit,
    onMetodosPagoClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val usuario = auth.usuario ?: return
    val preferencias = auth.preferencias

    var info by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }

    val elegirFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val id = auth.usuario?.id
        if (uri != null && id != null) {
            val copia = ArchivosLocales.copiar(context, uri, "perfiles", "perfil-$id")
            if (copia != null) {
                auth.actualizarFotoPerfil(copia)
                info = "Foto de perfil actualizada."
                error = null
            } else {
                Toast.makeText(context, "No se pudo cargar la imagen.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val version = remember { versionApp(context) }

    ConfiguracionScreen(
        nombreUsuario = usuario.nombreUsuario,
        correo = usuario.correo,
        tipoCuenta = when (usuario.rol) {
            UserRole.JUGADOR -> "Jugador"
            UserRole.DUENIO -> "Dueño de cancha"
            null -> ""
        },
        fechaNacimiento = usuario.fechaNacimiento,
        fotoPerfilUri = usuario.fotoPerfilUri,
        nequi = usuario.nequi?.let { if (it.length >= 4) "••• ${it.takeLast(4)}" else it },
        esJugador = usuario.rol == UserRole.JUGADOR,
        avisosChat = preferencias?.avisosChat ?: true,
        camaraAutomatica = preferencias?.camaraAutomatica ?: true,
        versionApp = version,
        mensajeInfo = info,
        mensajeError = error,
        enviandoCorreo = enviando,
        onBackClick = onBackClick,
        onCambiarFotoClick = {
            elegirFoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onQuitarFotoClick = {
            auth.actualizarFotoPerfil(null)
            info = "Quitaste tu foto de perfil."
            error = null
        },
        onCambiarContrasenaClick = {
            enviando = true
            info = null
            error = null
            scope.launch {
                error = auth.enviarCambioContrasena()
                if (error == null) {
                    info = "Te enviamos un correo a ${usuario.correo} para cambiar tu contraseña. " +
                        "Revisa también la carpeta de spam."
                }
                enviando = false
            }
        },
        onMetodosPagoClick = onMetodosPagoClick,
        onAvisosChatChange = { preferencias?.cambiarAvisosChat(it) },
        onCamaraAutomaticaChange = { preferencias?.cambiarCamaraAutomatica(it) },
        onNotificacionesSistemaClick = {
            abrirAjustes(
                context,
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        },
        onPermisosClick = {
            abrirAjustes(
                context,
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
            )
        },
        // Al cerrar sesión, la navegación detecta que ya no hay usuario y vuelve a la bienvenida.
        onCerrarSesionClick = { auth.cerrarSesion() }
    )
}

/** Abre una pantalla de Ajustes de Android (si el teléfono no la tiene, avisa). */
private fun abrirAjustes(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudieron abrir los ajustes.", Toast.LENGTH_SHORT).show()
    }
}

private fun versionApp(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
} catch (e: Exception) {
    ""
}
