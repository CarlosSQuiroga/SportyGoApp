package me.juliangarcia.sportygoapp.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException

/**
 * Copia archivos elegidos por el usuario (galería / archivos) al almacenamiento privado de la app.
 * Los permisos de las URIs del selector de fotos son temporales; con la copia local la imagen
 * sigue disponible aunque se cierre la app.
 */
object ArchivosLocales {

    /**
     * Copia [origen] a `filesDir/<carpeta>/<nombre>-<marca de tiempo>` y devuelve su URI
     * `file://`, o null si no se pudo copiar.
     *
     * Con [reemplazar] = true borra las copias anteriores con ese [nombre] (úsalo solo si el
     * resultado se guarda de inmediato, como la foto de perfil). En formularios que el usuario
     * puede abandonar sin guardar, usa false y llama a [limpiar] después de guardar: si no, la
     * imagen que ya estaba guardada se borra aunque el usuario cancele.
     */
    fun copiar(
        context: Context,
        origen: Uri,
        carpeta: String,
        nombre: String,
        reemplazar: Boolean = true
    ): String? {
        return try {
            val dir = File(context.filesDir, carpeta).apply { mkdirs() }
            if (reemplazar) {
                dir.listFiles { f -> f.name.startsWith("$nombre-") }?.forEach { it.delete() }
            }
            // Nombre único para que Coil no muestre la imagen anterior desde su caché.
            val destino = File(dir, "$nombre-${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(origen)?.use { input ->
                destino.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            Uri.fromFile(destino).toString()
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    /** Borra las copias de `filesDir/<carpeta>` que empiezan por [prefijo], menos [conservar]. */
    fun limpiar(context: Context, carpeta: String, prefijo: String, conservar: String) {
        val dir = File(context.filesDir, carpeta)
        dir.listFiles { f -> f.name.startsWith(prefijo) }
            ?.filter { Uri.fromFile(it).toString() != conservar }
            ?.forEach { it.delete() }
    }
}
