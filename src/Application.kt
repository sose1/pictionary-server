package pl.sose1

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.SendChannel
import pl.sose1.model.lobby.Lobby
import pl.sose1.model.lobby.LobbyRequestEventName
import pl.sose1.model.lobby.LobbyRequestMessage
import pl.sose1.model.lobby.toJSON
import pl.sose1.model.user.User
import pl.sose1.model.user.UserSession
import java.time.Duration
import java.util.*
import kotlin.collections.LinkedHashSet

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

val mapper = jacksonObjectMapper()
var lobbies: MutableList<Lobby> = mutableListOf()


@Suppress("unused") // Referenced in application.conf
fun Application.module() {

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            setDefaultPrettyPrinter ( DefaultPrettyPrinter (). apply {
                indentArraysWith ( DefaultPrettyPrinter.FixedSpaceIndenter .instance)
                indentObjectsWith ( DefaultIndenter ( "   " , " \n " ))
            })
        }
    }
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
// TODO: 30.09.2020 Walidacja nazw użyktonika, zeby nie było przypadku kiedy jest kilu tych samych użytkowników
// TODO: 30.09.2020 Usuwanie gracza z lobby gdy wyłacza sie aplikacja
// TODO: 30.09.2020 Usuwanie gracza jak wyjdzie z activity

    routing {
        this.root()

        val userSessions = Collections.synchronizedSet(LinkedHashSet<UserSession>())
        webSocket("/lobby") {
            val userSession = UserSession(this)
            userSessions += userSession

            try {
                while (true) {
                    when(val frame = incoming.receive()) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            val lobbyRequestMessage: LobbyRequestMessage = mapper.readValue(text)

                            val user = User(userSession.id, lobbyRequestMessage.userName)
                            println(user.name + " " + user.userID)

                            when(lobbyRequestMessage.eventName) {
                                LobbyRequestEventName.CREATE_LOBBY.name ->
                                    createLobby(outgoing, user)
                                LobbyRequestEventName.CONNECT_TO_LOBBY.name ->
                                    connectToLobby(lobbyRequestMessage.code, user, userSessions)
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
}

suspend fun createLobby(outgoing: SendChannel<Frame>, user: User) {
    val lobby = Lobby()
    lobby.users.add(user)
    lobbies.add(lobby)

    outgoing.send(Frame.Text(lobby.toJSON()))
}

suspend fun connectToLobby(code: String?, user: User, userSessions: MutableSet<UserSession>, ) {
    val lobby = findLobbyByCode(code)
    lobby?.users?.add(user)
    lobby?.users?.forEach {sendResponse(it.userID, userSessions, lobby.toJSON()) }
}

suspend fun disconnectUser(userSessions: MutableSet<UserSession>, id: UUID) {
    lobbies.forEach {lobby ->
        val user = lobby.users.find { user ->
            id == user.userID
        }
        user?.let { lobby.users.remove(user)
            lobby.users.forEach { sendResponse(it.userID, userSessions, lobby.toJSON()) }
        }
    }
}

suspend fun sendResponse(id: UUID, userSessions: MutableSet<UserSession>, response: String) {
    userSessions.find { userSession ->
        id == userSession.id
    }?.session?.send(Frame.Text(response))
}

fun findLobbyByCode(code: String?): Lobby? = lobbies.find { lobby -> code == lobby.code }
fun findUserInLobby(id: UUID?, code: String?): User? = findLobbyByCode(code)?.users
        ?.find { user ->
            id == user.userID
        }