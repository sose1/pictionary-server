package pl.sose1.model.user

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class User(
    val name: String,
    var sessionId: String ) {
    val userId: String = UUID.randomUUID().toString()
}