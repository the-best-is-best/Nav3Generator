plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(project(":nav-compiler-api"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}
