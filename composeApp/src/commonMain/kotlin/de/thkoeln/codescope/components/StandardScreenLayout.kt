package de.thkoeln.codescope.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardScreenLayout(
    title: String,
    isMobile: Boolean,
    showBackButton: Boolean = false,
    onNavigateBack: () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        // WICHTIG: Wir deaktivieren die automatischen Insets für das Scaffold, 
        // da die Navigation in App.kt außerhalb liegt oder die bottomBar es selbst regelt.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (isMobile) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
        bottomBar = bottomBar
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isMobile) 16.dp else 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Padding oben hinzufügen, damit der Inhalt nicht am Header klebt
                Spacer(modifier = Modifier.height(if (isMobile) 16.dp else 32.dp))
                
                if (!isMobile) {
                    ScreenHeader(
                        title = title,
                        showBackButton = showBackButton,
                        onNavigateBack = onNavigateBack
                    )
                }
                content()
                
                // Padding unten hinzufügen, damit man über das letzte Element hinaus scrollen kann
                Spacer(modifier = Modifier.height(if (isMobile) 32.dp else 32.dp))
            }
        }
    }
}
