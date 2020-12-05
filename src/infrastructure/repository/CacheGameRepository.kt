package pl.sose1.infrastructure.repository

import pl.sose1.domain.`interface`.GameRepository
import pl.sose1.domain.entity.Game

class CacheGameRepository : GameRepository {

    private val games = mutableListOf<Game>()

    override fun save(game: Game): Game {
        val g = findById(game.id)
        val i = games.indexOf(g)

        if (i == -1) {
            games.add(game)
        } else {
            games[i] = game
        }

        return game
    }

    override fun findById(id: String): Game? =
        games.find { it.id == id }

    override fun findByCode(code: String): Game? =
        games.find { it.code == code }

    override fun findEmpty(): Game? =
        games.find { it.users.isEmpty() }

    override fun deleteById(id: String) {
        games.removeIf { it.id == id }
    }
}