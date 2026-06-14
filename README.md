<div align="center">
 <h1> Nav3 Generator <h1>
</div>
<div align="center">
<a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
<img src="https://img.shields.io/badge/Platform-Kotlin%20Multiplatform-blueviolet.svg" />
<img src="https://img.shields.io/badge/KSP-Supported-brightgreen.svg" />
<a href="https://github.com/the-best-is-best/"><img alt="Profile" src="https://img.shields.io/badge/github-%23181717.svg?&style=for-the-badge&logo=github&logoColor=white" height="20"/></a>
</div>

---

A powerful KSP-based navigation generator for **Compose Multiplatform Navigation 3**. It automates route definitions, polymorphic serialization, and screen mapping to provide a type-safe, boilerplate-free navigation experience across **Android**, **iOS**, **Desktop**, and **Web**.

It includes:

- **Automatic Route Discovery**: Generates route objects/classes from your Composable screens.
- **Auto-Serialization**: Generates `SerializersModule` for polymorphic navigation state saving.
- **Boilerplate-Free Mapping**: Generates the entire `when(key)` block for `NavDisplay`.
- **Smart Parameter Mapping**: Automatically maps Route data to Screen parameters.
- **CompositionLocal Actions**: Automatically gathers screen callbacks into a single, type-safe `Actions` class.
- **Screen Wrappers**: Easily apply common wrappers (like ViewModels or Providers) to groups of screens via annotations.

---

# Versions

[![Maven Central](https://img.shields.io/maven-central/v/io.github.the-best-is-best/nav-compiler-api)](https://search.maven.org/artifact/io.github.the-best-is-best/nav-compiler-api)

# 📦 Setup

## Add to `commonMain` dependencies

```kotlin
implementation("io.github.the-best-is-best:nav-compiler-api:1.0.0-rc.1")
```

Add the KSP processor to your project:

```kotlin
dependencies {
    add("kspCommonMainMetadata", "io.github.the-best-is-best:nav-compiler:1.0.0-rc.1")
    // Add for each target if needed
    add("kspAndroid", "io.github.the-best-is-best:nav-compiler:1.0.0-rc.1")
    add("kspIosArm64", "io.github.the-best-is-best:nav-compiler:1.0.0-rc.1")
    add("kspIosSimulatorArm64", "io.github.the-best-is-best:nav-compiler:1.0.0-rc.1")
}
```

## To start generator

```bash
./gradlew :shared:assemble
```

---

# 🧩 Available Annotations

## `@NavGenerate`

### Attach to a sealed interface to mark it as the base for generated routes.

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
    val name: String = "",      // Custom name for the route
    val group: String = "",     // Optional nesting group (e.g. "Secure")
    val wrapper: String = ""    // Optional wrapper function name
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
    x: Int,                         // Data: Automatically added to RoutesGenerated.Secure.Home
    onDialogOpen: (Boolean) -> Unit // Action: Automatically added to RoutesActions
) {
    Text("Home Screen with value $x")
}
```

### 3. Usage in App

```kotlin
@Composable
fun App() {
    // rememberRoutesBackStack and RoutesGenerated are auto-generated
    val backStack = rememberRoutesBackStack(initialKey = RoutesGenerated.Splash)
    var isDialogOpen by remember { mutableStateOf(false) }

    // RoutesActions is auto-generated based on all @NavDestination parameters
    val actions = RoutesActions(
        onDialogOpen = { isDialogOpen = it },
        onBack = { backStack.removeLastOrNull() }
    )

    CompositionLocalProvider(LocalRoutesActions provides actions) {
        NavDisplay<Routes>(
            backStack = backStack,
            onBack = { actions.onBack() },
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
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
}
```

---

# 📍 Notes

- **Generated code path**: `build/generated/ksp/`
- **Full Type-Safety**: Any change in screen parameters is immediately reflected in the generated routes and actions.
- **Zero Manual Mapping**: No more manual `polymorphic { subclass(...) }` or huge `when` blocks.

---

# 🎉 Done
