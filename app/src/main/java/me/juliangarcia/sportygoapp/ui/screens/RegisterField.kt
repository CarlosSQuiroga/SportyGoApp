package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Definición de colores principales
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)

/**
 * Paso 1 del dueño: elegir dónde está la cancha. El usuario mueve el mapa y el punto
 * elegido es el que queda bajo el indicador verde del centro.
 */
@Composable
fun RegisterFieldScreen(
    modifier: Modifier = Modifier,
    /** Dirección del punto elegido (o un texto de ayuda mientras se busca). */
    direccionTexto: String = "Mueve el mapa hasta tu cancha",
    /** false mientras aún no hay un punto elegido. */
    puedeConfirmar: Boolean = true,
    /** El mapa real (MapaSeleccion); en el Preview es solo un fondo. */
    mapa: @Composable (Modifier) -> Unit = { m -> Box(m.background(DarkCardBackground)) },
    onMiUbicacionClick: () -> Unit = {},
    onRegistrarUbicacionClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Registra tu cancha",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Mueve el mapa y deja el indicador sobre tu cancha",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Mapa con el indicador de selección fijo en el centro
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                mapa(Modifier.fillMaxSize())

                UbicacionTargetMarker(modifier = Modifier.size(60.dp))

                IconButton(
                    onClick = onMiUbicacionClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(48.dp)
                        .background(DarkBackground, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = "Ir a mi ubicación",
                        tint = PrimaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dirección del punto elegido
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCardBackground)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = direccionTexto, fontSize = 14.sp, color = TextWhite)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRegistrarUbicacionClick,
                enabled = puedeConfirmar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    disabledContainerColor = PrimaryGreen.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Registrar ubicación",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Composable que dibuja el objetivo verde de ubicación con anillos concéntricos
@Composable
private fun UbicacionTargetMarker(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)

        // Anillo exterior transparente verde
        drawCircle(
            color = PrimaryGreen.copy(alpha = 0.3f),
            radius = size.width / 2f,
            center = center
        )

        // Anillo verde medio
        drawCircle(
            color = PrimaryGreen,
            radius = size.width / 2.8f,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )

        // Punto verde central
        drawCircle(
            color = PrimaryGreen,
            radius = size.width / 8f,
            center = center
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterFieldPreview() {
    RegisterFieldScreen()
}
