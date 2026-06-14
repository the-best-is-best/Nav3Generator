package io.github.tbib.nav3generator

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Nav3Generator",
    ) {
        App()
    }
}