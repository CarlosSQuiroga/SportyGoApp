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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)
private val ErrorRed = Color(0xFFFF6B6B)

/** Pantalla "Crear equipo": nombre del equipo e imagen del escudo. */
@Composable
fun CreateTeamScreen(
    modifier: Modifier = Modifier,
    nombre: String = "",
    /** Ruta local (file://) del escudo elegido. Vacío = escudo con la inicial. */
    escudoUri: String = "",
    errorMensaje: String? = null,
    onBackClick: () -> Unit = {},
    onNombreChange: (String) -> Unit = {},
    onElegirEscudoClick: () -> Unit = {},
    onQuitarEscudoClick: () -> Unit = {},
    onCrearClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Nuevo equipo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Vista previa del escudo (toca para elegir la imagen)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(DarkCardBackground)
                        .clickable { onElegirEscudoClick() }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EscudoDeEquipo(
                        nombre = nombre.ifBlank { "?" },
                        escudoUri = escudoUri,
                        alto = 140.dp
                    )
                    if (escudoUri.isBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddPhotoAlternate,
                                contentDescription = null,
                                tint = DarkBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onElegirEscudoClick) {
                        Text(
                            text = if (escudoUri.isBlank()) "Elegir imagen del escudo" else "Cambiar imagen",
                            color = PrimaryGreen
                        )
                    }
                    if (escudoUri.isNotBlank()) {
                        TextButton(onClick = onQuitarEscudoClick) {
                            Text(text = "Quitar", color = TextSecondary)
                        }
                    }
                }

                Text(
                    text = "Si no eliges una imagen, usaremos un escudo con la inicial del equipo.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Nombre del equipo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextWhite,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nombre,
                    onValueChange = onNombreChange,
                    placeholder = { Text("Ej. Los Halcones", color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Text("${nombre.length}/30", color = TextSecondary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkCardBackground,
                        unfocusedContainerColor = DarkCardBackground,
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = PrimaryGreen
                    )
                )
            }

            if (errorMensaje != null) {
                Text(
                    text = errorMensaje,
                    fontSize = 14.sp,
                    color = ErrorRed,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = onCrearClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Crear equipo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CreateTeamPreview() {
    CreateTeamScreen(nombre = "Los Halcones")
}
