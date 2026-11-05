package me.juliangarcia.sportygoapp

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import me.juliangarcia.sportygoapp.data.AccountStorage
import me.juliangarcia.sportygoapp.data.AuthRepository
import me.juliangarcia.sportygoapp.data.AuthState
import me.juliangarcia.sportygoapp.ui.theme.SportyGoAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // La app es oscura: íconos claros en las barras del sistema sin importar el tema del teléfono.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        // Cuentas y sesión guardadas en el dispositivo.
        val auth = AuthState(AuthRepository(AccountStorage(applicationContext)))

        setContent {
            SportyGoAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0B131D)
                ) {
                    SportyGoNavHost(
                        auth = auth,
                        // Evita que el contenido quede debajo de la barra de estado,
                        // la barra de navegación y el teclado.
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                    )
                }
            }
        }
    }
}
