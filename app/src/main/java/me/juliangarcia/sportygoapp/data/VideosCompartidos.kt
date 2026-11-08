package me.juliangarcia.sportygoapp.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Compartir grabaciones de partidos: el video completo o solo un momento clave recortado.
 * El recorte copia el video y el audio tal cual (sin volver a codificar), así que es rápido
 * y no pierde calidad; empieza en el fotograma clave anterior al inicio elegido.
 */
object VideosCompartidos {

    private const val TAG = "VideosCompartidos"

    /** Abre el menú de Android para enviar [archivo] (WhatsApp, Instagram, Drive...). */
    fun compartir(context: Context, archivo: File, titulo: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
        val enviar = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, titulo)
            putExtra(Intent.EXTRA_TEXT, "$titulo · grabado con SportyGo")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(enviar, "Compartir video").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * Recorta [origen] entre [inicioMs] y [finMs] en un archivo nuevo en la caché (carpeta
     * "clips"). Devuelve el archivo, o null si el video no se pudo recortar.
     */
    @SuppressLint("WrongConstant") // sampleFlags -> flags de MediaCodec (mismos valores)
    suspend fun recortar(context: Context, origen: File, inicioMs: Long, finMs: Long): File? =
        withContext(Dispatchers.IO) {
            val carpeta = File(context.cacheDir, "clips").apply { mkdirs() }
            // Clips de más de un día ya se compartieron: se borran para no llenar el teléfono.
            val ayer = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            carpeta.listFiles()?.filter { it.lastModified() < ayer }?.forEach { it.delete() }
            val destino = File(carpeta, "momento-${System.currentTimeMillis()}.mp4")

            val extractor = MediaExtractor()
            var muxer: MediaMuxer? = null
            try {
                extractor.setDataSource(origen.path)
                val m = MediaMuxer(destino.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                muxer = m

                // Pistas de video y audio del partido.
                val pistas = HashMap<Int, Int>()
                var tamanoMaximo = 1024 * 1024
                for (i in 0 until extractor.trackCount) {
                    val formato = extractor.getTrackFormat(i)
                    val mime = formato.getString(MediaFormat.KEY_MIME) ?: continue
                    if (!mime.startsWith("video/") && !mime.startsWith("audio/")) continue
                    extractor.selectTrack(i)
                    pistas[i] = m.addTrack(formato)
                    if (formato.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        tamanoMaximo = maxOf(tamanoMaximo, formato.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                    }
                }
                if (pistas.isEmpty()) return@withContext null

                // Conserva la orientación del video (vertical u horizontal).
                metadato(origen, MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    ?.toIntOrNull()?.let { m.setOrientationHint(it) }

                val inicioUs = inicioMs * 1000
                val finUs = finMs * 1000
                extractor.seekTo(inicioUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                m.start()

                val buffer = ByteBuffer.allocate(tamanoMaximo)
                val info = MediaCodec.BufferInfo()
                var base = -1L
                var escritas = 0
                while (true) {
                    val tamano = extractor.readSampleData(buffer, 0)
                    if (tamano < 0) break
                    val tiempo = extractor.sampleTime
                    if (tiempo > finUs) break
                    if (base < 0) base = tiempo
                    val pista = pistas[extractor.sampleTrackIndex]
                    if (pista != null && tiempo >= base) {
                        info.set(0, tamano, tiempo - base, extractor.sampleFlags)
                        m.writeSampleData(pista, buffer, info)
                        escritas++
                    }
                    extractor.advance()
                }
                m.stop()
                if (escritas == 0) null else destino
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo recortar el video", e)
                null
            } finally {
                runCatching { muxer?.release() }
                extractor.release()
            }.also { if (it == null) destino.delete() }
        }

    /** Duración del video en milisegundos (0 si no se puede leer). */
    fun duracionMs(archivo: File): Long =
        metadato(archivo, MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

    private fun metadato(archivo: File, clave: Int): String? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(archivo.path)
            r.extractMetadata(clave)
        } catch (e: Exception) {
            null
        } finally {
            runCatching { r.release() }
        }
    }
}
