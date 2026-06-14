package io.github.tbib.nav3generator.shared.ui.wrapper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier


val LocalShowSheet = staticCompositionLocalOf { false }

@Composable
fun SecureWrapper(content: @Composable () -> Unit) {
    val isSheetVisible = true
    CompositionLocalProvider(LocalShowSheet provides isSheetVisible) {
       Box(modifier = Modifier.safeContentPadding()){
           content()
       }
    }
}