package pl.sose1.domain.`interface`

import pl.sose1.domain.entity.User

interface UserRepository {

    fun save(user: User)

    fun findBySessionId(sessionId: String): User?

    fun deleteBySessionId(sessionId: String)
}