package de.thkoeln.codescope.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.thkoeln.codescope.domain.user.User
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.thkoeln.codescope.viewmodel.SettingsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import de.thkoeln.codescope.components.StandardScreenLayout
import moe.tlaster.precompose.navigation.BackHandler

/**
 * Composable for the settings screen on desktop.
 *
 * @param user The current user.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param onLogout Callback to logout the user.
 */
@Composable
fun DesktopSettingsScreen(user: User, onNavigateBack: () -> Unit, onLogout: () -> Unit) {
    SettingsScreenContent(user = user, onLogout = onLogout, onNavigateBack = onNavigateBack, isMobile = false)
}

/**
 * Composable for the settings screen on mobile.
 *
 * @param user The current user.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param onLogout Callback to logout the user.
 */
@Composable
fun MobileSettingsScreen(user: User, onNavigateBack: () -> Unit, onLogout: () -> Unit) {
    SettingsScreenContent(user = user, onLogout = onLogout, onNavigateBack = onNavigateBack, isMobile = true)
}

/**
 * Main content for the settings screen.
 *
 * @param user The current user.
 * @param onLogout Callback to logout the user.
 * @param onNavigateBack Callback to navigate back.
 * @param isMobile True if the screen is for a mobile device, false otherwise.
 */
@Composable
private fun SettingsScreenContent(user: User, onLogout: () -> Unit, onNavigateBack: () -> Unit, isMobile: Boolean) {
    val viewModel: SettingsViewModel = viewModel { SettingsViewModel() }

    BackHandler(enabled = true) {
        onNavigateBack()
    }

    StandardScreenLayout(title = "Einstellungen", isMobile = isMobile) {
        SettingsCard(
            user = user,
            darkMode = viewModel.isDarkMode,
            onDarkModeChange = { viewModel.setDarkMode(it) },
            onLogout = onLogout
        )
    }
}

/**
 * Displays the settings card.
 *
 * @param user The current user.
 * @param darkMode True if dark mode is enabled, false otherwise.
 * @param onDarkModeChange Callback for when the dark mode setting changes.
 * @param onLogout Callback to logout the user.
 */
@Composable
private fun SettingsCard(
    user: User,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Allgemein", 
                fontWeight = FontWeight.Bold, 
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            SettingItem("Dark Mode", darkMode, onDarkModeChange)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Konto", 
                fontWeight = FontWeight.Bold, 
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Name: ${user.name}", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "E-Mail: ${user.email}", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Rolle: ${user.role}", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abmelden")
            }
        }
    }
}

/**
 * Displays a single setting item.
 *
 * @param title The title of the setting.
 * @param checked True if the setting is checked, false otherwise.
 * @param onCheckedChange Callback for when the setting is checked or unchecked.
 */
@Composable
private fun SettingItem(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title, 
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
