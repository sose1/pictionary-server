package pl.sose1.routes


import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.lobby.LobbyResponse
import model.lobby.Users
import pl.sose1.lobbies
import pl.sose1.model.lobby.Connect
import pl.sose1.model.lobby.Lobby
import pl.sose1.model.lobby.LobbyRequest
import pl.sose1.model.user.User
import pl.sose1.model.user.UserSession
import java.util.*
import kotlin.collections.LinkedHashSet

private lateinit var lobby: Lobby

fun Routing.lobby() {

    val userSessions = Collections.synchronizedSet(LinkedHashSet<UserSession>())


    webSocket("/lobby/{id}") {
        println("Find lobby by ID: ${call.parameters["id"]}")

        lobby = findLobbyById(call.parameters["id"])!!

        val userSession = UserSession(this)
        userSessions += userSession

        try {
            while (true) {
                when (val frame = incoming.receive() ) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        val request: LobbyRequest = Json.decodeFromString(text)

                        when (request) {
                            is Connect -> connectedUser(userSessions, request, userSession)
                            else -> { }
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


suspend fun connectedUser(userSessions: MutableSet<UserSession>,
                          request: Connect,
                          userSession: UserSession) {

    val user = findUserById(request.userId)
    user?.sessionId = userSession.id

    lobby.users.forEach { sendResponse(it.sessionId, userSessions, Users(lobby.users) as LobbyResponse) }
}

suspend fun disconnectUser(userSessions: MutableSet<UserSession>,
                           sessionId: String) {
    val response: LobbyResponse = Users(lobby.users)

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
            lobby.users.forEach { sendResponse(it.sessionId, userSessions,Users(lobby.users) as LobbyResponse) }
        }
    }
}

suspend fun sendResponse(sessionId: String,
                         userSessions: MutableSet<UserSession>,
                         response: LobbyResponse) {
    userSessions.find { userSession ->
        sessionId == userSession.id
    }?.session?.send(Frame.Text(Json.encodeToString(response)))
}

fun findLobbyById(lobbyId: String?): Lobby? = lobbies.find { lobby ->
    lobbyId == lobby.lobbyId }

fun findUserById(userID: String): User? = lobby.users.find { user ->
    userID == user.userId }