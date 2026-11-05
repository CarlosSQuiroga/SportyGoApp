package me.juliangarcia.sportygoapp.data

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed interface AuthResult {
    data class Exito(val usuario: User) : AuthResult
    data class Error(val mensaje: String) : AuthResult
}

/** Hash de contraseñas con PBKDF2 + salt aleatorio (disponible en Android 8+; minSdk = 28). */
internal object PasswordHasher {
    private const val ITERACIONES = 60_000
    private const val LONGITUD_BITS = 256

    fun nuevoSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(contrasena: String, salt: String): String {
        val spec = PBEKeySpec(
            contrasena.toCharArray(),
            Base64.decode(salt, Base64.NO_WRAP),
            ITERACIONES,
            LONGITUD_BITS
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return Base64.encodeToString(factory.generateSecret(spec).encoded, Base64.NO_WRAP)
    }

    fun iguales(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
}

class AuthRepository(private val storage: AccountStorage) {

    fun usuarioEnSesion(): User? {
        val id = storage.getSessionUserId() ?: return null
        return storage.loadUsers().firstOrNull { it.id == id }
    }

    /** Crea la cuenta y deja la sesión iniciada (el siguiente paso es elegir el perfil). */
    fun registrar(
        nombreUsuario: String,
        correo: String,
        contrasena: String,
        fechaNacimiento: String
    ): AuthResult {
        val nombre = nombreUsuario.trim()
        val email = correo.trim()
        val fecha = fechaNacimiento.trim()

        Validaciones.nombreUsuario(nombre)?.let { return AuthResult.Error(it) }
        Validaciones.correo(email)?.let { return AuthResult.Error(it) }
        Validaciones.contrasena(contrasena)?.let { return AuthResult.Error(it) }
        Validaciones.fechaNacimiento(fecha)?.let { return AuthResult.Error(it) }

        val usuarios = storage.loadUsers()
        if (usuarios.any { it.nombreUsuario.equals(nombre, ignoreCase = true) }) {
            return AuthResult.Error("Ese nombre de usuario ya está en uso.")
        }
        if (usuarios.any { it.correo.equals(email, ignoreCase = true) }) {
            return AuthResult.Error("Ya existe una cuenta con ese correo.")
        }

        val salt = PasswordHasher.nuevoSalt()
        val nuevo = User(
            id = UUID.randomUUID().toString(),
            nombreUsuario = nombre,
            correo = email,
            fechaNacimiento = fecha,
            passwordHash = PasswordHasher.hash(contrasena, salt),
            salt = salt
        )
        storage.saveUsers(usuarios + nuevo)
        storage.setSessionUserId(nuevo.id)
        return AuthResult.Exito(nuevo)
    }

    /** Solo permite entrar con una cuenta que ya fue creada. */
    fun iniciarSesion(identificador: String, contrasena: String): AuthResult {
        val ident = identificador.trim()
        if (ident.isEmpty() || contrasena.isEmpty()) {
            return AuthResult.Error("Completa todos los campos.")
        }

        val usuario = storage.loadUsers().firstOrNull {
            it.nombreUsuario.equals(ident, ignoreCase = true) ||
                it.correo.equals(ident, ignoreCase = true)
        } ?: return AuthResult.Error(
            "No existe una cuenta con ese usuario o correo. Crea una cuenta primero."
        )

        val hash = PasswordHasher.hash(contrasena, usuario.salt)
        if (!PasswordHasher.iguales(hash, usuario.passwordHash)) {
            return AuthResult.Error("La contraseña es incorrecta.")
        }

        storage.setSessionUserId(usuario.id)
        return AuthResult.Exito(usuario)
    }

    fun cerrarSesion() = storage.setSessionUserId(null)

    fun elegirRol(usuarioId: String, rol: UserRole): User? {
        val usuarios = storage.loadUsers().toMutableList()
        val indice = usuarios.indexOfFirst { it.id == usuarioId }
        if (indice < 0) return null
        val actualizado = usuarios[indice].copy(rol = rol)
        usuarios[indice] = actualizado
        storage.saveUsers(usuarios)
        return actualizado
    }

    fun actualizarFotoPerfil(usuarioId: String, fotoUri: String?): User? {
        val usuarios = storage.loadUsers().toMutableList()
        val indice = usuarios.indexOfFirst { it.id == usuarioId }
        if (indice < 0) return null
        val actualizado = usuarios[indice].copy(fotoPerfilUri = fotoUri)
        usuarios[indice] = actualizado
        storage.saveUsers(usuarios)
        return actualizado
    }

    // ---------- Canchas ----------

    fun canchaDe(ownerId: String): Cancha? =
        storage.loadCanchas().firstOrNull { it.ownerId == ownerId }

    fun canchaPorId(id: String): Cancha? =
        storage.loadCanchas().firstOrNull { it.id == id }

    /** Todas las canchas registradas por los dueños (las que ve el jugador para reservar). */
    fun todasLasCanchas(): List<Cancha> = storage.loadCanchas()

    fun guardarCancha(cancha: Cancha) {
        val otras = storage.loadCanchas().filter { it.ownerId != cancha.ownerId }
        storage.saveCanchas(otras + cancha)
    }

    // ---------- Reservas ----------

    fun reservasDeUsuario(usuarioId: String): List<Reserva> =
        storage.loadReservas().filter { it.usuarioId == usuarioId }

    fun reservasDeCancha(canchaId: String): List<Reserva> =
        storage.loadReservas().filter { it.canchaId == canchaId }

    fun guardarReserva(reserva: Reserva) {
        storage.saveReservas(storage.loadReservas() + reserva)
    }

    // ---------- Grabaciones ----------

    fun grabacionesDeUsuario(usuarioId: String): List<Grabacion> =
        storage.loadGrabaciones().filter { it.usuarioId == usuarioId }

    fun guardarGrabacion(grabacion: Grabacion) {
        storage.saveGrabaciones(storage.loadGrabaciones() + grabacion)
    }
}
