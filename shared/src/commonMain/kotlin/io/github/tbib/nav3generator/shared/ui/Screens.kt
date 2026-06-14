package io.github.tbib.nav3generator.shared.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.tbib.nav3generator.annotations.NavDestination

val LocalShowSheet = staticCompositionLocalOf { false }

@Composable
fun SecureWrapper(content: @Composable () -> Unit) {
    val isSheetVisible = true 
    CompositionLocalProvider(LocalShowSheet provides isSheetVisible) {
        content()
    }
}

@Composable
@NavDestination(name = "Splash")
fun SplashScreen(onNavigateToLogin: () -> Unit) {
    Column {
        Text("Splash Screen")
        Button(onClick = onNavigateToLogin) {
            Text("Go to Login (Add)")
        }
    }
}

@Composable
@NavDestination(name = "Login")
fun LoginScreen(onLoginSuccess: () -> Unit, onBack: () -> Unit) {
    Column {
        Text("Login Screen")
        Button(onClick = onLoginSuccess) {
            Text("Login (Replace with Home)")
        }
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
@NavDestination(name = "Home", group = "Secure", wrapper = "SecureWrapper")
fun HomeScreen(x: Int, onNavigateToNotification: () -> Unit, onBack: () -> Unit) {
    Column {
        Text("Home Screen (x: $x)")
        Button(onClick = onNavigateToNotification) {
            Text("Go to Notifications (Add)")
        }
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
@NavDestination(name = "Notification", group = "Secure", wrapper = "SecureWrapper")
fun NotificationScreen(goToAvailability: Boolean, onLogout: () -> Unit, onBack: () -> Unit) {
    Column {
        Text("Notification Screen (Go to availability: $goToAvailability)")
        Button(onClick = onLogout) {
            Text("Logout (Replace with Splash)")
        }
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
