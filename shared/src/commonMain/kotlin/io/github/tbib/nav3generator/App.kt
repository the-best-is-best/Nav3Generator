package io.github.tbib.nav3generator

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.ui.NavDisplay
import io.github.tbib.nav3generator.shared.navigation.*

@Composable
fun App() {
    MaterialTheme {
        // 1. Initial key from generated Routes
        val backStack = rememberRoutesBackStack(initialKey = RoutesDestinations.Splash)

        // 2. Define per-screen actions
        val splashActions = RoutesSplashActions(
            onNavigateToLogin = { backStack.add(RoutesDestinations.Login) }
        )

        val loginActions = RoutesLoginActions(
            onLoginSuccess = {
                backStack.clear()
                backStack.add(RoutesDestinations.Secure.Home(x = 100))
            },
            onBack = { backStack.removeLastOrNull() }
        )

        val homeActions = RoutesHomeActions(
            onNavigateToNotification = {
                backStack.add(RoutesDestinations.Secure.Notification(goToAvailability = true))
            },
            onBack = { backStack.removeLastOrNull() }
        )

        val notificationActions = RoutesNotificationActions(
            onLogout = {
                backStack.clear()
                backStack.add(RoutesDestinations.Splash)
            },
            onBack = { backStack.removeLastOrNull() }
        )

        // 3. Provide actions using per-screen CompositionLocals
        CompositionLocalProvider(
            LocalRoutesSplashActions provides splashActions,
            LocalRoutesLoginActions provides loginActions,
            LocalRoutesHomeActions provides homeActions,
            LocalRoutesNotificationActions provides notificationActions
        ) {
            NavDisplay<Routes>(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = { key ->
                    // Fully auto-generated mapping
                    routesEntryProvider(key)
                }
            )
        }
    }
}
