package com.micalle.mx
data class Report(
    var id: String = "",
    val userId: String = "",
    val problem: String = "",
    val direction: String = "",
    val suggestion: String = "",
    val date: String = "",
    val description: String = "",
    val timestamp: Long = 0L,
    val status: String = "pendiente",
    val photoBase64: String = "" // ✅ Campo para guardar la imagen como texto
)