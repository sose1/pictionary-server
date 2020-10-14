package pl.sose1

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
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
import pl.sose1.model.error.ErrorResponse
import pl.sose1.model.lobby.Lobby
import pl.sose1.model.lobby.LobbyConnectResponse
import pl.sose1.model.lobby.RegisterRequest
import pl.sose1.model.lobby.RequestEventName
import pl.sose1.model.user.User
import pl.sose1.model.user.UserSession
import pl.sose1.routes.lobby
import pl.sose1.routes.root
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

    routing {
        this.root()
        this.lobby()

        val userSessions = Collections.synchronizedSet(LinkedHashSet<UserSession>())

        webSocket("/lobby") {
            val userSession = UserSession(this)
            userSessions += userSession

            try {
                while (true) {
                    when (val frame = incoming.receive()) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            val request: RegisterRequest = mapper.readValue(text)

                            val user = User(request.userName, userSession.id)

                            when (request.eventName) {
                                RequestEventName.CREATE_LOBBY ->
                                    createLobby(outgoing, user)
                                RequestEventName.REGISTER_TO_LOBBY ->
                                    registerToLobby(request.code, user, outgoing)
                            }
                        }
                    }
                }
            } finally {
                userSessions -= userSession
            }
        }
    }
}

suspend fun createLobby(outgoing: SendChannel<Frame>, user: User) {
    val lobby = Lobby(user.userId)
    lobby.users.add(user)
    lobbies.add(lobby)

    println("${user.name} created lobby: lobbyId = ${lobby.lobbyId}")

    outgoing.send(Frame.Text(LobbyConnectResponse(user, lobby.lobbyId, lobby.code, lobby.creatorId).toJSON()))
}

suspend fun registerToLobby(code: String?, user: User, outgoing: SendChannel<Frame> ) {
    val lobby = findLobbyByCode(code)

    if (lobby != null) {
        lobby.users.add(user)
        println("${user.name} registered to lobby: lobbyId = ${lobby.lobbyId}")

        outgoing.send(Frame.Text(LobbyConnectResponse(user, lobby.lobbyId, lobby.code, lobby.creatorId).toJSON()))
    }
}

fun findLobbyByCode(code: String?): Lobby? = lobbies.find { lobby -> code == lobby.code }

fun Any.toJSON(): String = ObjectMapper().writeValueAsString(this)