package com.graveyard.yingdilite

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform