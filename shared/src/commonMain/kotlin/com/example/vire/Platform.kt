package com.example.vire

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform