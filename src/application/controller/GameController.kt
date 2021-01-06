package pl.sose1.application.controller

import application.model.game.SendMessage
import application.model.user.UserSession
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import pl.sose1.domain.`interface`.GameRepository
import pl.sose1.domain.service.GameService

class GameController(
        private val gameService: GameService,
        private val gameRepository: GameRepository,
) {

    suspend fun getEmptyGame(call: ApplicationCall) {
        val game = gameRepository.findEmpty() ?: throw Exception()

        println("GetEmptyGame: GAME ID ${game.id}")
        call.respond(game)
    }

    suspend fun getGameByCode(code: String, call: ApplicationCall) {
        val game = gameRepository.findByCode(code) ?: throw NotFoundException()

        println("GetGameByCode: GAME ID ${game.id}")

        call.respond(game)
    }


    suspend fun getGameById(id: String, call: ApplicationCall) {
        val game = gameRepository.findById(id) ?: throw Exception()
        println("GetGameById: GAME ID ${game.id}")

        call.respond(game)
    }

    suspend fun connect(gameId: String, sessionId: String, userName: String) {
        gameService.saveUserInGame(userName, sessionId, gameId)
    }

    suspend fun disconnect(gameId: String, sessionId: String) {
        gameService.removeUserFromGame(sessionId, gameId)
    }

    suspend fun sendMessage(sendMessageRequest: SendMessage, gameId: String, session: UserSession ) {
        gameService.sendMessage(sendMessageRequest.text, gameId, session.id)
    }

    suspend fun onPathDrawn(byteArray: ByteArray, gameId: String) {
        gameService.onPathDrawn(byteArray, gameId)
    }
}