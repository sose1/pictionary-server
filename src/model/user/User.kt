package pl.sose1.model.user

import java.util.*

class User(val name: String, var sessionId : UUID ) {
    val userId: UUID = UUID.randomUUID()

}