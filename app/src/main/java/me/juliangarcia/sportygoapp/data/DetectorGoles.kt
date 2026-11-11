package me.juliangarcia.sportygoapp.data

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Busca en el audio de una grabación los momentos que suenan a gol: un "¡GOOOOOL!" gritado
 * (sonido fuerte, sostenido y con tono de voz) o el grito del público (ruido fuerte y largo).
 *
 * Funciona sin internet y sin modelos de voz: mide el volumen del audio cada 100 ms y lo
 * compara con el ruido normal del partido en ese momento (el minuto alrededor). Un tramo que
 * sube mucho por encima de ese nivel durante más de un segundo es una marca de posible gol.
 * El usuario confirma después cuáles fueron gol de verdad.
 */
object DetectorGoles {

    private const val TAG = "DetectorGoles"

    /** Cada cuadro de análisis dura 100 ms. */
    private const val CUADROS_POR_SEGUNDO = 10
    /** Frecuencia a la que se reduce el audio para medir el tono de voz. */
    private const val FRECUENCIA_TONO = 4000

    /** Decibeles por encima del ruido normal del partido para contar como grito. */
    private const val UMBRAL_DB = 10f
    /** Por debajo de este volumen (dBFS) no se considera grito aunque el resto sea silencio. */
    private const val PISO_DB = -42f
    /** Duración mínima del grito (un "gol" corto dura al menos un segundo). */
    private const val MIN_CUADROS = 10
    /** Huecos más cortos que esto (p. ej. tomar aire) no cortan el grito. */
    private const val MAX_HUECO = 3
    /** Gritos más cercanos que esto se consideran la misma celebración. */
    private const val SEPARACION_MS = 15_000L
    /** Máximo de marcas para no abrumar al usuario. */
    private const val MAX_MARCAS = 20

    /**
     * Devuelve los momentos (ms desde el inicio del video) con posibles goles, ordenados,
     * o null si el video no tiene audio o no se pudo leer.
     * [onProgreso] recibe valores de 0 a 1.
     */
    suspend fun buscar(archivo: File, onProgreso: (Float) -> Unit = {}): List<Long>? =
        withContext(Dispatchers.Default) {
            val medicion = try {
                medir(archivo, onProgreso)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo analizar el audio de ${archivo.name}", e)
                null
            } ?: return@withContext null
            onProgreso(1f)
            detectar(medicion.volumenDb, medicion.voz)
        }

    // ---------------------------------------------------------------
    // 1. Decodificar el audio y medir volumen y tono por cuadro
    // ---------------------------------------------------------------

    private class Medicion(val volumenDb: FloatArray, val voz: FloatArray)

    private suspend fun medir(archivo: File, onProgreso: (Float) -> Unit): Medicion? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(archivo.path)
            val pista = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            } ?: return null // grabado sin permiso de micrófono
            val formato = extractor.getTrackFormat(pista)
            val mime = formato.getString(MediaFormat.KEY_MIME) ?: return null
            val duracionUs = if (formato.containsKey(MediaFormat.KEY_DURATION)) {
                formato.getLong(MediaFormat.KEY_DURATION)
            } else 0L
            extractor.selectTrack(pista)

            val c = MediaCodec.createDecoderByType(mime)
            codec = c
            c.configure(formato, null, null, 0)
            c.start()

            var analizador: Analizador? = null
            val info = MediaCodec.BufferInfo()
            var finEntrada = false
            var finSalida = false
            var ultimoProgreso = -1
            val contexto = kotlin.coroutines.coroutineContext

            while (!finSalida) {
                contexto.ensureActive()
                if (!finEntrada) {
                    val i = c.dequeueInputBuffer(10_000)
                    if (i >= 0) {
                        val buffer = c.getInputBuffer(i) ?: continue
                        val leidos = extractor.readSampleData(buffer, 0)
                        if (leidos < 0) {
                            c.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            finEntrada = true
                        } else {
                            c.queueInputBuffer(i, 0, leidos, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val o = c.dequeueOutputBuffer(info, 10_000)
                if (o >= 0) {
                    val salida = c.getOutputBuffer(o)
                    if (salida != null && info.size > 0) {
                        val a = analizador ?: Analizador.desde(c.outputFormat).also { analizador = it }
                        salida.position(info.offset)
                        salida.limit(info.offset + info.size)
                        a.agregar(salida.order(ByteOrder.nativeOrder()))
                    }
                    c.releaseOutputBuffer(o, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) finSalida = true
                    if (duracionUs > 0) {
                        val p = (info.presentationTimeUs * 100 / duracionUs).toInt()
                        if (p != ultimoProgreso) {
                            ultimoProgreso = p
                            onProgreso((p / 100f).coerceIn(0f, 0.99f))
                        }
                    }
                } else if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    analizador = Analizador.desde(c.outputFormat)
                        .also { nuevo -> analizador?.let { nuevo.continuarDe(it) } }
                }
            }
            val a = analizador ?: return null
            return Medicion(a.volumenes.aArreglo(), a.voces.aArreglo())
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    /** Recibe el audio decodificado y produce volumen (dB) y "tono de voz" (0..1) cada 100 ms. */
    private class Analizador(
        frecuencia: Int,
        private val canales: Int,
        private val flotante: Boolean
    ) {
        val volumenes = ListaFloat()
        val voces = ListaFloat()

        private val muestrasPorCuadro = max(1, frecuencia / CUADROS_POR_SEGUNDO)
        private val paso = max(1, frecuencia / FRECUENCIA_TONO)
        private val frecuenciaReducida = frecuencia / paso
        private val reducido = FloatArray(muestrasPorCuadro / paso + 1)
        private var nReducido = 0
        private var sumaPaso = 0f
        private var nPaso = 0
        private var energia = 0.0
        private var nCuadro = 0

        fun continuarDe(anterior: Analizador) {
            volumenes.agregarTodo(anterior.volumenes)
            voces.agregarTodo(anterior.voces)
        }

        fun agregar(buffer: java.nio.ByteBuffer) {
            if (flotante) {
                val f = buffer.asFloatBuffer()
                while (f.remaining() >= canales) {
                    var suma = 0f
                    repeat(canales) { suma += f.get() }
                    muestra(suma / canales)
                }
            } else {
                val s = buffer.asShortBuffer()
                while (s.remaining() >= canales) {
                    var suma = 0f
                    repeat(canales) { suma += s.get() }
                    muestra(suma / canales / 32768f)
                }
            }
        }

        private fun muestra(x: Float) {
            energia += x * x
            sumaPaso += x
            nPaso++
            if (nPaso == paso) {
                if (nReducido < reducido.size) reducido[nReducido++] = sumaPaso / paso
                sumaPaso = 0f
                nPaso = 0
            }
            nCuadro++
            if (nCuadro == muestrasPorCuadro) cerrarCuadro()
        }

        private fun cerrarCuadro() {
            val rms = sqrt(energia / nCuadro).toFloat()
            val db = 20f * log10(rms + 1e-9f)
            volumenes.agregar(db)
            // El tono solo se calcula si hay sonido (ahorra tiempo en los silencios).
            voces.agregar(if (db > PISO_DB) tonoDeVoz(reducido, nReducido, frecuenciaReducida) else 0f)
            energia = 0.0
            nCuadro = 0
            nReducido = 0
        }

        companion object {
            fun desde(formato: MediaFormat): Analizador {
                val frecuencia = formato.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val canales = max(1, formato.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
                val flotante = formato.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                    formato.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                return Analizador(frecuencia, canales, flotante)
            }
        }
    }

    /**
     * Qué tan "cantado" es el sonido (0..1): una voz sostenida como "GOOOOL" tiene un tono
     * claro (autocorrelación alta entre 100 y 800 Hz); el ruido o los golpes, no.
     */
    private fun tonoDeVoz(x: FloatArray, n: Int, frecuencia: Int): Float {
        if (n < 16) return 0f
        var media = 0f
        for (i in 0 until n) media += x[i]
        media /= n
        var r0 = 0f
        for (i in 0 until n) {
            val v = x[i] - media
            x[i] = v
            r0 += v * v
        }
        if (r0 < 1e-9f) return 0f
        val minRetardo = max(1, frecuencia / 800)
        val maxRetardo = min(n / 2, frecuencia / 100)
        var mejor = 0f
        for (retardo in minRetardo..maxRetardo) {
            var r = 0f
            for (i in 0 until n - retardo) r += x[i] * x[i + retardo]
            // Se compensa que con más retardo hay menos muestras sumadas.
            val normalizado = r / r0 * n / (n - retardo)
            if (normalizado > mejor) mejor = normalizado
        }
        return mejor.coerceIn(0f, 1f)
    }

    // ---------------------------------------------------------------
    // 2. Encontrar los gritos
    // ---------------------------------------------------------------

    private class Grito(val inicio: Int, val fin: Int, val puntaje: Float)

    internal fun detectar(db: FloatArray, voz: FloatArray): List<Long> {
        val n = db.size
        if (n < MIN_CUADROS) return emptyList()
        val base = ruidoNormal(db)

        val alto = BooleanArray(n) { i ->
            db[i] > PISO_DB && db[i] > base[i] + UMBRAL_DB
        }

        // Tramos altos seguidos (tolerando huecos cortos para tomar aire).
        val gritos = mutableListOf<Grito>()
        var i = 0
        while (i < n) {
            if (!alto[i]) { i++; continue }
            val inicio = i
            var fin = i
            var hueco = 0
            var j = i + 1
            while (j < n) {
                if (alto[j]) { fin = j; hueco = 0 } else if (++hueco > MAX_HUECO) break
                j++
            }
            val largo = fin - inicio + 1
            if (largo >= MIN_CUADROS) {
                var exceso = 0f
                var cuadrosConVoz = 0
                for (k in inicio..fin) {
                    exceso += max(0f, db[k] - base[k] - UMBRAL_DB)
                    if (voz[k] > 0.5f) cuadrosConVoz++
                }
                val segundos = largo.toFloat() / CUADROS_POR_SEGUNDO
                val excesoMedio = exceso / largo
                val fraccionVoz = cuadrosConVoz.toFloat() / largo
                // Más largo, más fuerte y más "cantado" = más probable que sea un gol.
                val puntaje = segundos * (1f + excesoMedio / 10f) * (1f + 0.6f * fraccionVoz)
                gritos += Grito(inicio, fin, puntaje)
            }
            i = fin + 1
        }

        // Una misma celebración puede tener varios gritos: se unen en una sola marca.
        val separacion = (SEPARACION_MS * CUADROS_POR_SEGUNDO / 1000).toInt()
        val celebraciones = mutableListOf<Grito>()
        for (g in gritos) {
            val ultima = celebraciones.lastOrNull()
            if (ultima != null && g.inicio - ultima.fin <= separacion) {
                celebraciones[celebraciones.lastIndex] =
                    Grito(ultima.inicio, g.fin, max(ultima.puntaje, g.puntaje))
            } else {
                celebraciones += g
            }
        }

        return celebraciones
            .sortedByDescending { it.puntaje }
            .take(MAX_MARCAS)
            .map { it.inicio.toLong() * 1000 / CUADROS_POR_SEGUNDO }
            .sorted()
    }

    /**
     * Ruido normal del partido en cada cuadro: mediana del volumen del minuto alrededor.
     * Se adapta a que la cancha sea más ruidosa en unos momentos que en otros.
     */
    private fun ruidoNormal(db: FloatArray): FloatArray {
        val porSegundo = CUADROS_POR_SEGUNDO
        val segundos = (db.size + porSegundo - 1) / porSegundo
        val volumenSegundo = FloatArray(segundos) { s ->
            val desde = s * porSegundo
            val hasta = min(db.size, desde + porSegundo)
            var suma = 0f
            for (k in desde until hasta) suma += db[k]
            suma / (hasta - desde)
        }
        val ventana = 30 // segundos a cada lado
        val medianaSegundo = FloatArray(segundos) { s ->
            val desde = max(0, s - ventana)
            val hasta = min(segundos, s + ventana + 1)
            val valores = volumenSegundo.copyOfRange(desde, hasta)
            valores.sort()
            valores[valores.size / 2]
        }
        return FloatArray(db.size) { k -> medianaSegundo[k / porSegundo] }
    }

    /** Lista de Float sin cajas (un partido de una hora son 36 000 cuadros). */
    private class ListaFloat {
        private var datos = FloatArray(1024)
        var tamano = 0
            private set

        fun agregar(v: Float) {
            if (tamano == datos.size) datos = datos.copyOf(datos.size * 2)
            datos[tamano++] = v
        }

        fun agregarTodo(otra: ListaFloat) {
            for (k in 0 until otra.tamano) agregar(otra.datos[k])
        }

        fun aArreglo(): FloatArray = datos.copyOf(tamano)
    }
}
