package pl.sose1.domain.`interface`

import pl.sose1.domain.entity.Game

interface GameRepository {

    fun save(game: Game): Game

    fun findById(id: String): Game?

    fun findByCode(code: String): Game?

    fun findEmpty(): Game?

    fun deleteById(id: String)
}