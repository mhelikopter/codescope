package de.thkoeln.codescope.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class CSButtonVariant {
    PRIMARY, SECONDARY, OUTLINE, GHOST, DESTRUCTIVE
}

enum class CSButtonSize {
    SMALL, MEDIUM, LARGE
}

@Composable
fun CSButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CSButtonVariant = CSButtonVariant.PRIMARY,
    size: CSButtonSize = CSButtonSize.MEDIUM,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    isLoading: Boolean = false
) {
    val colors = when (variant) {
        CSButtonVariant.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        CSButtonVariant.SECONDARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
        CSButtonVariant.OUTLINE -> ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
        CSButtonVariant.GHOST -> ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        CSButtonVariant.DESTRUCTIVE -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )
    }
    
    val contentPadding = when (size) {
        CSButtonSize.SMALL -> PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        CSButtonSize.MEDIUM -> PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        CSButtonSize.LARGE -> PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    }
    
    val buttonContent: @Composable RowScope.() -> Unit = {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text)
        }
    }
    
    when (variant) {
        CSButtonVariant.OUTLINE -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled && !isLoading,
                colors = colors,
                contentPadding = contentPadding,
                content = buttonContent
            )
        }
        CSButtonVariant.GHOST -> {
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled && !isLoading,
                colors = colors,
                contentPadding = contentPadding,
                content = buttonContent
            )
        }
        else -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled && !isLoading,
                colors = colors,
                contentPadding = contentPadding,
                content = buttonContent
            )
        }
    }
}
