package pl.sose1.routes


import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.lobby.LobbyResponse
import model.lobby.Messages
import model.lobby.NewCreator
import model.lobby.Users
import pl.sose1.lobbies
import pl.sose1.model.lobby.Connect
import pl.sose1.model.lobby.Lobby
import pl.sose1.model.lobby.LobbyRequest
import pl.sose1.model.lobby.Message
import pl.sose1.model.user.User
import pl.sose1.model.user.UserSession
import java.util.*
import kotlin.collections.LinkedHashSet

private lateinit var lobby: Lobby

fun Routing.lobby() {

    val userSessions = Collections.synchronizedSet(LinkedHashSet<UserSession>())


    webSocket("/lobby/{id}") {
        lobby = findLobbyById(call.parameters["id"])!!

        val userSession = UserSession(this)
        userSessions += userSession

        try {
            while (true) {
                when (val frame = incoming.receive()) {
                    is Frame.Text -> {
                        val text = frame.readText()

                        when (val request: LobbyRequest = Json.decodeFromString(text)) {
                            is Connect -> connectedUser(userSessions, request, userSession)
                            is Message -> sendMessage(request, userSessions)
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

suspend fun sendMessage(request: Message,
                        userSessions: MutableSet<UserSession>) {


    lobby.messages.add(Message(request.text, request.authorId, "incoming", request.authorName))
    lobby.users.forEach { sendMessages(it, userSessions, lobby) }
}

suspend fun sendMessages(user: User,
                         userSessions: MutableSet<UserSession>,
                         lobby: Lobby) {

    val userSession = userSessions.find { userSession ->
        user.sessionId == userSession.id
    }!!

    lobby.messages.forEach { message ->
        if (message.authorId == user.userId && user.sessionId == userSession.id ) {
            message.messageType = "send"
        } else {
            message.messageType = "incoming"
        }
    }

    userSession.session.send(Frame.Text(Json.encodeToString(Messages(lobby.messages) as LobbyResponse)))
}

suspend fun connectedUser(userSessions: MutableSet<UserSession>,
                          request: Connect,
                          userSession: UserSession) {

    val user = findUserById(request.userId)
    user?.sessionId = userSession.id

    if (lobby.messages.isNotEmpty()) {
        lobby.users.forEach { sendMessages(it, userSessions, lobby) }
    }
    lobby.users.forEach { sendResponse(it.sessionId, userSessions, Users(lobby.users) as LobbyResponse) }

}

suspend fun disconnectUser(userSessions: MutableSet<UserSession>,
                           sessionId: String) {
    lobbies.forEach {lobby ->
        val user = lobby.users.find { user ->
            sessionId == user.sessionId
        }

        println("DISCONNECT: ${user?.userId}")

        user?.let { lobby.users.remove(user)
            deleteLobby(lobby)

            if (user.userId == lobby.creatorId) {
                val newCreator = lobby.users.random()
                lobby.creatorId = newCreator.userId
                println("NEW CREATOR ID ${lobby.creatorId}")

                sendResponse(newCreator.sessionId, userSessions, NewCreator(lobby.creatorId) as LobbyResponse)
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


private fun deleteLobby(lobby: Lobby) {
    if (lobby.users.isEmpty()) {
        println("DELETE: LOBBY_ID ${lobby.lobbyId}")
        lobbies.remove(lobby)
    }
}

fun findLobbyById(lobbyId: String?): Lobby? = lobbies.find { lobby ->
    lobbyId == lobby.lobbyId }

fun findUserById(userID: String): User? = lobby.users.find { user ->
    userID == user.userId }