package dev.wandscheer.remote_control

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform