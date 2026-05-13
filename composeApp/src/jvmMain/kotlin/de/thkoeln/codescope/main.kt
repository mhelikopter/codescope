package de.thkoeln.codescope

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import de.thkoeln.codescope.di.initKoin
import moe.tlaster.precompose.PreComposeApp

fun main() = application {
    initKoin()
    val icon = painterResource("icons/icon.png")
    Window(
        onCloseRequest = ::exitApplication,
        title = "CodeScope",
        icon = icon
    ) {
        PreComposeApp {
            App(isMobile = false)
        }
    }
}