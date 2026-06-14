<div align="center">
 <h1> Nav3 Generator </h1>
</div>
<div align="center">
<a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
<img alt="Platform" src="https://img.shields.io/badge/Platform-Kotlin%20Multiplatform-blueviolet.svg" />
<img alt="KSP" src="https://img.shields.io/badge/KSP-Supported-brightgreen.svg" />
<a href="https://github.com/the-best-is-best/"><img alt="Profile" src="https://img.shields.io/badge/github-%23181717.svg?&style=for-the-badge&logo=github&logoColor=white" height="20"/></a>
</div>

---

A powerful KSP-based navigation generator for **Compose Multiplatform Navigation 3**. It automates route definitions, polymorphic serialization, and screen mapping to provide a type-safe, boilerplate-free navigation experience across **Android**, **iOS**, **Desktop**, and **Web**.

It includes:

- **Automatic Route Discovery**: Generates route objects/classes from your Composable screens.
- **Auto-Serialization**: Generates `SerializersModule` for polymorphic navigation state saving.
- **Boilerplate-Free Mapping**: Generates the entire `when(key)` block for `NavDisplay`.
- **Smart Parameter Mapping**: Automatically maps Route data to Screen parameters.
- **Per-Screen Actions**: Automatically generates type-safe `Actions` classes and `CompositionLocals` for screen callbacks.
- **Screen Wrappers**: Easily apply common wrappers (like ViewModels or Providers) to groups of screens via annotations.

---

# Versions

[![Maven Central](https://img.shields.io/maven-central/v/io.github.the-best-is-best/nav3-annotations)](https://search.maven.org/artifact/io.github.the-best-is-best/nav3-annotations)

# 📦 Setup

## Add to `commonMain` dependencies

```kotlin
implementation("io.github.the-best-is-best:nav3-annotations:1.1.0")
```

Add the KSP processor to your project:

```kotlin
dependencies {
    add("kspCommonMainMetadata", "io.github.the-best-is-best:nav3-processor:1.1.0")
    // Add for each target to ensure code visibility in IDE
    add("kspAndroid", "io.github.the-best-is-best:nav3-processor:1.1.0")
    add("kspIosArm64", "io.github.the-best-is-best:nav3-processor:1.1.0")
    add("kspIosSimulatorArm64", "io.github.the-best-is-best:nav3-processor:1.1.0")
}
```

## To start generator

```bash
./gradlew :shared:assemble
```

---

# 🧩 Available Annotations

## `@NavGenerate`

### Attach to an interface to mark it as the base for generated routes.

```kotlin
@NavGenerate
interface Routes : NavKey
```

---

## `@NavDestination`

### Attach to a Composable function to generate a route for it.

```kotlin
@Target(AnnotationTarget.FUNCTION)
annotation class NavDestination(
    val name: String = "",      // Custom name for the route (e.g. "Splash")
    val group: String = "",     // Optional nesting group (e.g. "Secure")
    val wrapper: String = ""    // Optional wrapper function name (e.g. "SecureWrapper")
)
```

---

# 🛠️ Example Usage

### 1. Define your base interface

```kotlin
@NavGenerate
interface Routes : NavKey
```

### 2. Annotate your Screens

```kotlin
@Composable
@NavDestination(name = "Home", group = "Secure", wrapper = "SecureWrapper")
fun HomeScreen(
    x: Int,                         // Data: Automatically added to RoutesDestinations.Secure.Home
    onDialogOpen: (Boolean) -> Unit // Action: Automatically added to RoutesHomeActions
) {
    Text("Home Screen with value $x")
}
```

### 3. Usage in App

```kotlin
@Composable
fun App() {
    // rememberRoutesBackStack and RoutesDestinations are auto-generated
    val backStack = rememberRoutesBackStack(initialKey = RoutesDestinations.Splash)
    var isDialogOpen by remember { mutableStateOf(false) }

    // RoutesHomeActions is auto-generated specifically for HomeScreen
    val homeActions = RoutesHomeActions(
        onDialogOpen = { isDialogOpen = it },
        onBack = { backStack.removeLastOrNull() }
    )

    CompositionLocalProvider(LocalRoutesHomeActions provides homeActions) {
        NavDisplay<Routes>(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = { key -> routesEntryProvider(key) } // Auto-generated mapper
        )
    }
}
```

---

# ⚙️ Setup (Kotlin Multiplatform + KSP)

```kotlin
kotlin {
    sourceSets.named("commonMain").configure {
        // Ensure the IDE sees generated code
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
}
```

---

# 📍 Notes

- **Generated code path**: `build/generated/ksp/`
- **Full Type-Safety**: Any change in screen parameters is immediately reflected in the generated routes and actions.
- **Zero Manual Mapping**: No more manual `polymorphic { subclass(...) }` or huge `when` blocks.
- **Wrapper Support**: The `wrapper` function must take a single `@Composable () -> Unit` parameter named `content`.

---

# 🎉 Done
