package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Definición de colores principales
private val DarkBackground = Color(0xFF0B131D)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)

@Composable
fun RegisterFieldScreen(
    modifier: Modifier = Modifier,
    mapaImagenUrl: String = "https://images.unsplash.com/photo-1524661135-423995f22d0b?q=80&w=800&auto=format&fit=crop",
    // Callback para la futura navegación
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

            // Título principal
            Text(
                text = "Registra tu cancha",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtítulo
            Text(
                text = "Elige una ubicación",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Visor de Mapa con Marcador de Selección en el Centro
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Imagen del mapa cargada mediante Coil
                AsyncImage(
                    model = mapaImagenUrl,
                    contentDescription = "Mapa para seleccionar ubicación",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Indicador verde de selección de ubicación en el centro del mapa
                UbicacionTargetMarker(
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón principal de acción: "Registrar ubicación"
            Button(
                onClick = onRegistrarUbicacionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
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
