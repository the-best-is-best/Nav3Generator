package io.github.tbib.nav3generator

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform