package me.juliangarcia.sportygoapp

import me.juliangarcia.sportygoapp.data.DetectorGoles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Detección de gritos de gol sobre volúmenes sintéticos (10 cuadros por segundo). */
class DetectorGolesTest {

    private val porSegundo = 10

    /** Partido de [segundos] con ruido normal alrededor de -32 dB. */
    private fun partido(segundos: Int, semilla: Int = 1): Pair<FloatArray, FloatArray> {
        val r = Random(semilla)
        val db = FloatArray(segundos * porSegundo) { -32f + r.nextFloat() * 6f - 3f }
        val voz = FloatArray(db.size) { r.nextFloat() * 0.3f }
        return db to voz
    }

    private fun grito(db: FloatArray, voz: FloatArray, desdeS: Int, duracionS: Float, nivel: Float = -14f) {
        val desde = desdeS * porSegundo
        val hasta = desde + (duracionS * porSegundo).toInt()
        for (i in desde until hasta) {
            db[i] = nivel
            voz[i] = 0.8f
        }
    }

    @Test
    fun detectaLosGritosLargos() {
        val (db, voz) = partido(600)
        grito(db, voz, 120, 2.5f)
        grito(db, voz, 400, 3f)
        val marcas = DetectorGoles.detectar(db, voz)
        assertEquals(listOf(120_000L, 400_000L), marcas)
    }

    @Test
    fun ignoraGolpesCortosYRuidoNormal() {
        val (db, voz) = partido(300)
        grito(db, voz, 50, 0.4f) // un balonazo o un silbato corto
        assertTrue(DetectorGoles.detectar(db, voz).isEmpty())
    }

    @Test
    fun unaCelebracionConVariosGritosEsUnaSolaMarca() {
        val (db, voz) = partido(300)
        grito(db, voz, 100, 2f)
        grito(db, voz, 106, 2f) // segundo grito de la misma celebración
        assertEquals(listOf(100_000L), DetectorGoles.detectar(db, voz))
    }

    @Test
    fun seAdaptaAUnaCanchaMasRuidosa() {
        val (db, voz) = partido(600)
        // Segunda mitad con más ruido de fondo (público llegando): no es un gol.
        for (i in 300 * porSegundo until db.size) db[i] += 8f
        grito(db, voz, 450, 2f, nivel = -6f)
        assertEquals(listOf(450_000L), DetectorGoles.detectar(db, voz))
    }
}
