package pl.sose1.infrastructure.repository

import pl.sose1.domain.`interface`.UserRepository
import pl.sose1.domain.entity.User

class CacheUserRepository : UserRepository {

    private val users = mutableListOf<User>()

    override fun save(user: User) {
        users.add(user)
    }

    override fun findBySessionId(sessionId: String): User? =
            users.find { it.id == sessionId }


    override fun deleteBySessionId(sessionId: String) {
        users.removeIf { it.id == sessionId }
    }
}