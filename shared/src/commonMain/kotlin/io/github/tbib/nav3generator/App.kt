package io.github.tbib.nav3generator

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.ui.NavDisplay
import io.github.tbib.nav3generator.shared.navigation.Routes
import io.github.tbib.nav3generator.shared.navigation.RoutesActions
import io.github.tbib.nav3generator.shared.navigation.RoutesGenerated
import io.github.tbib.nav3generator.shared.navigation.LocalRoutesActions
import io.github.tbib.nav3generator.shared.navigation.rememberRoutesBackStack
import io.github.tbib.nav3generator.shared.navigation.routesEntryProvider

@Composable
fun App() {
    MaterialTheme {
        // 1. Initial key from generated Routes
        val backStack = rememberRoutesBackStack(initialKey = RoutesGenerated.Splash)
        
        // 2. Define your actions (Add, Back, Replace)
        val actions = RoutesActions(
            onNavigateToLogin = { 
                backStack.add(RoutesGenerated.Login) 
            },
            onLoginSuccess = { 
                // Replace: Clear and add Home
                backStack.clear()
                backStack.add(RoutesGenerated.Secure.Home(x = 100)) 
            },
            onNavigateToNotification = { 
                backStack.add(RoutesGenerated.Secure.Notification(goToAvailability = true)) 
            },
            onLogout = { 
                // Replace all with Splash
                backStack.clear()
                backStack.add(RoutesGenerated.Splash)
            },
            onBack = { 
                backStack.removeLastOrNull() 
            }
        )

        // 3. Provide actions to all screens
        CompositionLocalProvider(LocalRoutesActions provides actions) {
            NavDisplay<Routes>(
                backStack = backStack,
                onBack = { actions.onBack() },
                entryProvider = { key ->
                    // Fully auto-generated mapping
                    routesEntryProvider(key)
                }
            )
        }
    }
}
