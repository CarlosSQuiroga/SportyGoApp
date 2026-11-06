package me.juliangarcia.sportygoapp.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import me.juliangarcia.sportygoapp.Routes
import me.juliangarcia.sportygoapp.avisos.Avisos
import me.juliangarcia.sportygoapp.pagos.EstadoPago
import me.juliangarcia.sportygoapp.pagos.PasarelaPagos
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
class AuthState(
    private val repo: AuthRepository,
    /** Notificaciones del sistema y alarmas (null en previews/pruebas). */
    private val avisos: Avisos? = null,
    /** Cobros por Nequi (hoy de prueba, ver NequiSimulado). */
    private val pagos: PasarelaPagos? = null
) {

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

    /** Bandeja de notificaciones del usuario en sesión (más recientes primero). */
    var misNotificaciones: List<Notificacion> by mutableStateOf(emptyList())
        private set

    /** Productos del kiosco de la cancha del dueño en sesión. */
    var misProductos: List<Producto> by mutableStateOf(emptyList())
        private set

    init {
        recargarDatosDeUsuario()
    }

    /**
     * Canchas que el jugador puede reservar: solo las activas, es decir, las de dueños
     * que registraron su Nequi para recibir los pagos.
     */
    fun canchasDisponibles(): List<Cancha> = repo.todasLasCanchas().filter { canchaActiva(it) }

    fun canchaPorId(id: String): Cancha? = repo.canchaPorId(id)

    private fun recargarDatosDeUsuario() {
        val u = usuario
        if (u == null) {
            cancha = null
            misReservas = emptyList()
            misGrabaciones = emptyList()
            reservasDeMiCancha = emptyList()
            misNotificaciones = emptyList()
            misProductos = emptyList()
            return
        }
        cancha = if (u.rol == UserRole.DUENIO) repo.canchaDe(u.id) else null
        misProductos = cancha?.let { productosDeCancha(it.id) } ?: emptyList()
        misReservas = repo.reservasDeUsuario(u.id)
        misGrabaciones = repo.grabacionesDeUsuario(u.id)
        // El dueño solo cuenta las reservas pagadas.
        reservasDeMiCancha = cancha?.let { c ->
            repo.reservasDeCancha(c.id).filter { it.estado == EstadoReserva.CONFIRMADA }
        } ?: emptyList()
        entregarPendientes(u.id)
        misNotificaciones = repo.notificacionesDe(u.id)
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
            notificar(
                usuarioId = r.usuario.id,
                tipo = TipoNotificacion.CUENTA_CREADA,
                titulo = "¡Bienvenido a SportyGo, ${r.usuario.nombreUsuario}!",
                mensaje = "Tu cuenta se creó correctamente. Elige tu perfil para empezar."
            )
            notificar(
                usuarioId = r.usuario.id,
                tipo = TipoNotificacion.VINCULAR_NEQUI,
                titulo = "Vincula tu número Nequi",
                mensaje = "Regístralo en Perfil > Métodos de pago. Como jugador, lo usas para " +
                    "pagar tus reservas; como dueño de cancha, ahí recibes los pagos y sin él tu " +
                    "cancha no aparece activa."
            )
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
        imagenUri: String = "",
        latitud: Double? = null,
        longitud: Double? = null,
        direccion: String = ""
    ): String? {
        val actual = usuario ?: return "Debes iniciar sesión."
        if (latitud == null || longitud == null) return "Marca la ubicación de la cancha en el mapa."
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
            precioHora = precio.toInt(),
            latitud = latitud,
            longitud = longitud,
            direccion = direccion.trim(),
            kiosco = cancha?.kiosco ?: ""
        )
        repo.guardarCancha(nueva)
        cancha = nueva
        return null
    }

    /**
     * Reserva la cancha y, opcionalmente, compra productos del kiosco.
     * [carrito] = id del producto -> cantidad. Devuelve un mensaje de error, o null si quedó guardada.
     */
    fun reservar(
        canchaId: String,
        fecha: LocalDate?,
        hora: String?,
        carrito: Map<String, Int> = emptyMap()
    ): String? {
        val actual = usuario ?: return "Debes iniciar sesión."
        val c = repo.canchaPorId(canchaId) ?: return "La cancha ya no está disponible."
        if (fecha == null) return "Selecciona un día."
        if (hora == null) return "Selecciona una hora."
        val pasarela = pagos ?: return "Los pagos no están disponibles en este momento."

        // Pagos con Nequi: el jugador paga desde su número y el dueño recibe en el suyo.
        val nequiJugador = actual.nequi
            ?: return "Registra tu número Nequi en Perfil > Métodos de pago para poder reservar."
        val nequiDuenio = repo.usuarioPorId(c.ownerId)?.nequi
            ?: return "Esta cancha no está recibiendo reservas por ahora."

        val fechaTexto = fecha.format(FORMATO_FECHA)
        val ocupada = repo.reservasDeCancha(c.id)
            .any { it.estado != EstadoReserva.RECHAZADA && it.fecha == fechaTexto && it.hora == hora }
        if (ocupada) return "Ese horario ya está reservado. Elige otro."

        // Precio y nombre actuales de cada producto (solo los que siguen existiendo).
        val compra = repo.productosDeCancha(c.id).mapNotNull { p ->
            val cantidad = carrito[p.id] ?: 0
            if (cantidad <= 0) null
            else ItemCompra(p.id, p.nombre, p.precio, cantidad)
        }
        val total = c.precioHora + compra.sumOf { it.subtotal }

        // Se envía el cobro al Nequi del jugador. La reserva aparta el horario, pero solo
        // queda confirmada cuando el jugador acepta el pago (ver sincronizarPagos).
        val cobro = pasarela.solicitarCobro(
            nequiOrigen = nequiJugador,
            nequiDestino = nequiDuenio,
            monto = total,
            descripcion = "SportyGo · ${c.nombre} · $fechaTexto $hora" +
                if (compra.isNotEmpty()) " + productos" else ""
        )

        val reserva = Reserva(
            id = UUID.randomUUID().toString(),
            usuarioId = actual.id,
            canchaId = c.id,
            nombreCancha = c.nombre,
            fecha = fechaTexto,
            hora = hora,
            precio = c.precioHora,
            productos = compra,
            estado = EstadoReserva.PENDIENTE_PAGO,
            pagoId = cobro.id,
            nequiPagador = nequiJugador,
            nequiDestino = nequiDuenio
        )
        repo.guardarReserva(reserva)

        notificar(
            usuarioId = actual.id,
            tipo = TipoNotificacion.PAGO_PENDIENTE,
            titulo = "Acepta el pago en Nequi · ${formatoPesos(total)}",
            mensaje = "Enviamos una solicitud de pago a tu Nequi ${ocultarNumero(nequiJugador)} " +
                "por tu reserva en ${c.nombre} el $fechaTexto a las $hora. " +
                "La reserva se confirma cuando aceptes el pago en Nequi.",
            // Id fijo para poder quitarla del teléfono cuando el pago se resuelva.
            id = idAvisoPago(reserva.id)
        )

        recargarDatosDeUsuario()
        return null
    }

    // ---------------------------------------------------------------
    // Pagos con Nequi
    // ---------------------------------------------------------------

    /**
     * Revisa en la pasarela los cobros pendientes y confirma o libera cada reserva.
     * Se llama al volver a la app (p. ej. después de aceptar el pago en Nequi).
     */
    fun sincronizarPagos() {
        val pasarela = pagos ?: return
        var cambio = false
        repo.todasLasReservas()
            .filter { it.estado == EstadoReserva.PENDIENTE_PAGO && it.pagoId.isNotBlank() }
            .forEach { r ->
                when (pasarela.consultarEstado(r.pagoId)) {
                    EstadoPago.APROBADO -> { confirmarReserva(r); cambio = true }
                    EstadoPago.RECHAZADO -> { rechazarReserva(r); cambio = true }
                    else -> Unit
                }
            }
        if (cambio) recargarDatosDeUsuario()
    }

    /** Pago aceptado: la reserva queda confirmada y se avisa al jugador y al dueño. */
    private fun idAvisoPago(reservaId: String) = "pago-$reservaId"

    private fun confirmarReserva(r: Reserva) {
        val confirmada = r.copy(estado = EstadoReserva.CONFIRMADA)
        repo.actualizarReserva(confirmada)
        avisos?.quitar(idAvisoPago(r.id))

        // A la hora de la reserva llega el aviso para que el anfitrión grabe el partido.
        avisos?.programarGrabacion(confirmada)

        val c = repo.canchaPorId(r.canchaId)
        val jugador = repo.usuarioPorId(r.usuarioId)?.nombreUsuario ?: "Un jugador"
        val dondeReclamar = c?.let { lugarKiosco(it) } ?: "el kiosco de la cancha"

        notificar(
            usuarioId = r.usuarioId,
            tipo = TipoNotificacion.RESERVA_HECHA,
            titulo = "Reserva confirmada",
            mensaje = buildString {
                append("Pagaste ${formatoPesos(r.total)} con Nequi. ")
                append("Tu reserva en ${r.nombreCancha} el ${r.fecha} a las ${r.hora} está confirmada. ")
                if (r.productos.isNotEmpty()) {
                    append("Productos: ${resumenCompra(r.productos)}. Reclámalos en $dondeReclamar. ")
                }
                append("A esa hora te avisaremos para grabar el partido.")
            }
        )

        // El dueño las ve en el teléfono cuando entra con su cuenta.
        val ownerId = c?.ownerId ?: return
        if (ownerId == r.usuarioId) return
        notificar(
            usuarioId = ownerId,
            tipo = TipoNotificacion.RESERVA_RECIBIDA,
            titulo = "Nueva reserva en ${r.nombreCancha}",
            mensaje = "$jugador reservó tu cancha el ${r.fecha} a las ${r.hora} y pagó " +
                "${formatoPesos(r.total)}. El dinero se envió a tu Nequi ${ocultarNumero(r.nequiDestino)}."
        )
        if (r.productos.isNotEmpty()) {
            notificar(
                usuarioId = ownerId,
                tipo = TipoNotificacion.PEDIDO_RECIBIDO,
                titulo = "Pedido para el kiosco · ${formatoPesos(r.totalProductos)}",
                mensaje = "$jugador compró ${resumenCompra(r.productos)} para su reserva del " +
                    "${r.fecha} a las ${r.hora}. Lo reclamará en $dondeReclamar."
            )
        }
    }

    /** Pago rechazado: el horario vuelve a quedar libre. */
    private fun rechazarReserva(r: Reserva) {
        repo.actualizarReserva(r.copy(estado = EstadoReserva.RECHAZADA))
        avisos?.quitar(idAvisoPago(r.id))
        notificar(
            usuarioId = r.usuarioId,
            tipo = TipoNotificacion.PAGO_RECHAZADO,
            titulo = "Pago rechazado",
            mensaje = "Rechazaste el pago en Nequi, así que la reserva en ${r.nombreCancha} " +
                "el ${r.fecha} a las ${r.hora} no se hizo. Ese horario quedó libre."
        )
    }

    /**
     * Registra (o quita, con texto vacío) el número Nequi del usuario en sesión.
     * Devuelve un mensaje de error, o null si quedó guardado.
     */
    fun actualizarNequi(numero: String): String? {
        val actual = usuario ?: return "Debes iniciar sesión."
        val limpio = numero.filter { it.isDigit() }
        if (limpio.isNotEmpty() && !Regex("^3\\d{9}$").matches(limpio)) {
            return "Escribe un celular colombiano de 10 dígitos que empiece por 3."
        }
        usuario = repo.actualizarNequi(actual.id, limpio.ifEmpty { null }) ?: return "No se pudo guardar."
        return null
    }

    /** true si la cancha puede recibir reservas (su dueño tiene Nequi para recibir pagos). */
    fun canchaActiva(c: Cancha): Boolean = repo.usuarioPorId(c.ownerId)?.nequi != null

    private fun ocultarNumero(numero: String): String =
        if (numero.length >= 4) "••• ${numero.takeLast(4)}" else numero

    // ---------------------------------------------------------------
    // Kiosco e inventario de la cancha (dueño)
    // ---------------------------------------------------------------

    /** Productos que un jugador puede comprar al reservar [canchaId]. */
    fun productosDeCancha(canchaId: String): List<Producto> =
        repo.productosDeCancha(canchaId).sortedBy { it.nombre.lowercase() }

    fun productoPorId(id: String): Producto? = misProductos.firstOrNull { it.id == id }

    /**
     * Crea (si [id] es null) o edita un producto del kiosco de la cancha del dueño.
     * Devuelve un mensaje de error, o null si quedó guardado.
     */
    fun guardarProducto(
        id: String?,
        nombre: String,
        precio: String,
        categoria: CategoriaProducto,
        imagenUri: String,
        iconoId: String
    ): String? {
        val c = cancha ?: return "Primero registra tu cancha."
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isEmpty()) return "Escribe el nombre del producto."
        if (nombreLimpio.length > 40) return "El nombre es muy largo (máximo 40 caracteres)."
        val valor = precio.toIntOrNull() ?: return "Escribe el precio del producto."
        if (valor <= 0) return "El precio debe ser mayor que 0."
        if (imagenUri.isBlank() && iconoId.isBlank()) {
            return "Elige una foto (galería o cámara) o uno de los íconos."
        }
        val repetido = misProductos.any {
            it.id != id && it.nombre.equals(nombreLimpio, ignoreCase = true)
        }
        if (repetido) return "Ya tienes un producto con ese nombre."

        repo.guardarProducto(
            Producto(
                id = id ?: UUID.randomUUID().toString(),
                canchaId = c.id,
                nombre = nombreLimpio,
                precio = valor,
                categoria = categoria,
                imagenUri = imagenUri,
                iconoId = iconoId
            )
        )
        misProductos = productosDeCancha(c.id)
        return null
    }

    fun eliminarProducto(id: String) {
        val c = cancha ?: return
        repo.eliminarProducto(id)
        misProductos = productosDeCancha(c.id)
    }

    /** Guarda dónde están los productos para reclamar (ej. "Junto a la entrada"). */
    fun actualizarKiosco(ubicacion: String) {
        val c = cancha ?: return
        val actualizada = c.copy(kiosco = ubicacion.trim().take(80))
        repo.guardarCancha(actualizada)
        cancha = actualizada
    }

    private fun lugarKiosco(c: Cancha): String =
        if (c.kiosco.isBlank()) "el kiosco de ${c.nombre}" else "el kiosco de ${c.nombre} (${c.kiosco})"

    private fun resumenCompra(items: List<ItemCompra>): String =
        items.joinToString(", ") { "${it.cantidad}× ${it.nombre}" }

    private fun formatoPesos(valor: Int): String =
        "$" + java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("es-CO"))
            .format(valor)

    // ---------------------------------------------------------------
    // Notificaciones
    // ---------------------------------------------------------------

    /**
     * Guarda la notificación en la bandeja de [usuarioId]. Si ese usuario es el que tiene la
     * sesión abierta, también sale en el teléfono; si no, saldrá cuando entre con su cuenta.
     */
    private fun notificar(
        usuarioId: String,
        tipo: TipoNotificacion,
        titulo: String,
        mensaje: String,
        id: String = UUID.randomUUID().toString()
    ) {
        val paraSesionActual = usuarioId == usuario?.id
        val n = Notificacion(
            id = id,
            usuarioId = usuarioId,
            tipo = tipo,
            titulo = titulo,
            mensaje = mensaje,
            creadaEn = System.currentTimeMillis(),
            mostrada = paraSesionActual
        )
        repo.guardarNotificacion(n)
        if (paraSesionActual) {
            avisos?.mostrar(n)
            misNotificaciones = repo.notificacionesDe(usuarioId)
        }
    }

    /** Muestra en el teléfono las notificaciones que llegaron mientras el usuario no tenía sesión. */
    private fun entregarPendientes(usuarioId: String) {
        val pendientes = repo.notificacionesDe(usuarioId).filter { !it.mostrada }
        if (pendientes.isEmpty()) return
        pendientes.sortedBy { it.creadaEn }.forEach { avisos?.mostrar(it) }
        val ids = pendientes.map { it.id }.toSet()
        repo.actualizarNotificaciones(usuarioId, { it.id in ids }) { it.copy(mostrada = true) }
    }

    fun marcarNotificacionLeida(id: String) {
        val u = usuario ?: return
        repo.actualizarNotificaciones(u.id, { it.id == id }) { it.copy(leida = true) }
        misNotificaciones = repo.notificacionesDe(u.id)
    }

    fun marcarTodasLeidas() {
        val u = usuario ?: return
        repo.actualizarNotificaciones(u.id, { !it.leida }) { it.copy(leida = true) }
        misNotificaciones = repo.notificacionesDe(u.id)
    }

    /** Vuelve a leer los datos guardados (p. ej. al volver a la app: pudo llegar un aviso). */
    fun refrescar() {
        val u = usuario ?: return
        usuario = repo.usuarioEnSesion() ?: u
        // Pudo aceptarse o rechazarse un pago en Nequi mientras la app estaba en segundo plano.
        sincronizarPagos()
        recargarDatosDeUsuario()
    }

    // ---------------------------------------------------------------
    // Grabación del partido
    // ---------------------------------------------------------------

    /**
     * Reserva del jugador en sesión que está en curso ahora mismo y todavía no se grabó
     * (ni el anfitrión eligió "Ahora no"). Si existe, la app abre la cámara al entrar.
     */
    fun reservaParaGrabar(): Reserva? {
        val u = usuario ?: return null
        if (u.rol != UserRole.JUGADOR) return null
        val omitidas = repo.reservasSinGrabar()
        return repo.reservasDeUsuario(u.id).firstOrNull { r ->
            r.estado == EstadoReserva.CONFIRMADA && Avisos.enCurso(r) &&
                r.id !in omitidas && !repo.reservaYaGrabada(r.id)
        }
    }

    fun reservaPorId(id: String): Reserva? =
        repo.reservaPorId(id)?.takeIf { it.usuarioId == usuario?.id }

    /** El anfitrión decidió no grabar esta reserva: no se vuelve a abrir la cámara para ella. */
    fun omitirGrabacion(reservaId: String) = repo.marcarSinGrabar(reservaId)

    /** Guarda el video grabado en "Grabaciones". */
    fun guardarGrabacion(reserva: Reserva, videoUri: String) {
        val u = usuario ?: return
        repo.guardarGrabacion(
            Grabacion(
                id = UUID.randomUUID().toString(),
                usuarioId = u.id,
                videoUri = videoUri,
                fechaGuardado = LocalDate.now().format(FORMATO_FECHA),
                horaReserva = reserva.hora,
                nombreCancha = reserva.nombreCancha,
                reservaId = reserva.id
            )
        )
        misGrabaciones = repo.grabacionesDeUsuario(u.id)
    }

    fun grabacionPorId(id: String): Grabacion? =
        repo.grabacionPorId(id)?.takeIf { it.usuarioId == usuario?.id }

    /** Horas ya reservadas (por cualquier jugador) en una cancha para una fecha. */
    fun horasOcupadas(canchaId: String, fecha: LocalDate): Set<String> {
        val fechaTexto = fecha.format(FORMATO_FECHA)
        // Las pendientes de pago también apartan el horario; las rechazadas lo liberan.
        return repo.reservasDeCancha(canchaId)
            .filter { it.fecha == fechaTexto && it.estado != EstadoReserva.RECHAZADA }
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
