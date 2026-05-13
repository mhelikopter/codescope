package de.thkoeln.codescope.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.thkoeln.codescope.domain.user.UserRole

data class SidebarItem(
    val icon: ImageVector,
    val label: String,
    val route: String
)

@Composable
fun CSSidebar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    userRole: UserRole, // Geändert von isStudent auf userRole für präzisere Steuerung
    modifier: Modifier = Modifier
) {
    val studentItems = listOf(
        SidebarItem(Icons.Filled.Dashboard, "Dashboard", "dashboard"),
        SidebarItem(Icons.Filled.Folder, "Meine Projekte", "projects"),
        SidebarItem(Icons.Filled.Description, "Kriterienkataloge", "criteria"),
        SidebarItem(Icons.Filled.School, "Kurse", "courses"),
        SidebarItem(Icons.Filled.Settings, "Einstellungen", "settings")
    )

    val docentItems = listOf(
        SidebarItem(Icons.Filled.Dashboard, "Dashboard", "dashboard"),
        SidebarItem(Icons.Filled.Folder, "Projekte", "projects"),
        SidebarItem(Icons.Filled.Description, "Kriterienkataloge", "criteria"),
        SidebarItem(Icons.Filled.School, "Kurse", "courses"),
        SidebarItem(Icons.Filled.Assessment, "Bewertungen", "grading"),
        SidebarItem(Icons.Filled.Settings, "Einstellungen", "settings")
    )

    val adminItems = listOf(
        SidebarItem(Icons.Filled.AdminPanelSettings, "Admin Panel", "admin"),
        SidebarItem(Icons.Filled.Settings, "Einstellungen", "settings")
    )

    val items = when (userRole) {
        UserRole.STUDENT -> studentItems
        UserRole.LECTURER -> docentItems
        UserRole.ADMIN -> adminItems
    }

    Surface(
        modifier = modifier
            .width(240.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp)
        ) {
            // Logo/Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "CodeScope",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Items
            items.forEach { item ->
                SidebarNavItem(
                    item = item,
                    isSelected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout
            SidebarNavItem(
                item = SidebarItem(Icons.Filled.Logout, "Abmelden", "logout"),
                isSelected = false,
                onClick = { onNavigate("logout") }
            )
        }
    }
}

@Composable
private fun SidebarNavItem(
    item: SidebarItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor
        )
    }
}
