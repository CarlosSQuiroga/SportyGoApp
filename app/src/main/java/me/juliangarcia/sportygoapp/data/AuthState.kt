package me.juliangarcia.sportygoapp.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import me.juliangarcia.sportygoapp.Routes
import me.juliangarcia.sportygoapp.avisos.Avisos
import me.juliangarcia.sportygoapp.pagos.EstadoPago
import me.juliangarcia.sportygoapp.pagos.PasarelaPagos
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
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
    private val pagos: PasarelaPagos? = null,
    /** Ajustes de "Configuración" guardados en el teléfono (null en previews/pruebas). */
    val preferencias: Preferencias? = null
) {

    private val scope = MainScope()

    /**
     * true mientras se descargan los datos de la sesión guardada al abrir la app. La
     * navegación espera a que termine para saber a qué pantalla ir.
     */
    var cargando: Boolean by mutableStateOf(true)
        private set

    var usuario: User? by mutableStateOf(null)
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

    /**
     * Grabaciones que los anfitriones de las reservas hicieron en la cancha del dueño en
     * sesión (solo las que ya terminaron de subirse a la nube), la más reciente primero.
     */
    var grabacionesDeMiCancha: List<Grabacion> by mutableStateOf(emptyList())
        private set

    /** Grabaciones cuyo video se está subiendo ahora (para no subirlo dos veces). */
    private val subiendoVideos = mutableSetOf<String>()

    /** Reservas que otros jugadores hicieron en la cancha del dueño en sesión. */
    var reservasDeMiCancha: List<Reserva> by mutableStateOf(emptyList())
        private set

    /** Bandeja de notificaciones del usuario en sesión (más recientes primero). */
    var misNotificaciones: List<Notificacion> by mutableStateOf(emptyList())
        private set

    /** Productos del kiosco de la cancha del dueño en sesión. */
    var misProductos: List<Producto> by mutableStateOf(emptyList())
        private set

    /** Equipos creados por el usuario en sesión (los que puede elegir al reservar). */
    var misEquipos: List<Equipo> by mutableStateOf(emptyList())
        private set

    /** Mensajes de los chats del usuario en sesión (con dueños o con jugadores). */
    var misMensajes: List<Mensaje> by mutableStateOf(emptyList())
        private set

    /** Chat que se está viendo en pantalla: sus mensajes nuevos no generan aviso. */
    var chatAbierto: String? = null

    /** Mensajes recibidos ya avisados en el teléfono (o que ya estaban al iniciar sesión). */
    private val mensajesAvisados = mutableSetOf<String>()
    private var mensajesCargados = false

    init {
        // Cambios que llegan de la nube (otro teléfono reservó, aceptaron un pago, etc.).
        NubeDatos.onCambio = {
            usuario?.let { actual -> usuario = repo.usuarioPorId(actual.id) ?: actual }
            recargarDatosDeUsuario()
        }
        scope.launch {
            // Firebase recuerda la sesión: si había una, se descargan sus datos antes de mostrar nada.
            val uid = repo.uidEnSesion()
            if (uid != null) {
                repo.cargarDatos(uid)
                usuario = repo.usuarioEnSesion()
                if (usuario == null) repo.cerrarSesion() // la cuenta ya no tiene perfil
                alEntrar()
            }
            cargando = false
        }
    }

    /** Al abrir la app o iniciar sesión: cargar los datos y programar los avisos de grabación. */
    private fun alEntrar() {
        recargarDatosDeUsuario()
        avisos?.reprogramar(misReservas)
        subirVideosPendientes()
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
            grabacionesDeMiCancha = emptyList()
            reservasDeMiCancha = emptyList()
            misNotificaciones = emptyList()
            misProductos = emptyList()
            misEquipos = emptyList()
            misMensajes = emptyList()
            mensajesAvisados.clear()
            mensajesCargados = false
            return
        }
        misMensajes = repo.misMensajes().sortedBy { it.enviadoEn }
        avisarMensajesNuevos(u.id)
        misEquipos = repo.equiposDe(u.id).sortedBy { it.nombre.lowercase() }
        cancha = if (u.rol == UserRole.DUENIO) repo.canchaDe(u.id) else null
        misProductos = cancha?.let { productosDeCancha(it.id) } ?: emptyList()
        misReservas = repo.reservasDeUsuario(u.id)
        misGrabaciones = repo.grabacionesDeUsuario(u.id)
        grabacionesDeMiCancha = if (u.rol == UserRole.DUENIO) {
            repo.grabacionesDeCancha(u.id)
                .filter { it.videoUrl.isNotBlank() }
                .sortedWith(
                    compareByDescending<Grabacion> {
                        runCatching { LocalDate.parse(it.fechaGuardado, FORMATO_FECHA) }.getOrNull()
                    }.thenByDescending { Validaciones.minutosDelDia(it.horaReserva) }
                )
        } else emptyList()
        // El dueño solo cuenta las reservas pagadas.
        reservasDeMiCancha = cancha?.let { c ->
            repo.reservasDeCancha(c.id).filter { it.estado == EstadoReserva.CONFIRMADA }
        } ?: emptyList()
        entregarPendientes(u.id)
        misNotificaciones = repo.notificacionesDe(u.id)
    }

    /** Devuelve un mensaje de error, o null si la cuenta se creó (y la sesión quedó iniciada). */
    suspend fun registrar(
        nombreUsuario: String,
        correo: String,
        contrasena: String,
        fechaNacimiento: String
    ): String? = when (val r = repo.registrar(nombreUsuario, correo, contrasena, fechaNacimiento)) {
        is AuthResult.Exito -> {
            repo.cargarDatos(r.usuario.id)
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
    suspend fun iniciarSesion(identificador: String, contrasena: String): String? =
        when (val r = repo.iniciarSesion(identificador, contrasena)) {
            is AuthResult.Exito -> {
                usuario = r.usuario
                alEntrar()
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

    /**
     * Envía al correo de la cuenta un enlace de Firebase para crear una nueva contraseña.
     * Devuelve un mensaje de error, o null si se envió.
     */
    suspend fun enviarCambioContrasena(): String? {
        val correo = usuario?.correo?.takeIf { it.isNotBlank() }
            ?: return "Tu cuenta no tiene un correo registrado."
        return repo.enviarRestablecerContrasena(correo)
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
        // Se relee lo guardado (el repositorio conserva el id si el dueño ya tenía cancha).
        cancha = repo.canchaDe(actual.id) ?: nueva
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
        carrito: Map<String, Int> = emptyMap(),
        /** Equipo del jugador que hace la reserva (null = sin equipo). */
        equipoId: String? = null,
        /** Horas seguidas desde [hora]. */
        duracionHoras: Int = 1
    ): String? {
        val actual = usuario ?: return "Debes iniciar sesión."
        val c = repo.canchaPorId(canchaId) ?: return "La cancha ya no está disponible."
        if (fecha == null) return "Selecciona un día."
        if (hora == null) return "Selecciona una hora."
        if (duracionHoras < 1) return "Elige cuántas horas quieres reservar."
        val bloque = Validaciones.horasSeguidas(hora, duracionHoras)
        val disponibles = Validaciones.horasDeReserva(c, fecha)
        if (!bloque.all { it in disponibles }) {
            return "La cancha no está abierta todas esas horas. Elige menos horas."
        }
        val pasarela = pagos ?: return "Los pagos no están disponibles en este momento."

        // Pagos con Nequi: el jugador paga desde su número y el dueño recibe en el suyo.
        val nequiJugador = actual.nequi
            ?: return "Registra tu número Nequi en Perfil > Métodos de pago para poder reservar."
        val nequiDuenio = repo.usuarioPorId(c.ownerId)?.nequi
            ?: return "Esta cancha no está recibiendo reservas por ahora."

        val fechaTexto = fecha.format(FORMATO_FECHA)
        val ocupada = horasOcupadas(c.id, fecha).intersect(bloque.toSet())
        if (ocupada.isNotEmpty()) {
            return "Las ${ocupada.joinToString(", ")} ya están reservadas. Elige otro horario."
        }
        val precioCancha = c.precioHora * duracionHoras
        val horario = "${hora} a ${Validaciones.sumarHoras(hora, duracionHoras)}"

        // Precio y nombre actuales de cada producto (solo los que siguen existiendo).
        val compra = repo.productosDeCancha(c.id).mapNotNull { p ->
            val cantidad = carrito[p.id] ?: 0
            if (cantidad <= 0) null
            else ItemCompra(p.id, p.nombre, p.precio, cantidad)
        }
        val total = precioCancha + compra.sumOf { it.subtotal }

        // Solo puede reservar a nombre de un equipo que creó.
        val equipo = equipoId?.let { id ->
            repo.equipoPorId(id)?.takeIf { it.creadorId == actual.id }
                ?: return "Ese equipo ya no existe. Elige otro o reserva sin equipo."
        }

        // Se envía el cobro al Nequi del jugador. La reserva aparta el horario, pero solo
        // queda confirmada cuando el jugador acepta el pago (ver sincronizarPagos).
        val cobro = pasarela.solicitarCobro(
            nequiOrigen = nequiJugador,
            nequiDestino = nequiDuenio,
            monto = total,
            descripcion = "SportyGo · ${c.nombre} · $fechaTexto $horario" +
                if (compra.isNotEmpty()) " + productos" else ""
        )

        val reserva = Reserva(
            id = UUID.randomUUID().toString(),
            usuarioId = actual.id,
            canchaId = c.id,
            nombreCancha = c.nombre,
            fecha = fechaTexto,
            hora = hora,
            duracionHoras = duracionHoras,
            precio = precioCancha,
            productos = compra,
            estado = EstadoReserva.PENDIENTE_PAGO,
            pagoId = cobro.id,
            nequiPagador = nequiJugador,
            nequiDestino = nequiDuenio,
            equipoId = equipo?.id.orEmpty(),
            nombreEquipo = equipo?.nombre.orEmpty()
        )
        repo.guardarReserva(reserva)

        notificar(
            usuarioId = actual.id,
            tipo = TipoNotificacion.PAGO_PENDIENTE,
            titulo = "Acepta el pago en Nequi · ${formatoPesos(total)}",
            mensaje = "Enviamos una solicitud de pago a tu Nequi ${ocultarNumero(nequiJugador)} " +
                "por tu reserva en ${c.nombre} el $fechaTexto de $horario. " +
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
        val u = usuario ?: return
        var cambio = false
        // Los cobros los responde el jugador desde su teléfono: se revisan sus propias reservas.
        repo.reservasDeUsuario(u.id)
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
        // Se guarda cuándo se pagó: los ingresos del dueño se cuentan desde el día del pago.
        val confirmada = r.copy(
            estado = EstadoReserva.CONFIRMADA,
            pagadaEn = System.currentTimeMillis()
        )
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
                append("Tu reserva en ${r.nombreCancha} el ${r.fecha} de ${r.rangoHoras} está confirmada. ")
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
            mensaje = "$jugador reservó tu cancha el ${r.fecha} de ${r.rangoHoras} y pagó " +
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
                "el ${r.fecha} de ${r.rangoHoras} no se hizo. Ese horario quedó libre."
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
    // Chat jugador-dueño
    // ---------------------------------------------------------------

    /** Mensajes de la conversación [chatId], del más antiguo al más reciente. */
    fun mensajesDeChat(chatId: String): List<Mensaje> = misMensajes.filter { it.chatId == chatId }

    /** Mensajes recibidos (no escritos por mí) sin leer en [chatId]. */
    fun noLeidosEnChat(chatId: String): Int {
        val yo = usuario?.id ?: return 0
        return misMensajes.count { it.chatId == chatId && it.autorId != yo && !it.leido }
    }

    /** Conversaciones del usuario en sesión, la más reciente primero. */
    fun misConversaciones(): List<Conversacion> {
        val yo = usuario?.id ?: return emptyList()
        return misMensajes.groupBy { it.chatId }.map { (chatId, lista) ->
            val ultimo = lista.maxBy { it.enviadoEn }
            Conversacion(
                chatId = chatId,
                canchaId = ultimo.canchaId,
                jugadorId = ultimo.jugadorId,
                duenioId = ultimo.duenioId,
                ultimo = ultimo,
                noLeidos = lista.count { it.autorId != yo && !it.leido }
            )
        }.sortedByDescending { it.ultimo.enviadoEn }
    }

    /** Total de mensajes recibidos sin leer (para el panel del dueño). */
    fun totalMensajesSinLeer(): Int {
        val yo = usuario?.id ?: return 0
        return misMensajes.count { it.autorId != yo && !it.leido }
    }

    /**
     * Envía [texto] en el chat de la cancha [canchaId] con el jugador [jugadorId]. Lo puede
     * escribir ese jugador o el dueño de la cancha. Devuelve un mensaje de error, o null si se envió.
     */
    fun enviarMensaje(canchaId: String, jugadorId: String, texto: String): String? {
        val yo = usuario ?: return "Debes iniciar sesión."
        val c = repo.canchaPorId(canchaId) ?: return "La cancha ya no está disponible."
        if (yo.id != jugadorId && yo.id != c.ownerId) return "No participas en este chat."
        if (jugadorId == c.ownerId) return "No puedes escribirte a ti mismo."
        val limpio = texto.trim()
        if (limpio.isEmpty()) return "Escribe un mensaje."
        if (limpio.length > 1000) return "El mensaje es muy largo (máximo 1000 caracteres)."

        val m = Mensaje(
            id = UUID.randomUUID().toString(),
            chatId = Mensaje.idChat(canchaId, jugadorId),
            canchaId = canchaId,
            jugadorId = jugadorId,
            duenioId = c.ownerId,
            autorId = yo.id,
            texto = limpio,
            enviadoEn = System.currentTimeMillis()
        )
        repo.guardarMensaje(m)
        mensajesAvisados += m.id
        misMensajes = repo.misMensajes().sortedBy { it.enviadoEn }
        return null
    }

    /** Marca como leídos los mensajes que recibí en [chatId]. */
    fun marcarChatLeido(chatId: String) {
        val yo = usuario?.id ?: return
        avisos?.quitar("chat-$chatId")
        val pendientes = misMensajes.filter { it.chatId == chatId && it.autorId != yo && !it.leido }
        if (pendientes.isEmpty()) return
        pendientes.forEach { repo.marcarMensajeLeido(it.id) }
        misMensajes = repo.misMensajes().sortedBy { it.enviadoEn }
    }

    /**
     * Muestra en el teléfono los mensajes que acaban de llegar (no los que ya estaban al
     * iniciar sesión, ni los del chat que está abierto en pantalla).
     */
    private fun avisarMensajesNuevos(yo: String) {
        val recibidos = misMensajes.filter { it.autorId != yo && !it.leido }
        if (!mensajesCargados) {
            mensajesAvisados += recibidos.map { it.id }
            mensajesCargados = true
            return
        }
        recibidos.filter { it.id !in mensajesAvisados }.forEach { m ->
            mensajesAvisados += m.id
            if (m.chatId == chatAbierto) return@forEach
            if (preferencias?.avisosChat == false) return@forEach
            val autor = repo.usuarioPorId(m.autorId)?.nombreUsuario ?: "Alguien"
            val cancha = repo.canchaPorId(m.canchaId)?.nombre.orEmpty()
            avisos?.mostrar(
                Notificacion(
                    // Un solo aviso por chat: el nuevo reemplaza al anterior.
                    id = "chat-${m.chatId}",
                    usuarioId = yo,
                    tipo = TipoNotificacion.MENSAJE_NUEVO,
                    titulo = if (m.autorId == m.duenioId) "$autor · $cancha" else "$autor te escribió",
                    mensaje = m.texto,
                    creadaEn = m.enviadoEn,
                    mostrada = true
                )
            )
        }
    }

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

    /**
     * Guarda el video grabado en "Grabaciones", con el equipo que reservó y, si se eligió,
     * el equipo contrincante ([rivalId]).
     */
    fun guardarGrabacion(reserva: Reserva, videoUri: String, rivalId: String? = null): String? {
        val u = usuario ?: return null
        // El nombre guardado en la reserva sirve si el equipo se borra después.
        val equipo = reserva.equipoId.takeIf { it.isNotBlank() }?.let { repo.equipoPorId(it) }
        val rival = rivalId?.let { repo.equipoPorId(it) }
        val id = UUID.randomUUID().toString()
        val grabacion = Grabacion(
            id = id,
            usuarioId = u.id,
            videoUri = videoUri,
            fechaGuardado = LocalDate.now().format(FORMATO_FECHA),
            horaReserva = reserva.hora,
            nombreCancha = reserva.nombreCancha,
            reservaId = reserva.id,
            equipoId = reserva.equipoId,
            nombreEquipo = equipo?.nombre ?: reserva.nombreEquipo,
            rivalId = rival?.id.orEmpty(),
            nombreRival = rival?.nombre.orEmpty()
        )
        repo.guardarGrabacion(grabacion)
        misGrabaciones = repo.grabacionesDeUsuario(u.id)
        // El video también le llega al dueño de la cancha: se sube a la nube en segundo plano.
        subirVideo(grabacion)
        return id
    }

    /**
     * Sube los videos de las reservas que aún no están en la nube (los grabados antes de
     * esta función o cuya subida se cortó, p. ej. sin red o al cerrar la app).
     */
    private fun subirVideosPendientes() {
        val u = usuario ?: return
        if (u.rol != UserRole.JUGADOR) return
        repo.grabacionesDeUsuario(u.id)
            .filter { it.videoUrl.isBlank() && it.reservaId.isNotBlank() }
            .forEach { subirVideo(it) }
    }

    /**
     * Sube el video de [g] a Cloud Storage y lo asigna al dueño de la cancha de la reserva.
     * Si el video no está en este teléfono, se intentará desde el teléfono que lo grabó.
     */
    private fun subirVideo(g: Grabacion) {
        val archivo = archivoDeVideo(g) ?: return
        val reserva = repo.reservaPorId(g.reservaId) ?: return
        val c = repo.canchaPorId(reserva.canchaId) ?: return
        if (c.ownerId == g.usuarioId || !subiendoVideos.add(g.id)) return

        NubeDatos.subirVideo(archivo, "grabaciones/${g.usuarioId}/${g.id}") { url ->
            subiendoVideos -= g.id
            if (url == null) return@subirVideo
            // Si la borraron (o se cerró la sesión) mientras subía, el video ya no se usa.
            if (usuario?.id != g.usuarioId || repo.grabacionPorId(g.id) == null) {
                NubeDatos.borrarArchivo(url)
                return@subirVideo
            }
            repo.asignarVideo(g.id, c.id, c.ownerId, url)
            misGrabaciones = repo.grabacionesDeUsuario(g.usuarioId)

            val jugador = usuario?.nombreUsuario ?: "Un jugador"
            val actual = repo.grabacionPorId(g.id) ?: g
            notificar(
                usuarioId = c.ownerId,
                tipo = TipoNotificacion.GRABACION_RECIBIDA,
                titulo = "Nuevo video de ${c.nombre}",
                mensaje = buildString {
                    append("$jugador grabó su partido del ${reserva.fecha} de ${reserva.rangoHoras}")
                    actual.enfrentamiento?.let { append(" ($it)") }
                    append(". Míralo en Grabaciones.")
                }
            )
        }
    }

    /** Guarda los momentos con posibles goles encontrados en el audio de la grabación [id]. */
    fun guardarMarcasPosibles(id: String, marcas: List<Long>) {
        val g = grabacionPorId(id) ?: return
        repo.guardarMarcasPosibles(g.id, marcas.sorted())
        misGrabaciones = repo.grabacionesDeUsuario(g.usuarioId)
    }

    /** Guarda los goles confirmados por el usuario (y con ellos el marcador del partido). */
    fun guardarMarcador(id: String, goles: List<Gol>) {
        val g = grabacionPorId(id) ?: return
        repo.guardarMarcador(
            g.id,
            goles.filter { it.equipo == 1 || it.equipo == 2 }.sortedBy { it.tiempoMs }
        )
        misGrabaciones = repo.grabacionesDeUsuario(g.usuarioId)
    }

    // ---------------------------------------------------------------
    // Equipos
    // ---------------------------------------------------------------

    /** Todos los equipos registrados en la app (para elegir el contrincante). */
    fun todosLosEquipos(): List<Equipo> =
        repo.todosLosEquipos().sortedBy { it.nombre.lowercase() }

    fun equipoPorId(id: String): Equipo? = repo.equipoPorId(id)

    fun nombreDeUsuario(id: String): String? = repo.usuarioPorId(id)?.nombreUsuario

    /**
     * Crea un equipo del usuario en sesión con su nombre y la imagen del escudo (opcional).
     * Devuelve un mensaje de error, o null si quedó guardado.
     */
    fun crearEquipo(nombre: String, escudoUri: String): String? {
        val actual = usuario ?: return "Debes iniciar sesión."
        val limpio = nombre.trim().replace(Regex("\\s+"), " ")
        if (limpio.isEmpty()) return "Escribe el nombre del equipo."
        if (limpio.length < 3) return "El nombre debe tener al menos 3 caracteres."
        if (limpio.length > 30) return "El nombre es muy largo (máximo 30 caracteres)."
        // Los nombres son únicos en la app, para distinguir al contrincante al grabar.
        if (repo.todosLosEquipos().any { it.nombre.equals(limpio, ignoreCase = true) }) {
            return "Ya existe un equipo con ese nombre."
        }
        repo.guardarEquipo(
            Equipo(
                id = UUID.randomUUID().toString(),
                creadorId = actual.id,
                nombre = limpio,
                escudoUri = escudoUri,
                creadoEn = System.currentTimeMillis()
            )
        )
        misEquipos = repo.equiposDe(actual.id).sortedBy { it.nombre.lowercase() }
        return null
    }

    /**
     * Cambia el nombre de una grabación propia (vacío = vuelve a mostrar el nombre de la cancha).
     * Devuelve un mensaje de error, o null si se guardó.
     */
    fun renombrarGrabacion(id: String, nombre: String): String? {
        val g = grabacionPorId(id) ?: return "Esa grabación ya no existe."
        val limpio = nombre.trim().replace(Regex("\\s+"), " ")
        if (limpio.length > 60) return "El nombre es muy largo (máximo 60 caracteres)."
        repo.renombrarGrabacion(g.id, limpio)
        misGrabaciones = repo.grabacionesDeUsuario(g.usuarioId)
        return null
    }

    /**
     * Borra la grabación de la lista, su video de este teléfono y el de la nube (así también
     * desaparece de las grabaciones del dueño de la cancha).
     */
    fun eliminarGrabacion(id: String) {
        val g = grabacionPorId(id) ?: return
        repo.eliminarGrabacion(g.id)
        archivoDeVideo(g)?.delete()
        if (g.videoUrl.isNotBlank()) NubeDatos.borrarArchivo(g.videoUrl)
        misGrabaciones = repo.grabacionesDeUsuario(g.usuarioId)
    }

    fun grabacionPorId(id: String): Grabacion? =
        repo.grabacionPorId(id)?.takeIf { it.usuarioId == usuario?.id }

    /** Grabación hecha por un jugador en la cancha del dueño en sesión (solo para verla). */
    fun grabacionDeMiCanchaPorId(id: String): Grabacion? =
        grabacionesDeMiCancha.firstOrNull { it.id == id }

    /** Horas ya reservadas (por cualquier jugador) en una cancha para una fecha. */
    fun horasOcupadas(canchaId: String, fecha: LocalDate): Set<String> {
        val fechaTexto = fecha.format(FORMATO_FECHA)
        // Las pendientes de pago también apartan el horario; las rechazadas lo liberan.
        return repo.reservasDeCancha(canchaId)
            .filter { it.fecha == fechaTexto && it.estado != EstadoReserva.RECHAZADA }
            .flatMap { it.horasOcupadas }
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

        /**
         * Archivo del video de [g] en este teléfono, o null si no está aquí (el archivo queda
         * en el teléfono que grabó; desde otros teléfonos se ve la copia de la nube, [Grabacion.videoUrl]).
         */
        fun archivoDeVideo(g: Grabacion): java.io.File? {
            val ruta = android.net.Uri.parse(g.videoUri).path ?: return null
            return java.io.File(ruta).takeIf { it.isFile }
        }
    }
}
