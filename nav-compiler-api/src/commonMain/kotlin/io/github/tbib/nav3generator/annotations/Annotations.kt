package io.github.tbib.nav3generator.annotations

import kotlin.reflect.KClass

/**
 * Annotate your base navigation interface with this.
 * KSP will generate route classes for all @NavDestination annotated functions.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class NavGenerate

/**
 * Annotate your Composable screen functions with this.
 * @param name The name of the generated route class. If empty, uses the function name.
 * @param group Optional group name for nesting (e.g., "Secure").
 * @param wrapper Optional name of a Composable function that wraps this screen.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class NavDestination(
    val name: String = "",
    val group: String = "",
    val wrapper: String = ""
)
