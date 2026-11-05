package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Definición de colores principales
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)
private val ButtonBorderGreen = Color(0xFF64F089)
private val ErrorRed = Color(0xFFFF6B6B)

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    // Valores del formulario pasados por parámetro (Stateless)
    nombreUsuario: String = "",
    correo: String = "",
    contrasena: String = "",
    fechaNacimiento: String = "",
    // Mensaje de validación / error al crear la cuenta (null = sin error)
    errorMensaje: String? = null,
    // Callbacks para la futura navegación e ingreso de datos
    onNombreUsuarioChange: (String) -> Unit = {},
    onCorreoChange: (String) -> Unit = {},
    onContrasenaChange: (String) -> Unit = {},
    onFechaNacimientoChange: (String) -> Unit = {},
    onCrearCuentaClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Título principal
            Text(
                text = "Crea tu cuenta",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Campo: Nombre de Usuario
            Text(
                text = "Nombre de Usuario:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            CustomFormTextField(
                value = nombreUsuario,
                onValueChange = onNombreUsuarioChange,
                placeholderText = "Ingresa tu usuario"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Campo: Correo
            Text(
                text = "Correo:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            CustomFormTextField(
                value = correo,
                onValueChange = onCorreoChange,
                placeholderText = "ejemplo@correo.com",
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Campo: Contraseña
            Text(
                text = "Contraseña:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            CustomFormTextField(
                value = contrasena,
                onValueChange = onContrasenaChange,
                placeholderText = "••••••••",
                isPassword = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Campo: Fecha de Nacimiento
            Text(
                text = "Fecha de Nacimiento:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            CustomFormTextField(
                value = fechaNacimiento,
                onValueChange = onFechaNacimientoChange,
                placeholderText = "DD/MM/AAAA",
                keyboardType = KeyboardType.Number
            )

            if (errorMensaje != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMensaje,
                    fontSize = 14.sp,
                    color = ErrorRed
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón de acción: Crear cuenta
            OutlinedButton(
                onClick = onCrearCuentaClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ButtonBorderGreen)
            ) {
                Text(
                    text = "Crear cuenta",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// Composable auxiliar para los campos de texto del formulario
@Composable
private fun CustomFormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
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
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType
        ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
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
fun LoginPreview() {
    LoginScreen()
}
