package pl.sose1.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val name: String,
    var id: String,
)