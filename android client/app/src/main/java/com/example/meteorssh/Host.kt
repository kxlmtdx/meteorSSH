package com.example.meteorssh

// Host.kt
data class Host(
    val name: String,
    val address: String,
    val osType: String = "windows"
)  : java.io.Serializable