package me.juliangarcia.sportygoapp.data

/** Tipo de uso que el usuario elige después de crear su cuenta. */
enum class UserRole { JUGADOR, DUENIO }

/**
 * Cuenta guardada en el dispositivo.
 * La contraseña NUNCA se guarda en texto plano: solo su hash (PBKDF2) y el salt.
 * [rol] es null hasta que el usuario elige "Soy jugador" o "Soy dueño de cancha".
 * Una cuenta nueva empieza sin foto de perfil ([fotoPerfilUri] = null).
 */
data class User(
    val id: String,
    val nombreUsuario: String,
    val correo: String,
    val fechaNacimiento: String,
    val passwordHash: String,
    val salt: String,
    val rol: UserRole? = null,
    /** Ruta local (file://) de la foto de perfil elegida desde los archivos del teléfono. */
    val fotoPerfilUri: String? = null
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
    val precioHora: Int = 0
)

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
    val nombreCancha: String = ""
)

/** Reserva que un jugador hace en una cancha. */
data class Reserva(
    val id: String = "",
    val usuarioId: String = "",
    val canchaId: String = "",
    val nombreCancha: String = "",
    /** Fecha de la reserva, formato "dd/MM/yyyy". */
    val fecha: String = "",
    /** Hora de inicio, formato "H:mm". Cada reserva dura 1 hora. */
    val hora: String = "",
    val precio: Int = 0
)
