package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Definición de paleta de colores para el formulario Datos de la Cancha
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)
private val ErrorRed = Color(0xFFFF6B6B)

@Composable
fun AddFieldScreen(
    modifier: Modifier = Modifier,
    // Valores de los campos del formulario (Stateless)
    // Ruta local de la imagen elegida ("" = sin imagen)
    imagenUri: String = "",
    nombreCancha: String = "",
    descripcion: String = "",
    capacidadJugadores: String = "",
    horaApertura: String = "0:00",
    horaCierre: String = "1:00",
    precioHora: String = "",
    mapaImagenUrl: String = "https://images.unsplash.com/photo-1524661135-423995f22d0b?q=80&w=800&auto=format&fit=crop",
    // Mensaje de validación (null = sin error)
    errorMensaje: String? = null,
    // Callbacks para manejar eventos e ingresar datos en la navegación futura
    onElegirImagenClick: () -> Unit = {},
    onNombreCanchaChange: (String) -> Unit = {},
    onDescripcionChange: (String) -> Unit = {},
    onCapacidadChange: (String) -> Unit = {},
    onHoraAperturaClick: () -> Unit = {},
    onHoraCierreClick: () -> Unit = {},
    onEscanearQrClick: () -> Unit = {},
    onPrecioHoraChange: (String) -> Unit = {},
    onRegistrarCanchaClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Título principal y subtítulo
            Text(
                text = "Datos de la cancha",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Completa los Datos",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Campo: Imagen de la cancha (se elige desde los archivos del teléfono)
            Text(
                text = "Imagen de la cancha:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCardBackground)
                    .clickable { onElegirImagenClick() },
                contentAlignment = Alignment.Center
            ) {
                if (imagenUri.isBlank()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toca para elegir una imagen",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    AsyncImage(
                        model = imagenUri,
                        contentDescription = "Imagen de la cancha",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: Nombre de la cancha
            Text(
                text = "Nombre de la cancha:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            CustomTextField(
                value = nombreCancha,
                onValueChange = onNombreCanchaChange,
                placeholderText = "Ingresa el nombre"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: Descripción
            Text(
                text = "Descripción:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            CustomTextField(
                value = descripcion,
                onValueChange = onDescripcionChange,
                placeholderText = "Ingresa la descripción de la cancha",
                singleLine = false,
                minLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: Capacidad de Jugadores
            Text(
                text = "Capacidad de Jugadores:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            CustomTextField(
                value = capacidadJugadores,
                onValueChange = onCapacidadChange,
                placeholderText = "Ejemplo: 10",
                keyboardType = KeyboardType.Number
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: Hora de Apertura y Cierre
            Text(
                text = "Hora de Apertura y Cierre:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector de Hora de Apertura
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCardBackground)
                            .clickable { onHoraAperturaClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = horaApertura,
                            fontSize = 15.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Apertura",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // Selector de Hora de Cierre
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCardBackground)
                            .clickable { onHoraCierreClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = horaCierre,
                            fontSize = 15.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cierre",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: Escanear QR de la Cámara
            Text(
                text = "Escanear QR de la Cámara:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCardBackground)
                    .clickable { onEscanearQrClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = "Escanear QR de la Cámara",
                    tint = PrimaryGreen,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: Ajusta el Precio/Hora
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ajusta el Precio/Hora:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.width(12.dp))
                CustomTextField(
                    value = precioHora,
                    onValueChange = onPrecioHoraChange,
                    placeholderText = "$ 0",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sección de Mapa de Ubicación con Coil
            AsyncImage(
                model = mapaImagenUrl,
                contentDescription = "Mapa de Ubicación de la Cancha",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMensaje != null) {
                Text(
                    text = errorMensaje,
                    fontSize = 14.sp,
                    color = ErrorRed
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Botón de acción principal: Registrar Cancha
            Button(
                onClick = onRegistrarCanchaClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Registrar Cancha",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Composable de campo de texto personalizado con estilo oscuro
@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholderText,
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkCardBackground,
            unfocusedContainerColor = DarkCardBackground,
            disabledContainerColor = DarkCardBackground,
            focusedBorderColor = PrimaryGreen,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite
        )
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddFieldPreview() {
    AddFieldScreen()
}
