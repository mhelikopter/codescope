package de.thkoeln.codescope.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(userName: String, onNavigateToSettings: () -> Unit) {
    TopAppBar(
        title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
        actions = {
            Text(
                text = userName,
                modifier = Modifier.padding(end = 12.dp),
                fontSize = 14.sp
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onNavigateToSettings() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.split(" ").mapNotNull { it.firstOrNull() }.joinToString(""),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun DashboardWelcomeCard(
    title: String,
    subtitle: String,
    buttonText: String,
    buttonIcon: ImageVector? = null,
    buttonColor: Color = MaterialTheme.colorScheme.primary,
    onButtonClick: () -> Unit,
    isMobile: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(if (isMobile) 16.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = if (isMobile) 18.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                modifier = if (isMobile) Modifier.fillMaxWidth() else Modifier
            ) {
                if (buttonIcon != null) {
                    Icon(buttonIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(buttonText)
            }
        }
    }
}

data class DashboardStat(
    val title: String,
    val value: String,
    val color: Color
)

@Composable
fun DashboardStatistics(
    stats: List<DashboardStat>,
    isMobile: Boolean
) {
    if (isMobile) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            stats.forEach { stat ->
                StatCard(
                    title = stat.title,
                    value = stat.value,
                    valueColor = stat.color,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            stats.forEach { stat ->
                StatCard(
                    title = stat.title,
                    value = stat.value,
                    valueColor = stat.color,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
