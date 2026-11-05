package me.juliangarcia.sportygoapp.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import me.juliangarcia.sportygoapp.Routes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Estado de sesión observable desde Compose. La navegación reacciona a estos valores:
 * cuando cambian, las rutas públicas/protegidas redirigen a donde corresponde.
 *
 * Los datos personales (reservas, grabaciones) se cargan SOLO del usuario en sesión,
 * así que una cuenta nueva empieza completamente vacía.
 */
class AuthState(private val repo: AuthRepository) {

    var usuario: User? by mutableStateOf(repo.usuarioEnSesion())
        private set

    /** Cancha del dueño en sesión (null si es jugador o aún no la registra). */
    var cancha: Cancha? by mutableStateOf(null)
        private set

    /** Reservas hechas por el usuario en sesión. */
    var misReservas: List<Reserva> by mutableStateOf(emptyList())
        private set

    /** Grabaciones del usuario en sesión. */
    var misGrabaciones: List<Grabacion> by mutableStateOf(emptyList())
        private set

    /** Reservas que otros jugadores hicieron en la cancha del dueño en sesión. */
    var reservasDeMiCancha: List<Reserva> by mutableStateOf(emptyList())
        private set

    init {
        recargarDatosDeUsuario()
    }

    /** Todas las canchas registradas (para que el jugador elija dónde reservar). */
    fun canchasDisponibles(): List<Cancha> = repo.todasLasCanchas()

    fun canchaPorId(id: String): Cancha? = repo.canchaPorId(id)

    private fun recargarDatosDeUsuario() {
        val u = usuario
        if (u == null) {
            cancha = null
            misReservas = emptyList()
            misGrabaciones = emptyList()
            reservasDeMiCancha = emptyList()
            return
        }
        cancha = if (u.rol == UserRole.DUENIO) repo.canchaDe(u.id) else null
        misReservas = repo.reservasDeUsuario(u.id)
        misGrabaciones = repo.grabacionesDeUsuario(u.id)
        reservasDeMiCancha = cancha?.let { repo.reservasDeCancha(it.id) } ?: emptyList()
    }

    /** Devuelve un mensaje de error, o null si la cuenta se creó (y la sesión quedó iniciada). */
    fun registrar(
        nombreUsuario: String,
        correo: String,
        contrasena: String,
        fechaNacimiento: String
    ): String? = when (val r = repo.registrar(nombreUsuario, correo, contrasena, fechaNacimiento)) {
        is AuthResult.Exito -> {
            usuario = r.usuario
            recargarDatosDeUsuario() // cuenta nueva: todo vacío
            null
        }
        is AuthResult.Error -> r.mensaje
    }

    /** Devuelve un mensaje de error, o null si el inicio de sesión fue correcto. */
    fun iniciarSesion(identificador: String, contrasena: String): String? =
        when (val r = repo.iniciarSesion(identificador, contrasena)) {
            is AuthResult.Exito -> {
                usuario = r.usuario
                recargarDatosDeUsuario()
                null
            }
            is AuthResult.Error -> r.mensaje
        }

    fun cerrarSesion() {
        repo.cerrarSesion()
        usuario = null
        recargarDatosDeUsuario()
    }

    fun elegirRol(rol: UserRole) {
        val actual = usuario ?: return
        val actualizado = repo.elegirRol(actual.id, rol) ?: return
        usuario = actualizado
        recargarDatosDeUsuario()
    }

    /** Guarda la ruta local de la nueva foto de perfil (o null para quitarla). */
    fun actualizarFotoPerfil(fotoUri: String?) {
        val actual = usuario ?: return
        usuario = repo.actualizarFotoPerfil(actual.id, fotoUri) ?: return
    }

    /** Devuelve un mensaje de error, o null si la cancha quedó registrada. */
    fun registrarCancha(
        nombre: String,
        descripcion: String,
        capacidad: String,
        apertura: String,
        cierre: String,
        precio: String,
        imagenUri: String = ""
    ): String? {
        val actual = usuario ?: return "Debes iniciar sesión."
        Validaciones.cancha(nombre.trim(), capacidad, apertura, cierre, precio)?.let { return it }

        val nueva = Cancha(
            id = cancha?.id ?: UUID.randomUUID().toString(),
            ownerId = actual.id,
            imagenUri = imagenUri,
            nombre = nombre.trim(),
            descripcion = descripcion.trim(),
            capacidad = capacidad.toInt(),
            horaApertura = apertura,
            horaCierre = cierre,
            precioHora = precio.toInt()
        )
        repo.guardarCancha(nueva)
        cancha = nueva
        return null
    }

    /** Devuelve un mensaje de error, o null si la reserva quedó guardada. */
    fun reservar(canchaId: String, fecha: LocalDate?, hora: String?): String? {
        val actual = usuario ?: return "Debes iniciar sesión."
        val c = repo.canchaPorId(canchaId) ?: return "La cancha ya no está disponible."
        if (fecha == null) return "Selecciona un día."
        if (hora == null) return "Selecciona una hora."

        val fechaTexto = fecha.format(FORMATO_FECHA)
        val ocupada = repo.reservasDeCancha(c.id).any { it.fecha == fechaTexto && it.hora == hora }
        if (ocupada) return "Ese horario ya está reservado. Elige otro."

        repo.guardarReserva(
            Reserva(
                id = UUID.randomUUID().toString(),
                usuarioId = actual.id,
                canchaId = c.id,
                nombreCancha = c.nombre,
                fecha = fechaTexto,
                hora = hora,
                precio = c.precioHora
            )
        )
        recargarDatosDeUsuario()
        return null
    }

    /** Horas ya reservadas (por cualquier jugador) en una cancha para una fecha. */
    fun horasOcupadas(canchaId: String, fecha: LocalDate): Set<String> {
        val fechaTexto = fecha.format(FORMATO_FECHA)
        return repo.reservasDeCancha(canchaId)
            .filter { it.fecha == fechaTexto }
            .map { it.hora }
            .toSet()
    }

    /** Pantalla "casa" según el estado actual de la sesión. */
    fun rutaInicial(): String {
        val u = usuario ?: return Routes.OPEN_SCREEN
        return when (u.rol) {
            null -> Routes.SELECT_PROFILE
            UserRole.JUGADOR -> Routes.SEARCH_FIELDS
            UserRole.DUENIO ->
                if (cancha != null) Routes.OWNERS_PANEL else Routes.REGISTER_FIELD
        }
    }

    companion object {
        val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}
