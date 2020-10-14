package pl.sose1.routes

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import pl.sose1.lobbies
import pl.sose1.model.lobby.Lobby
import pl.sose1.model.lobby.LobbyRequest
import pl.sose1.model.lobby.RequestEventName
import pl.sose1.model.user.User
import pl.sose1.model.user.UserSession
import pl.sose1.toJSON
import java.util.*
import kotlin.collections.LinkedHashSet

private lateinit var lobby: Lobby

fun Routing.lobby() {

    val mapper = jacksonObjectMapper()
    val userSessions = Collections.synchronizedSet(LinkedHashSet<UserSession>())


    webSocket("/lobby/{id}") {
        println("Find lobby by id: ${call.parameters["id"]}")

        lobby = findLobbyById(call.parameters["id"])!!

        val userSession = UserSession(this)
        userSessions += userSession

        try {
            while (true) {
                when (val frame = incoming.receive() ) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        val request: LobbyRequest = mapper.readValue(text)

                        val user = findUserById(request.userId)
                        user?.sessionId = userSession.id

                        when (request.eventName) {
                            RequestEventName.CONNECT -> connectedUser(userSessions)

                        }
                    }
                }
            }
        } finally {
            disconnectUser(userSessions, userSession.id)
            userSessions -= userSession
        }

    }
}


suspend fun connectedUser(userSessions: MutableSet<UserSession>) {
    lobby.users.forEach { sendResponse(it.sessionId, userSessions, lobby.toJSON()) }
}

suspend fun disconnectUser(userSessions: MutableSet<UserSession>, sessionId: UUID) {
    lobbies.forEach {lobby ->
        val user = lobby.users.find { user ->
            sessionId == user.sessionId
        }

        print("${user?.name}, ${user?.userId}")

        user?.let { lobby.users.remove(user)
            if (user.userId == lobby.creatorId) {
                lobbies.remove(lobby)
                lobby.users.forEach { TODO("Info o usunieciu lobby i wyrzucenie do home activity") }
            }
            lobby.users.forEach { sendResponse(it.sessionId, userSessions, lobby.toJSON()) }
        }
    }
}

suspend fun sendResponse(sessionId: UUID, userSessions: MutableSet<UserSession>, response: String) {
    userSessions.find { userSession ->
        sessionId == userSession.id
    }?.session?.send(Frame.Text(response))
}

fun findLobbyById(lobbyId: String?): Lobby? = lobbies.find { lobby -> lobbyId == lobby.lobbyId.toString() }
fun findUserById(userID: UUID): User? = lobby.users.find { user -> userID == user.userId }