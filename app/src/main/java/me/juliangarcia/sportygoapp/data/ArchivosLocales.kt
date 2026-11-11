package me.juliangarcia.sportygoapp.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException

object ArchivosLocales {

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
