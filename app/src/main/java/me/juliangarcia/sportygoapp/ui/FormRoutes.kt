package me.juliangarcia.sportygoapp.ui

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import me.juliangarcia.sportygoapp.data.ArchivosLocales
import me.juliangarcia.sportygoapp.data.AuthState
import me.juliangarcia.sportygoapp.data.Cancha
import me.juliangarcia.sportygoapp.data.UserRole
import me.juliangarcia.sportygoapp.data.Validaciones
import me.juliangarcia.sportygoapp.ui.screens.AddFieldScreen
import me.juliangarcia.sportygoapp.ui.screens.DetallesCanchaScreen
import me.juliangarcia.sportygoapp.ui.screens.LoginScreen
import me.juliangarcia.sportygoapp.ui.screens.ProfileScreen
import me.juliangarcia.sportygoapp.ui.screens.SearchFieldsScreen
import me.juliangarcia.sportygoapp.ui.screens.SelectAccountScreen
import java.time.LocalDate
import java.time.LocalTime

/*
 * Las pantallas del proyecto son "stateless" (reciben valores y callbacks).
 * Estos composables guardan el estado de los formularios y llaman a la lógica de cuentas.
 * No navegan: al cambiar la sesión (AuthState), el NavHost redirige solo.
 */

/** Pantalla "Crea tu cuenta". */
@Composable
fun RegistroRoute(auth: AuthState) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") } // la contraseña no se guarda en el estado restaurable
    var fecha by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    LoginScreen(
        nombreUsuario = nombre,
        correo = correo,
        contrasena = contrasena,
        fechaNacimiento = fecha,
        errorMensaje = error,
        onNombreUsuarioChange = { nombre = it; error = null },
        onCorreoChange = { correo = it; error = null },
        onContrasenaChange = { contrasena = it; error = null },
        onFechaNacimientoChange = { fecha = Validaciones.formatearFechaInput(it); error = null },
        onCrearCuentaClick = {
            error = auth.registrar(nombre, correo, contrasena, fecha)
        }
    )
}

/** Pantalla "Inicia con tu cuenta". Solo entra quien ya tiene una cuenta creada. */
@Composable
fun InicioSesionRoute(auth: AuthState) {
    var usuarioCorreo by rememberSaveable { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    SelectAccountScreen(
        usuarioCorreo = usuarioCorreo,
        contrasena = contrasena,
        errorMensaje = error,
        onUsuarioCorreoChange = { usuarioCorreo = it; error = null },
        onContrasenaChange = { contrasena = it; error = null },
        onIniciarSesionClick = {
            error = auth.iniciarSesion(usuarioCorreo, contrasena)
        }
    )
}

/** Pantalla "Datos de la cancha" (dueño). */
@Composable
fun AddFieldRoute(auth: AuthState, onCanchaRegistrada: () -> Unit) {
    val context = LocalContext.current

    var nombre by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var capacidad by rememberSaveable { mutableStateOf("") }
    var apertura by rememberSaveable { mutableStateOf("") }
    var cierre by rememberSaveable { mutableStateOf("") }
    var precio by rememberSaveable { mutableStateOf("") }
    var imagen by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    // Selector de imágenes del sistema (no requiere permisos de almacenamiento).
    val elegirImagen = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val nombreArchivo = "cancha-${auth.usuario?.id ?: "nueva"}"
            val copia = ArchivosLocales.copiar(context, uri, "canchas", nombreArchivo)
            if (copia != null) {
                imagen = copia
            } else {
                Toast.makeText(context, "No se pudo cargar la imagen.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun elegirHora(actual: String, alElegir: (String) -> Unit) {
        val minutos = Validaciones.minutosDelDia(actual) ?: (8 * 60)
        TimePickerDialog(
            context,
            { _, hora, minuto ->
                error = null
                alElegir("$hora:${minuto.toString().padStart(2, '0')}")
            },
            minutos / 60,
            minutos % 60,
            true
        ).show()
    }

    AddFieldScreen(
        imagenUri = imagen,
        onElegirImagenClick = {
            elegirImagen.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        nombreCancha = nombre,
        descripcion = descripcion,
        capacidadJugadores = capacidad,
        horaApertura = apertura.ifEmpty { "Seleccionar" },
        horaCierre = cierre.ifEmpty { "Seleccionar" },
        precioHora = precio,
        errorMensaje = error,
        onNombreCanchaChange = { nombre = it; error = null },
        onDescripcionChange = { descripcion = it; error = null },
        onCapacidadChange = { valor ->
            capacidad = valor.filter { c -> c.isDigit() }.take(3)
            error = null
        },
        onHoraAperturaClick = { elegirHora(apertura) { apertura = it } },
        onHoraCierreClick = { elegirHora(cierre) { cierre = it } },
        onEscanearQrClick = {
            Toast.makeText(context, "Escanear QR estará disponible pronto.", Toast.LENGTH_SHORT)
                .show()
        },
        onPrecioHoraChange = { valor ->
            precio = valor.filter { c -> c.isDigit() }.take(9)
            error = null
        },
        onRegistrarCanchaClick = {
            error = auth.registrarCancha(
                nombre, descripcion, capacidad, apertura, cierre, precio, imagenUri = imagen
            )
            if (error == null) onCanchaRegistrada()
        }
    )
}

/** Inicio del jugador: mapa con su ubicación + canchas registradas para reservar. */
@Composable
fun BuscarCanchasRoute(
    auth: AuthState,
    onNotificacionesClick: () -> Unit,
    onCanchaClick: (Cancha) -> Unit,
    onBottomNavTabClick: (String) -> Unit
) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var deporte by rememberSaveable { mutableStateOf("Fútbol") }
    val ubicacion = rememberUbicacionUsuario()

    // Se leen al entrar a la pantalla (incluye canchas registradas por otros dueños).
    val todas = remember { auth.canchasDisponibles() }
    val canchas = remember(todas, busqueda) {
        val q = busqueda.trim()
        if (q.isEmpty()) {
            todas
        } else {
            todas.filter {
                it.nombre.contains(q, ignoreCase = true) ||
                    it.descripcion.contains(q, ignoreCase = true)
            }
        }
    }

    val textoUbicacion = when {
        ubicacion.ciudad != null -> ubicacion.ciudad!!
        ubicacion.buscando -> "Buscando tu ubicación..."
        ubicacion.punto != null -> "Tu ubicación actual"
        !ubicacion.permisoConcedido -> "Toca para activar tu ubicación"
        else -> "Ubicación no disponible"
    }

    SearchFieldsScreen(
        ubicacionTexto = textoUbicacion,
        busquedaTexto = busqueda,
        deporteSeleccionado = deporte,
        canchas = canchas,
        mapa = { m -> MapaCiudad(ubicacion = ubicacion.punto, modifier = m) },
        onNotificacionesClick = onNotificacionesClick,
        onUbicacionClick = { ubicacion.solicitar() },
        onMapTargetClick = { ubicacion.solicitar() },
        onBusquedaChange = { busqueda = it },
        onDeporteTabClick = { deporte = it },
        onCanchaClick = onCanchaClick,
        onBottomNavTabClick = onBottomNavTabClick
    )
}

/** Detalle de una cancha y elección de día/hora para reservar. */
@Composable
fun DetallesCanchaRoute(
    auth: AuthState,
    canchaId: String,
    onBackClick: () -> Unit,
    onReservaCreada: () -> Unit
) {
    val cancha = remember(canchaId) { auth.canchaPorId(canchaId) }
    if (cancha == null) {
        LaunchedEffect(Unit) { onBackClick() }
        return
    }

    val hoy = remember { LocalDate.now() }
    val fechas = remember(hoy) { (0L..3L).map { hoy.plusDays(it) } }
    val etiquetas = remember(fechas) {
        fechas.mapIndexed { i, f ->
            when (i) {
                0 -> "Hoy"
                1 -> "Mañana"
                else -> f.dayOfMonth.toString()
            }
        }
    }

    var diaIndice by rememberSaveable { mutableStateOf(0) }
    var hora by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    // Sube cuando se intenta reservar, para refrescar las horas ocupadas.
    var refrescar by remember { mutableStateOf(0) }

    val fecha = fechas[diaIndice]
    val horas = remember(cancha, fecha) { horasDeReserva(cancha, fecha) }
    val ocupadas = remember(cancha.id, fecha, refrescar) { auth.horasOcupadas(cancha.id, fecha) }

    DetallesCanchaScreen(
        cancha = cancha,
        dias = etiquetas,
        diaSeleccionado = diaIndice,
        horas = horas,
        horasOcupadas = ocupadas,
        horaSeleccionada = hora,
        errorMensaje = error,
        onBackClick = onBackClick,
        onDiaSelected = {
            diaIndice = it
            hora = null
            error = null
        },
        onHoraSelected = {
            hora = it
            error = null
        },
        onReservarClick = {
            error = auth.reservar(cancha.id, fecha, hora)
            refrescar++
            if (error == null) onReservaCreada()
        }
    )
}

/**
 * Horas de inicio (cada reserva dura 1 hora) entre la apertura y el cierre de la cancha.
 * Si el cierre es menor que la apertura, la cancha cierra después de medianoche.
 * Para hoy, solo se muestran las horas que aún no han pasado.
 */
private fun horasDeReserva(cancha: Cancha, fecha: LocalDate): List<String> {
    val apertura = Validaciones.minutosDelDia(cancha.horaApertura) ?: return emptyList()
    var cierre = Validaciones.minutosDelDia(cancha.horaCierre) ?: return emptyList()
    if (cierre <= apertura) cierre += 24 * 60

    val ahora = LocalTime.now().let { it.hour * 60 + it.minute }
    val esHoy = fecha == LocalDate.now()

    return generateSequence(apertura) { it + 60 }
        .takeWhile { it + 60 <= cierre }
        .filter { !esHoy || it > ahora }
        .map { total ->
            val m = total % (24 * 60)
            "${m / 60}:${(m % 60).toString().padStart(2, '0')}"
        }
        .toList()
}

/** Perfil del usuario: permite elegir la foto de perfil desde los archivos del teléfono. */
@Composable
fun PerfilRoute(
    auth: AuthState,
    onBackClick: () -> Unit,
    onMisReservasClick: () -> Unit,
    onMisGrabacionesClick: () -> Unit,
    onMisEquiposClick: () -> Unit,
    onBottomNavTabClick: (String) -> Unit
) {
    val context = LocalContext.current
    val usuario = auth.usuario

    val elegirFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val id = auth.usuario?.id
        if (uri != null && id != null) {
            val copia = ArchivosLocales.copiar(context, uri, "perfiles", "perfil-$id")
            if (copia != null) {
                auth.actualizarFotoPerfil(copia)
            } else {
                Toast.makeText(context, "No se pudo cargar la imagen.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ProfileScreen(
        nombreUsuario = usuario?.nombreUsuario ?: "",
        subtituloUsuario = usuario?.correo ?: "",
        fotoPerfilUri = usuario?.fotoPerfilUri,
        esDuenio = usuario?.rol == UserRole.DUENIO,
        onBackClick = onBackClick,
        onCambiarFotoClick = {
            elegirFoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onMisReservasClick = onMisReservasClick,
        onMisGrabacionesClick = onMisGrabacionesClick,
        onMisEquiposClick = onMisEquiposClick,
        // Al cerrar sesión, la navegación detecta que ya no hay usuario y vuelve a la bienvenida.
        onCerrarSesionClick = { auth.cerrarSesion() },
        onBottomNavTabClick = onBottomNavTabClick
    )
}
