package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
 * Paso 1 del dueño: elegir dónde está la cancha. El usuario escribe la dirección (y elige una
 * sugerencia) o mueve el mapa; el punto elegido es el que queda bajo el indicador verde del centro.
 */
@Composable
fun RegisterFieldScreen(
    modifier: Modifier = Modifier,
    /** Dirección del punto elegido (o un texto de ayuda mientras se busca). */
    direccionTexto: String = "Mueve el mapa hasta tu cancha",
    /** false mientras aún no hay un punto elegido. */
    puedeConfirmar: Boolean = true,
    /** Texto del buscador de direcciones. */
    busqueda: String = "",
    onBusquedaChange: (String) -> Unit = {},
    /** Direcciones encontradas para lo escrito en el buscador. */
    sugerencias: List<String> = emptyList(),
    /** Mensaje del buscador ("Buscando...", "No encontramos..."), o null. */
    estadoBusqueda: String? = null,
    /** Se toca la sugerencia en la posición dada: el mapa se mueve ahí. */
    onSugerenciaClick: (Int) -> Unit = {},
    /** Se pulsa "Buscar" en el teclado: va al primer resultado. */
    onBuscar: () -> Unit = {},
    /** El mapa real (MapaSeleccion); en el Preview es solo un fondo. */
    mapa: @Composable (Modifier) -> Unit = { m -> Box(m.background(DarkCardBackground)) },
    onMiUbicacionClick: () -> Unit = {},
    onRegistrarUbicacionClick: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current

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
                text = "Escribe la dirección o mueve el mapa hasta tu cancha",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Buscador de direcciones
            OutlinedTextField(
                value = busqueda,
                onValueChange = onBusquedaChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ej. Calle 10 #43-20, Medellín", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary)
                },
                trailingIcon = if (busqueda.isNotEmpty()) {
                    {
                        IconButton(onClick = { onBusquedaChange("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Borrar", tint = TextSecondary)
                        }
                    }
                } else null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = {
                    if (sugerencias.isNotEmpty()) {
                        onBuscar()
                        focusManager.clearFocus()
                    }
                }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = DarkCardBackground,
                    unfocusedContainerColor = DarkCardBackground,
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = DarkCardBackground,
                    cursorColor = PrimaryGreen
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

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

                // Sugerencias del buscador, encima del mapa
                if (sugerencias.isNotEmpty() || estadoBusqueda != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCardBackground)
                    ) {
                        if (estadoBusqueda != null) {
                            Text(
                                text = estadoBusqueda,
                                fontSize = 14.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                        sugerencias.forEachIndexed { i, sugerencia ->
                            if (i > 0) HorizontalDivider(color = DarkBackground)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        focusManager.clearFocus()
                                        onSugerenciaClick(i)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = sugerencia,
                                    fontSize = 14.sp,
                                    color = TextWhite,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
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
    RegisterFieldScreen(
        busqueda = "Calle 10",
        sugerencias = listOf("Calle 10 #43-20, El Poblado, Medellín", "Calle 10, Bogotá")
    )
}
