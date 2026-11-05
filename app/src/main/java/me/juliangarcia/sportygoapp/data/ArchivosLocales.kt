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
     * Copia [origen] a `filesDir/<carpeta>/<nombre>` (reemplazando el archivo anterior) y devuelve
     * su URI `file://`, o null si no se pudo copiar.
     */
    fun copiar(context: Context, origen: Uri, carpeta: String, nombre: String): String? {
        return try {
            val dir = File(context.filesDir, carpeta).apply { mkdirs() }
            // Nombre único para que Coil no muestre la imagen anterior desde su caché.
            dir.listFiles { f -> f.name.startsWith("$nombre-") }?.forEach { it.delete() }
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
}
