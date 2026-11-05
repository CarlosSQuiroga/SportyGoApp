package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Definición de colores principales
private val DarkBackground = Color(0xFF0B131D)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)
private val ButtonOutlineColor = Color(0xFF1B2938)

@Composable
fun OpenScreen(
    modifier: Modifier = Modifier,
    // Callbacks para la futura navegación
    onIniciarSesionClick: () -> Unit = {},
    onCrearCuentaClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Espaciador superior
            Spacer(modifier = Modifier.weight(1f))

            // Isotipo / Logo de SportyGo (Pin de ubicación con cancha de fútbol)
            SportyGoLogo(
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Nombre de la aplicación: SPORTYGO
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SPORTY",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "GO",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryGreen,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Eslogan / Subtítulo
            Text(
                text = "Reserva · Juega · Repite",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary
            )

            // Espaciador para centrar el contenido principal
            Spacer(modifier = Modifier.weight(1f))

            // Botón "Iniciar sesión"
            Button(
                onClick = onIniciarSesionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Iniciar sesión",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón "Crear cuenta"
            OutlinedButton(
                onClick = onCrearCuentaClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextWhite
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ButtonOutlineColor)
            ) {
                Text(
                    text = "Crear cuenta",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Composable que dibuja el logotipo oficial de SportyGo usando Canvas
@Composable
private fun SportyGoLogo(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(130.dp, 150.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Trayectoria del pin de ubicación
            val pinPath = Path().apply {
                moveTo(width * 0.5f, height * 0.92f)
                cubicTo(
                    width * 0.15f, height * 0.60f,
                    width * 0.10f, height * 0.40f,
                    width * 0.10f, height * 0.32f
                )
                cubicTo(
                    width * 0.10f, height * 0.12f,
                    width * 0.28f, 0f,
                    width * 0.5f, 0f
                )
                cubicTo(
                    width * 0.72f, 0f,
                    width * 0.90f, height * 0.12f,
                    width * 0.90f, height * 0.32f
                )
                cubicTo(
                    width * 0.90f, height * 0.40f,
                    width * 0.85f, height * 0.60f,
                    width * 0.5f, height * 0.92f
                )
                close()
            }

            // Fondo blanco del pin
            drawPath(path = pinPath, color = Color.White)

            // Dibujo de la cancha de fútbol dentro del pin
            val fieldWidth = width * 0.48f
            val fieldHeight = height * 0.26f
            val fieldLeft = (width - fieldWidth) / 2f
            val fieldTop = height * 0.18f

            // Borde exterior de la cancha (negro)
            drawRect(
                color = Color.Black,
                topLeft = Offset(fieldLeft, fieldTop),
                size = Size(fieldWidth, fieldHeight),
                style = Stroke(width = 3.dp.toPx())
            )

            // Línea central
            drawLine(
                color = Color.Black,
                start = Offset(width / 2f, fieldTop),
                end = Offset(width / 2f, fieldTop + fieldHeight),
                strokeWidth = 3.dp.toPx()
            )

            // Círculo central
            drawCircle(
                color = Color.Black,
                radius = 9.dp.toPx(),
                center = Offset(width / 2f, fieldTop + fieldHeight / 2f),
                style = Stroke(width = 3.dp.toPx())
            )

            // Áreas porterías (izquierda y derecha)
            val goalWidth = fieldWidth * 0.22f
            val goalHeight = fieldHeight * 0.6f
            val goalTop = fieldTop + (fieldHeight - goalHeight) / 2f

            drawRect(
                color = Color.Black,
                topLeft = Offset(fieldLeft, goalTop),
                size = Size(goalWidth, goalHeight),
                style = Stroke(width = 3.dp.toPx())
            )

            drawRect(
                color = Color.Black,
                topLeft = Offset(fieldLeft + fieldWidth - goalWidth, goalTop),
                size = Size(goalWidth, goalHeight),
                style = Stroke(width = 3.dp.toPx())
            )

            // Burbuja con 3 puntos en la parte superior derecha
            val dotsBubbleCenter = Offset(width * 0.82f, height * 0.08f)
            drawCircle(color = Color.White, radius = 9.dp.toPx(), center = dotsBubbleCenter)
            drawCircle(color = Color.Black, radius = 1.5.dp.toPx(), center = Offset(dotsBubbleCenter.x - 4.dp.toPx(), dotsBubbleCenter.y))
            drawCircle(color = Color.Black, radius = 1.5.dp.toPx(), center = dotsBubbleCenter)
            drawCircle(color = Color.Black, radius = 1.5.dp.toPx(), center = Offset(dotsBubbleCenter.x + 4.dp.toPx(), dotsBubbleCenter.y))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OpenPreview() {
    OpenScreen()
}
