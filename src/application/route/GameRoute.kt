package pl.sose1.application.route


import application.model.game.*
import application.model.user.UserSession
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import pl.sose1.application.controller.GameController
import pl.sose1.domain.`interface`.GameRepository
import pl.sose1.domain.`interface`.SessionRepository
import pl.sose1.domain.entity.Game

fun Routing.game() {
    val gameController: GameController by inject()
    val gameRepository: GameRepository by inject()
    val sessionRepository: SessionRepository by inject()

    webSocket("/game/{id}") {
        val gameId = call.parameters["id"] ?: throw Exception()
        val game: Game = gameRepository.findById(gameId) ?: throw Exception()

        val session = call.sessions.get<UserSession>()

        if (session == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No pictionary session"))
            return@webSocket
        }

        if (game.sessionBelongsToGame(session.id)) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Session do not belong to this game"))
            return@webSocket
        }

        println("SESJA: ${session.id}")
        sessionRepository.save(session.id, this)

        val userName: String = call.request.headers["UserName"] ?: throw Exception()
        println("USER: $userName")
        gameController.connect(gameId, session.id, userName)

        try {
            incoming.consumeEach {
                if (it is Frame.Text) {
                    val text = it.readText()

                    when (val request: GameRequest = Json.decodeFromString(text)) {
                        is SendMessage -> gameController.sendMessage(request, gameId, session)
                    }
                } else {
                    val byteArray = it.readBytes()
                    gameController.onPathDrawn(byteArray, gameId)
                }
            }
        } finally {
            gameController.disconnect(gameId, session.id)
            sessionRepository.delete(session.id)
        }
    }
}