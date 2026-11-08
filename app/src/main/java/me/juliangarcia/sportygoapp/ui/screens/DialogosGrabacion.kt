package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DialogBackground = Color(0xFF111A24)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)
private val ErrorRed = Color(0xFFFF6B6B)

/** Diálogo para cambiar el nombre de una grabación. Vacío = vuelve al nombre de la cancha. */
@Composable
fun RenombrarGrabacionDialog(
    nombreActual: String,
    error: String? = null,
    onGuardar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var texto by remember { mutableStateOf(nombreActual) }
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = DialogBackground,
        title = { Text("Renombrar video", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.take(60) },
                    singleLine = true,
                    placeholder = { Text("Ej. Final contra Real Barrio", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = TextSecondary,
                        cursorColor = PrimaryGreen
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = error ?: "Déjalo vacío para usar el nombre de la cancha.",
                    fontSize = 12.sp,
                    color = if (error != null) ErrorRed else TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(texto) }) { Text("Guardar", color = PrimaryGreen) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar", color = TextSecondary) }
        }
    )
}

/** Confirmación antes de borrar una grabación. */
@Composable
fun EliminarGrabacionDialog(
    titulo: String,
    onEliminar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = DialogBackground,
        title = { Text("Eliminar video", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Se borrará \"$titulo\" de tus grabaciones y de este teléfono. No se puede deshacer.",
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onEliminar) { Text("Eliminar", color = ErrorRed) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar", color = TextSecondary) }
        }
    )
}
