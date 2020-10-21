package pl.sose1

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.lobby.Error
import model.lobby.LobbyResponse
import model.lobby.Registered
import pl.sose1.model.lobby.Create
import pl.sose1.model.lobby.Lobby
import pl.sose1.model.lobby.LobbyRequest
import pl.sose1.model.lobby.Register
import pl.sose1.model.user.User
import pl.sose1.model.user.UserSession
import pl.sose1.routes.lobby
import pl.sose1.routes.root
import java.time.Duration
import java.util.*
import kotlin.collections.LinkedHashSet

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

var lobbies: MutableList<Lobby> = mutableListOf()


@Suppress("unused") // Referenced in application.conf
fun Application.module() {

    install(ContentNegotiation) {
        json(
                contentType = ContentType.Application.Json,
                json = Json {
                    encodeDefaults = true
                    isLenient = true
                    allowSpecialFloatingPointValues = true
                    allowStructuredMapKeys = true
                    prettyPrint = true
                    useArrayPolymorphism = true
                }
        )
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
                            println("REQUEST: $text")

                            when (val request: LobbyRequest = Json.decodeFromString(text)) {
                                is Create ->
                                    createLobby(outgoing,
                                            User(request.userName,
                                                    userSession.id)
                                    )
                                is Register ->
                                    registerToLobby(outgoing,
                                            User(request.userName,
                                                    userSession.id),
                                            request.code
                                    )
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

suspend fun createLobby(outgoing: SendChannel<Frame>,
                        user: User) {
    val lobby = Lobby(user.userId)
    lobby.users.add(user)
    lobbies.add(lobby)

    println("CREATE: LOBBY_ID = ${lobby.lobbyId}")

    outgoing.sendJson(Registered(user, lobby.lobbyId, lobby.code, lobby.creatorId) as LobbyResponse)
}

suspend fun registerToLobby(outgoing: SendChannel<Frame>,
                            user: User,
                            code: String? ) {
    val lobby = findLobbyByCode(code)

    if (lobby != null) {
        lobby.users.add(user)
        println("REGISTER TO ${lobby.lobbyId}, USER: ${user.userId}")

        outgoing.sendJson(Registered(user, lobby.lobbyId, lobby.code, lobby.creatorId) as LobbyResponse)
    } else {
        outgoing.sendJson(Error(404) as LobbyResponse)
    }
}

fun findLobbyByCode(code: String?): Lobby? = lobbies.find { lobby ->
    code == lobby.code
}

suspend inline fun <reified T>SendChannel<Frame>.sendJson(body: T) {
    send(Frame.Text(Json.encodeToString(body)))
}