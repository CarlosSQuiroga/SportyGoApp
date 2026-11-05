package me.juliangarcia.sportygoapp.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/** Reglas de validación. Cada función devuelve un mensaje de error, o null si el valor es válido. */
object Validaciones {

    const val MIN_CONTRASENA = 6

    private val REGEX_USUARIO = Regex("^[A-Za-z0-9_.]{3,20}$")
    private val REGEX_CORREO =
        Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
    private val FORMATO_FECHA: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

    fun nombreUsuario(valor: String): String? = when {
        valor.isBlank() -> "Ingresa un nombre de usuario."
        !REGEX_USUARIO.matches(valor) ->
            "El usuario debe tener entre 3 y 20 caracteres: letras, números, punto o guion bajo."
        else -> null
    }

    fun correo(valor: String): String? = when {
        valor.isBlank() -> "Ingresa tu correo."
        !REGEX_CORREO.matches(valor) -> "El correo no tiene un formato válido."
        else -> null
    }

    fun contrasena(valor: String): String? =
        if (valor.length < MIN_CONTRASENA) {
            "La contraseña debe tener al menos $MIN_CONTRASENA caracteres."
        } else {
            null
        }

    fun fechaNacimiento(valor: String): String? {
        val fecha = try {
            LocalDate.parse(valor, FORMATO_FECHA)
        } catch (e: DateTimeParseException) {
            return "Ingresa una fecha válida con el formato DD/MM/AAAA."
        }
        if (fecha.isAfter(LocalDate.now())) return "La fecha de nacimiento no puede ser futura."
        if (fecha.year < 1900) return "Ingresa una fecha de nacimiento válida."
        return null
    }

    /** Deja solo dígitos (máx. 8) y los muestra como DD/MM/AAAA mientras el usuario escribe. */
    fun formatearFechaInput(raw: String): String {
        val d = raw.filter { it.isDigit() }.take(8)
        return when {
            d.length <= 2 -> d
            d.length <= 4 -> d.substring(0, 2) + "/" + d.substring(2)
            else -> d.substring(0, 2) + "/" + d.substring(2, 4) + "/" + d.substring(4)
        }
    }

    /** Convierte "H:mm" en minutos desde medianoche, o null si no es una hora válida. */
    fun minutosDelDia(hora: String): Int? {
        val partes = hora.split(":")
        if (partes.size != 2) return null
        val h = partes[0].toIntOrNull() ?: return null
        val m = partes[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    fun cancha(
        nombre: String,
        capacidad: String,
        apertura: String,
        cierre: String,
        precio: String
    ): String? {
        if (nombre.isBlank()) return "Ingresa el nombre de la cancha."

        val cap = capacidad.toIntOrNull()
        if (cap == null || cap < 2) return "Ingresa una capacidad de jugadores válida (mínimo 2)."

        val minApertura = minutosDelDia(apertura)
        val minCierre = minutosDelDia(cierre)
        if (minApertura == null || minCierre == null) {
            return "Selecciona la hora de apertura y la de cierre."
        }
        if (minApertura == minCierre) return "La hora de apertura y la de cierre no pueden ser iguales."

        val p = precio.toIntOrNull()
        if (p == null || p <= 0) return "Ingresa un precio por hora válido."

        return null
    }
}
