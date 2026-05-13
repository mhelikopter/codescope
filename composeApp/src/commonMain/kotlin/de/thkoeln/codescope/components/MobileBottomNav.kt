package de.thkoeln.codescope.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.thkoeln.codescope.domain.user.UserRole

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun MobileBottomNav(
    currentRoute: String,
    userRole: UserRole,
    onNavigate: (String) -> Unit
) {
    val items = when (userRole) {
        UserRole.ADMIN -> listOf(
            BottomNavItem("admin", Icons.Filled.AdminPanelSettings, "Admin"),
            BottomNavItem("settings", Icons.Filled.Settings, "Settings")
        )
        UserRole.LECTURER -> listOf(
            BottomNavItem("dashboard", Icons.Filled.Home, "Home"),
            BottomNavItem("projects", Icons.Filled.Folder, "Projekte"),
            BottomNavItem("grading", Icons.Filled.Assignment, "Bewertung"),
            BottomNavItem("criteria", Icons.Filled.ListAlt, "Kriterien"),
            BottomNavItem("courses", Icons.Filled.School, "Kurse"),
            BottomNavItem("settings", Icons.Filled.Settings, "Optionen")
        )
        UserRole.STUDENT -> listOf(
            BottomNavItem("dashboard", Icons.Filled.Home, "Home"),
            BottomNavItem("projects", Icons.Filled.Folder, "Projekte"),
            BottomNavItem("criteria", Icons.Filled.ListAlt, "Kriterien"),
            BottomNavItem("courses", Icons.Filled.School, "Kurse"),
            BottomNavItem("settings", Icons.Filled.Settings, "Optionen")
        )
    }
    
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                alwaysShowLabel = false, // WICHTIG: Versteckt Text bei inaktiven Tabs für mehr Platz
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { 
                    Text(
                        text = item.label,
                        fontSize = 10.sp, // Etwas kleinere Schrift
                        softWrap = false,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
