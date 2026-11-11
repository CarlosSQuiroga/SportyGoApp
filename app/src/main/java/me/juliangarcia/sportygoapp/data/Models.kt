package me.juliangarcia.sportygoapp.data

/** Tipo de uso que el usuario elige después de crear su cuenta. */
enum class UserRole { JUGADOR, DUENIO }

/**
 * Perfil de una cuenta (documento usuarios/{id} en Firestore; [id] es el uid de Firebase Auth).
 * La contraseña no se guarda aquí: la maneja Firebase Authentication.
 * [rol] es null hasta que el usuario elige "Soy jugador" o "Soy dueño de cancha".
 * Una cuenta nueva empieza sin foto de perfil ([fotoPerfilUri] = null).
 */
data class User(
    val id: String,
    val nombreUsuario: String,
    val correo: String,
    val fechaNacimiento: String,
    val rol: UserRole? = null,
    /** URL de la foto de perfil (Cloud Storage), o ruta local mientras se sube. */
    val fotoPerfilUri: String? = null,
    /**
     * Número de celular Nequi (10 dígitos, empieza por 3). Jugador: desde aquí paga.
     * Dueño: aquí recibe los pagos; sin número, su cancha no aparece activa. null = sin registrar.
     */
    val nequi: String? = null
)

/**
 * Cancha registrada por un dueño. Por ahora cada dueño tiene una.
 * Todos los campos tienen valor por defecto: `Cancha()` es una cancha "vacía" lista para llenar.
 */
data class Cancha(
    val id: String = "",
    val ownerId: String = "",
    /** Ruta local (file://) de la imagen de la cancha. Vacío = sin imagen. */
    val imagenUri: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val capacidad: Int = 0,
    /** Formato "H:mm" (24 h). */
    val horaApertura: String = "",
    /** Formato "H:mm" (24 h). */
    val horaCierre: String = "",
    /** Precio por hora, en pesos. */
    val precioHora: Int = 0,
    /** Ubicación elegida por el dueño en el mapa (null = canchas antiguas sin ubicación). */
    val latitud: Double? = null,
    val longitud: Double? = null,
    /** Dirección aproximada obtenida del mapa (ej. "Cra. 43A #1-50, Medellín"). Vacío = desconocida. */
    val direccion: String = "",
    /** Dónde se reclaman los productos comprados (ej. "Junto a la entrada"). Vacío = sin indicar. */
    val kiosco: String = ""
)

enum class CategoriaProducto(val etiqueta: String) {
    BEBIDA("Bebidas"),
    SNACK("Snacks")
}

/**
 * Producto del kiosco de una cancha. Lo crea el dueño y lo compran los jugadores al reservar.
 * Se muestra con [imagenUri] (foto de galería o cámara) o, si está vacía, con el ícono [iconoId].
 */
data class Producto(
    val id: String = "",
    val canchaId: String = "",
    val nombre: String = "",
    /** Precio por unidad, en pesos. */
    val precio: Int = 0,
    val categoria: CategoriaProducto = CategoriaProducto.BEBIDA,
    /** Ruta local (file://) de la foto del producto. Vacío = usa el ícono. */
    val imagenUri: String = "",
    /** Id de un ícono predefinido de la app (ver IconosProducto). */
    val iconoId: String = ""
)

/**
 * Producto comprado junto con una reserva. Copia nombre y precio del momento de la compra,
 * así el pedido no cambia si luego el dueño edita o borra el producto.
 */
data class ItemCompra(
    val productoId: String = "",
    val nombre: String = "",
    val precioUnitario: Int = 0,
    val cantidad: Int = 0
) {
    val subtotal: Int get() = precioUnitario * cantidad
}

/**
 * Grabación de un partido. `Grabacion()` es una grabación "vacía" lista para llenar.
 */
data class Grabacion(
    val id: String = "",
    /** Usuario dueño de la grabación. */
    val usuarioId: String = "",
    /** Ruta/URI del archivo de video. Vacío = sin video. */
    val videoUri: String = "",
    /** Fecha en que se guardó la grabación, formato "dd/MM/yyyy". */
    val fechaGuardado: String = "",
    /** Hora de la reserva en la que se grabó, formato "H:mm". */
    val horaReserva: String = "",
    val nombreCancha: String = "",
    /** Reserva en la que se grabó (vacío = grabaciones antiguas sin reserva asociada). */
    val reservaId: String = "",
    /** Equipo que hizo la reserva (vacío = sin equipo). El nombre se copia al grabar. */
    val equipoId: String = "",
    val nombreEquipo: String = "",
    /** Equipo contrincante elegido al grabar (vacío = sin rival registrado). */
    val rivalId: String = "",
    val nombreRival: String = "",
    /** Nombre que le puso el usuario (vacío = se muestra el nombre de la cancha). */
    val nombre: String = "",
    /** true cuando ya se buscaron posibles goles en el audio (aunque no se encontrara ninguno). */
    val audioAnalizado: Boolean = false,
    /** Momentos (ms desde el inicio del video) con gritos de gol detectados en el audio. */
    val marcasPosibles: List<Long> = emptyList(),
    /** Goles que el usuario confirmó, en orden de tiempo. */
    val goles: List<Gol> = emptyList(),
    /** true cuando el usuario revisó las marcas y guardó el marcador. */
    val marcadorConfirmado: Boolean = false
) {
    /** Nombre que se muestra en la lista y al compartir. */
    val titulo: String get() = nombre.ifBlank { nombreCancha.ifBlank { "Partido" } }

    /** Equipo 1 = el que reservó; equipo 2 = el contrincante. */
    val nombreEquipo1: String get() = nombreEquipo.ifBlank { "Equipo 1" }
    val nombreEquipo2: String get() = nombreRival.ifBlank { "Equipo 2" }

    val golesEquipo1: Int get() = goles.count { it.equipo == 1 }
    val golesEquipo2: Int get() = goles.count { it.equipo == 2 }

    /** "Los Halcones 2 – 1 Real Barrio", o null si el marcador no se ha confirmado. */
    val marcador: String?
        get() = if (!marcadorConfirmado) null
        else "$nombreEquipo1 $golesEquipo1 – $golesEquipo2 $nombreEquipo2"

    /** "Los Halcones vs Real Barrio", o null si la grabación no tiene equipos. */
    val enfrentamiento: String?
        get() = when {
            nombreEquipo.isBlank() && nombreRival.isBlank() -> null
            nombreRival.isBlank() -> nombreEquipo
            else -> "${nombreEquipo.ifBlank { "Sin equipo" }} vs $nombreRival"
        }
}

/** Gol confirmado en una grabación: momento del video y equipo que lo marcó (1 o 2). */
data class Gol(
    /** Milisegundos desde el inicio del video. */
    val tiempoMs: Long = 0L,
    /** 1 = equipo que reservó, 2 = contrincante. */
    val equipo: Int = 1
)

/**
 * Equipo creado por un usuario. Se puede elegir al reservar (el equipo que reserva)
 * y al grabar el partido (como equipo contrincante).
 */
data class Equipo(
    val id: String = "",
    /** Usuario que creó el equipo. */
    val creadorId: String = "",
    val nombre: String = "",
    /** Ruta local (file://) de la imagen del escudo. Vacío = escudo con la inicial. */
    val escudoUri: String = "",
    /** Momento de creación (epoch millis). */
    val creadoEn: Long = 0L
)

/**
 * Una reserva aparta el horario apenas se crea, pero solo queda confirmada cuando el jugador
 * acepta el cobro en Nequi. Si lo rechaza, el horario vuelve a quedar libre.
 */
enum class EstadoReserva { PENDIENTE_PAGO, CONFIRMADA, RECHAZADA }

enum class TipoNotificacion {
    CUENTA_CREADA, VINCULAR_NEQUI, PAGO_PENDIENTE, PAGO_RECHAZADO,
    RESERVA_HECHA, RESERVA_RECIBIDA, PEDIDO_RECIBIDO, GRABAR_PARTIDO,
    /** Mensaje nuevo en el chat (solo sale en el teléfono, no se guarda en la bandeja). */
    MENSAJE_NUEVO
}

/**
 * Notificación guardada en la bandeja del usuario [usuarioId] (pantalla "Notificaciones").
 * [mostrada] indica si ya salió como notificación del sistema en el teléfono: las que llegan
 * a un usuario sin sesión iniciada se muestran cuando ese usuario entra a la app.
 */
data class Notificacion(
    val id: String = "",
    val usuarioId: String = "",
    val tipo: TipoNotificacion = TipoNotificacion.CUENTA_CREADA,
    val titulo: String = "",
    val mensaje: String = "",
    /** Momento de creación (epoch millis). */
    val creadaEn: Long = 0L,
    val leida: Boolean = false,
    val mostrada: Boolean = false
)

/** Reserva que un jugador hace en una cancha. */
data class Reserva(
    val id: String = "",
    val usuarioId: String = "",
    val canchaId: String = "",
    val nombreCancha: String = "",
    /** Fecha de la reserva, formato "dd/MM/yyyy". */
    val fecha: String = "",
    /** Hora de inicio, formato "H:mm". */
    val hora: String = "",
    /** Horas seguidas reservadas desde [hora] (las reservas antiguas duran 1 hora). */
    val duracionHoras: Int = 1,
    /** Precio de la cancha por todas las horas reservadas. */
    val precio: Int = 0,
    /** Productos comprados para reclamar en el kiosco de la cancha (vacío = no compró). */
    val productos: List<ItemCompra> = emptyList(),
    /** Las reservas antiguas (antes de los pagos) se consideran confirmadas. */
    val estado: EstadoReserva = EstadoReserva.CONFIRMADA,
    /** Id de la solicitud de cobro en la pasarela de pagos (vacío = sin cobro). */
    val pagoId: String = "",
    /** Nequi del jugador que paga y del dueño que recibe. */
    val nequiPagador: String = "",
    val nequiDestino: String = "",
    /** Equipo que hizo la reserva (opcional). El nombre se copia al reservar. */
    val equipoId: String = "",
    val nombreEquipo: String = "",
    /** El anfitrión eligió "Ahora no": no se vuelve a abrir la cámara para esta reserva. */
    val sinGrabar: Boolean = false,
    /** Momento en que se aceptó el pago (epoch millis). 0 = sin pagar o reserva antigua. */
    val pagadaEn: Long = 0L
) {
    val totalProductos: Int get() = productos.sumOf { it.subtotal }
    val total: Int get() = precio + totalProductos

    /** Horas de inicio que ocupa la reserva ("9:00", "10:00"...). */
    val horasOcupadas: List<String> get() = Validaciones.horasSeguidas(hora, duracionHoras)

    /** Hora en que termina, formato "H:mm" (ej. "11:00"). */
    val horaFin: String get() = Validaciones.sumarHoras(hora, duracionHoras)

    /** Ej. "9:00 – 11:00 (2 horas)". */
    val rangoHoras: String
        get() = "$hora – $horaFin (${if (duracionHoras == 1) "1 hora" else "$duracionHoras horas"})"
}

/**
 * Mensaje del chat entre un jugador y el dueño de una cancha. Cada conversación ([chatId])
 * es la de un jugador con una cancha: "<canchaId>_<jugadorId>".
 */
data class Mensaje(
    val id: String = "",
    val chatId: String = "",
    val canchaId: String = "",
    val jugadorId: String = "",
    val duenioId: String = "",
    /** Quién lo escribió (el jugador o el dueño). */
    val autorId: String = "",
    val texto: String = "",
    /** Momento de envío (epoch millis). */
    val enviadoEn: Long = 0L,
    /** true cuando el destinatario ya lo vio. */
    val leido: Boolean = false
) {
    companion object {
        fun idChat(canchaId: String, jugadorId: String) = "${canchaId}_$jugadorId"
    }
}

/** Resumen de una conversación para la lista de chats. */
data class Conversacion(
    val chatId: String,
    val canchaId: String,
    val jugadorId: String,
    val duenioId: String,
    val ultimo: Mensaje,
    /** Mensajes recibidos que el usuario en sesión aún no ha leído. */
    val noLeidos: Int
)
