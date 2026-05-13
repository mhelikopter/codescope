package de.thkoeln.codescope.components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.thkoeln.codescope.theme.*

enum class CSBadgeVariant {
    SUCCESS, WARNING, ERROR, INFO, DEFAULT
}

@Composable
fun CSBadge(
    text: String,
    variant: CSBadgeVariant = CSBadgeVariant.DEFAULT,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (variant) {
        CSBadgeVariant.SUCCESS -> Success.copy(alpha = 0.1f)
        CSBadgeVariant.WARNING -> Warning.copy(alpha = 0.1f)
        CSBadgeVariant.ERROR -> Error.copy(alpha = 0.1f)
        CSBadgeVariant.INFO -> Primary.copy(alpha = 0.1f)
        CSBadgeVariant.DEFAULT -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = when (variant) {
        CSBadgeVariant.SUCCESS -> SuccessDark
        CSBadgeVariant.WARNING -> WarningDark
        CSBadgeVariant.ERROR -> ErrorDark
        CSBadgeVariant.INFO -> PrimaryDark
        CSBadgeVariant.DEFAULT -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
