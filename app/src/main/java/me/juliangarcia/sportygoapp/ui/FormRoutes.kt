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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import me.juliangarcia.sportygoapp.data.ArchivosLocales
import me.juliangarcia.sportygoapp.data.AuthState
import me.juliangarcia.sportygoapp.data.Cancha
import me.juliangarcia.sportygoapp.data.UserRole
import me.juliangarcia.sportygoapp.data.Validaciones
import me.juliangarcia.sportygoapp.ui.screens.AddFieldScreen
import me.juliangarcia.sportygoapp.ui.screens.CreateTeamScreen
import me.juliangarcia.sportygoapp.ui.screens.DetallesCanchaScreen
import me.juliangarcia.sportygoapp.ui.screens.LoginScreen
import me.juliangarcia.sportygoapp.ui.screens.ProfileScreen
import me.juliangarcia.sportygoapp.ui.screens.RegisterFieldScreen
import me.juliangarcia.sportygoapp.ui.screens.SearchFieldsScreen
import me.juliangarcia.sportygoapp.ui.screens.SelectAccountScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
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
    var enviando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
            // Firebase responde por la red: se ignoran toques repetidos mientras tanto.
            if (!enviando) {
                enviando = true
                scope.launch {
                    error = auth.registrar(nombre, correo, contrasena, fecha)
                    enviando = false
                }
            }
        }
    )
}

/** Pantalla "Inicia con tu cuenta". Solo entra quien ya tiene una cuenta creada. */
@Composable
fun InicioSesionRoute(auth: AuthState) {
    var usuarioCorreo by rememberSaveable { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SelectAccountScreen(
        usuarioCorreo = usuarioCorreo,
        contrasena = contrasena,
        errorMensaje = error,
        onUsuarioCorreoChange = { usuarioCorreo = it; error = null },
        onContrasenaChange = { contrasena = it; error = null },
        onIniciarSesionClick = {
            if (!enviando) {
                enviando = true
                scope.launch {
                    error = auth.iniciarSesion(usuarioCorreo, contrasena)
                    enviando = false
                }
            }
        }
    )
}

/** Ubicación de la cancha elegida por el dueño en el mapa. */
data class UbicacionCancha(val latitud: Double, val longitud: Double, val direccion: String)

/** Pantalla "Registra tu cancha" (dueño): elegir en el mapa dónde está la cancha. */
@Composable
fun RegistrarUbicacionRoute(
    inicial: UbicacionCancha?,
    onUbicacionRegistrada: (UbicacionCancha) -> Unit
) {
    val context = LocalContext.current
    val ubicacionUsuario = rememberUbicacionUsuario()

    // Si ya había elegido una ubicación (vuelve con "Cambiar"), el mapa arranca ahí.
    val puntoInicial = remember { inicial?.let { GeoPoint(it.latitud, it.longitud) } }
    var centro by remember { mutableStateOf(puntoInicial) }
    var direccion by remember { mutableStateOf(inicial?.direccion) }
    var buscandoDireccion by remember { mutableStateOf(false) }
    var recentrar by remember { mutableStateOf(0) }
    // A dónde mover el mapa. Al llegar la ubicación del usuario solo se usa si aún no ha
    // tocado el mapa, para no deshacer el punto que ya eligió.
    var destino by remember { mutableStateOf(puntoInicial) }
    var tocado by remember { mutableStateOf(puntoInicial != null) }
    LaunchedEffect(ubicacionUsuario.punto) {
        val punto = ubicacionUsuario.punto
        if (punto != null && !tocado) destino = punto
    }

    // Busca la dirección cuando el mapa deja de moverse un momento.
    LaunchedEffect(centro) {
        val punto = centro ?: return@LaunchedEffect
        buscandoDireccion = true
        delay(600)
        direccion = direccionDe(context, punto)
        buscandoDireccion = false
    }

    val texto = when {
        centro == null && ubicacionUsuario.buscando -> "Buscando tu ubicación..."
        centro == null -> "Mueve el mapa hasta tu cancha"
        buscandoDireccion -> "Buscando dirección..."
        direccion != null -> direccion!!
        else -> "Ubicación seleccionada"
    }

    RegisterFieldScreen(
        direccionTexto = texto,
        puedeConfirmar = centro != null,
        mapa = { m ->
            MapaSeleccion(
                irA = destino,
                recentrar = recentrar,
                onCentroChange = { centro = it },
                onToque = { tocado = true },
                modifier = m
            )
        },
        onMiUbicacionClick = {
            val punto = ubicacionUsuario.punto
            if (punto != null) {
                destino = punto
                recentrar++
            } else {
                // Aún no hay ubicación: al llegar, el mapa se moverá ahí.
                tocado = false
                ubicacionUsuario.solicitar()
            }
        },
        onRegistrarUbicacionClick = {
            val punto = centro ?: return@RegisterFieldScreen
            onUbicacionRegistrada(
                UbicacionCancha(
                    latitud = punto.latitude,
                    longitud = punto.longitude,
                    direccion = if (buscandoDireccion) "" else direccion.orEmpty()
                )
            )
        }
    )
}

/** Pantalla "Datos de la cancha" (dueño). */
@Composable
fun AddFieldRoute(
    auth: AuthState,
    ubicacion: UbicacionCancha,
    onCambiarUbicacionClick: () -> Unit,
    onCanchaRegistrada: () -> Unit
) {
    val context = LocalContext.current
    val marcador = remember(ubicacion) {
        listOf(
            MarcadorCancha(
                id = "mi-cancha",
                punto = GeoPoint(ubicacion.latitud, ubicacion.longitud),
                titulo = "Tu cancha"
            )
        )
    }

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
            // No reemplaza: la imagen guardada se conserva si el dueño no termina el registro.
            val copia = ArchivosLocales.copiar(
                context, uri, "canchas", nombreArchivo, reemplazar = false
            )
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
        direccion = ubicacion.direccion,
        mapa = { m -> MapaCiudad(
                ubicacion = null,
                canchas = marcador,
                zoom = 16.0,
                interactivo = false,
                modifier = m
            ) },
        onCambiarUbicacionClick = onCambiarUbicacionClick,
        onPrecioHoraChange = { valor ->
            precio = valor.filter { c -> c.isDigit() }.take(9)
            error = null
        },
        onRegistrarCanchaClick = {
            error = auth.registrarCancha(
                nombre, descripcion, capacidad, apertura, cierre, precio,
                imagenUri = imagen,
                latitud = ubicacion.latitud,
                longitud = ubicacion.longitud,
                direccion = ubicacion.direccion
            )
            if (error == null) {
                // Ya guardada: se borran las imágenes elegidas antes que no se usaron.
                auth.usuario?.id?.let { id ->
                    ArchivosLocales.limpiar(context, "canchas", "cancha-$id-", conservar = imagen)
                }
                onCanchaRegistrada()
            }
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
    val ubicacion = rememberUbicacionUsuario()

    // Se leen al entrar a la pantalla (incluye canchas registradas por otros dueños).
    val todas = remember { auth.canchasDisponibles() }

    // Distancia (en metros) desde el jugador a cada cancha con ubicación.
    val distancias = remember(todas, ubicacion.punto) {
        val yo = ubicacion.punto ?: return@remember emptyMap<String, Double>()
        todas.mapNotNull { c ->
            val lat = c.latitud ?: return@mapNotNull null
            val lon = c.longitud ?: return@mapNotNull null
            c.id to yo.distanceToAsDouble(GeoPoint(lat, lon))
        }.toMap()
    }

    // Canchas disponibles cerca: primero las más cercanas (las sin ubicación al final).
    val canchas = remember(todas, busqueda, distancias) {
        val q = busqueda.trim()
        val filtradas = if (q.isEmpty()) {
            todas
        } else {
            todas.filter {
                it.nombre.contains(q, ignoreCase = true) ||
                    it.descripcion.contains(q, ignoreCase = true) ||
                    it.direccion.contains(q, ignoreCase = true)
            }
        }
        filtradas.sortedBy { distancias[it.id] ?: Double.MAX_VALUE }
    }
    // Canchas con ubicación, dibujadas en el mapa (respetan la búsqueda).
    val marcadores = remember(canchas) {
        canchas.mapNotNull { c ->
            val lat = c.latitud ?: return@mapNotNull null
            val lon = c.longitud ?: return@mapNotNull null
            MarcadorCancha(c.id, GeoPoint(lat, lon), c.nombre)
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
        canchas = canchas,
        distancias = distancias,
        mapa = { m ->
            MapaCiudad(
                ubicacion = ubicacion.punto,
                canchas = marcadores,
                onCanchaClick = { id -> todas.firstOrNull { it.id == id }?.let(onCanchaClick) },
                modifier = m
            )
        },
        onNotificacionesClick = onNotificacionesClick,
        onUbicacionClick = { ubicacion.solicitar() },
        onMapTargetClick = { ubicacion.solicitar() },
        onBusquedaChange = { busqueda = it },
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
    onReservaCreada: () -> Unit,
    onCrearEquipoClick: () -> Unit = {}
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

    // Productos del kiosco y cantidades elegidas (carrito).
    val productos = remember(cancha.id) { auth.productosDeCancha(cancha.id) }
    var carrito by rememberSaveable { mutableStateOf(mapOf<String, Int>()) }

    // Equipo que hace la reserva (opcional, null = sin equipo).
    var equipoId by rememberSaveable { mutableStateOf<String?>(null) }

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
        productos = productos,
        cantidades = carrito,
        onCantidadChange = { id, cantidad ->
            carrito = if (cantidad <= 0) carrito - id else carrito + (id to cantidad)
        },
        equipos = auth.misEquipos,
        equipoSeleccionadoId = equipoId,
        onEquipoSelected = {
            equipoId = it
            error = null
        },
        onCrearEquipoClick = onCrearEquipoClick,
        onReservarClick = {
            error = auth.reservar(cancha.id, fecha, hora, carrito, equipoId)
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

/** Pantalla "Nuevo equipo": nombre + imagen del escudo. Se guarda en la base de datos local. */
@Composable
fun CrearEquipoRoute(
    auth: AuthState,
    onBackClick: () -> Unit,
    onEquipoCreado: () -> Unit
) {
    val context = LocalContext.current
    var nombre by rememberSaveable { mutableStateOf("") }
    var escudo by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    // Prefijo de las imágenes elegidas en este formulario (para borrar las que no se usen).
    val borrador = rememberSaveable { "escudo-${java.util.UUID.randomUUID()}" }

    val elegirEscudo = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val copia = ArchivosLocales.copiar(context, uri, "equipos", borrador)
            if (copia != null) {
                escudo = copia
                error = null
            } else {
                Toast.makeText(context, "No se pudo cargar la imagen.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun salir() {
        // Sin guardar: se borran las imágenes elegidas.
        ArchivosLocales.limpiar(context, "equipos", "$borrador-", conservar = "")
        onBackClick()
    }
    androidx.activity.compose.BackHandler { salir() }

    CreateTeamScreen(
        nombre = nombre,
        escudoUri = escudo,
        errorMensaje = error,
        onBackClick = { salir() },
        onNombreChange = {
            nombre = it.take(30)
            error = null
        },
        onElegirEscudoClick = {
            elegirEscudo.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onQuitarEscudoClick = { escudo = "" },
        onCrearClick = {
            error = auth.crearEquipo(nombre, escudo)
            if (error == null) {
                ArchivosLocales.limpiar(context, "equipos", "$borrador-", conservar = escudo)
                Toast.makeText(context, "Equipo creado.", Toast.LENGTH_SHORT).show()
                onEquipoCreado()
            }
        }
    )
}

/** Perfil del usuario: permite elegir la foto de perfil desde los archivos del teléfono. */
@Composable
fun PerfilRoute(
    auth: AuthState,
    onBackClick: () -> Unit,
    onMisReservasClick: () -> Unit,
    onMisGrabacionesClick: () -> Unit,
    onMisEquiposClick: () -> Unit,
    onMetodosPagoClick: () -> Unit,
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
        onMetodosPagoClick = onMetodosPagoClick,
        // Al cerrar sesión, la navegación detecta que ya no hay usuario y vuelve a la bienvenida.
        onCerrarSesionClick = { auth.cerrarSesion() },
        onBottomNavTabClick = onBottomNavTabClick
    )
}
