package de.thkoeln.codescope.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.logic.ILoginSteuerung
import de.thkoeln.codescope.viewmodel.LoginViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Composable for the login screen on desktop.
 *
 * @param loginSteuerung The login controller.
 * @param onLoginSuccess Callback for when the login is successful.
 */
@Composable
fun DesktopLoginScreen(loginSteuerung: ILoginSteuerung, onLoginSuccess: (User) -> Unit) {
    LoginScreen(loginSteuerung = loginSteuerung, onLoginSuccess = onLoginSuccess)
}

/**
 * Composable for the login screen on mobile.
 *
 * @param loginSteuerung The login controller.
 * @param onLoginSuccess Callback for when the login is successful.
 */
@Composable
fun MobileLoginScreen(loginSteuerung: ILoginSteuerung, onLoginSuccess: (User) -> Unit) {
    LoginScreen(loginSteuerung = loginSteuerung, onLoginSuccess = onLoginSuccess)
}

/**
 * Main content for the login screen.
 *
 * @param loginSteuerung The login controller.
 * @param onLoginSuccess Callback for when the login is successful.
 */
@Composable
private fun LoginScreen(
    loginSteuerung: ILoginSteuerung,
    onLoginSuccess: (User) -> Unit
) {
    val viewModel: LoginViewModel = viewModel {
        LoginViewModel(loginSteuerung)
    }

    // Reset state when the screen is shown to ensure no stale loading state
    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 450.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp), // Fixed padding for consistency
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                WelcomeHeader()
                
                if (viewModel.errorMessage != null) {
                    Text(
                        text = viewModel.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                LoginButton(isLoading = viewModel.isLoading, onClick = {
                    viewModel.login(onLoginSuccess)
                })
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                LoginFooter()
            }
        }
    }
}

/**
 * Displays the welcome header of the login screen.
 */
@Composable
private fun WelcomeHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Willkommen bei CodeScope",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = "KI-gestützte Quellcode-Analyse für Studierende und Dozenten",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Displays the login button.
 *
 * @param isLoading True if the login is in progress, false otherwise.
 * @param onClick Callback for when the button is clicked.
 */
@Composable
private fun LoginButton(isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("G", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Mit Google anmelden", fontSize = 16.sp)
            }
        }
    }
}

/**
 * Displays the footer of the login screen.
 */
@Composable
private fun LoginFooter() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Nach der Anmeldung wird automatisch ein Benutzerprofil erstellt",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeatureItem(text = "Automatisierte KI-Codeanalyse")
            FeatureItem(text = "Individuelle Kriterienkataloge")
        }
    }
}

/**
 * Displays a feature item with a checkmark.
 *
 * @param text The text to display.
 */
@Composable
private fun FeatureItem(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
