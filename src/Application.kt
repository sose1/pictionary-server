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
import pl.sose1.model.lobby.LobbyRequestEventName
import pl.sose1.model.lobby.LobbyRequestMessage
import pl.sose1.model.lobby.toJSON
import pl.sose1.repository.Client
import java.time.Duration
import java.util.*
import kotlin.collections.LinkedHashSet

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

val mapper = jacksonObjectMapper()


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            setDefaultPrettyPrinter ( DefaultPrettyPrinter (). apply {
                indentArraysWith ( DefaultPrettyPrinter.FixedSpaceIndenter .instance)
                indentObjectsWith ( DefaultIndenter ( "   " , " \n " ))
            })
        }
    }
    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        this.root()

        val clients = Collections.synchronizedSet(LinkedHashSet<Client>())
        webSocket("/lobby") {
            val client = Client(this)
            println(client.name)

            clients += client

            try {
                while (true) {
                    val frame = incoming.receive()
                    when(frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            val lobbyRequestMessage: LobbyRequestMessage = mapper.readValue(text)

//                            val textToSend = "$text"
                            when(lobbyRequestMessage.eventName) {
                                LobbyRequestEventName.CREATE_LOBBY.name ->
                                    createLobby(outgoing, lobbyRequestMessage)
                                LobbyRequestEventName.CONNECT_TO_LOBBY.name ->
                                    connectToLobby(outgoing, lobbyRequestMessage)
                            }
//                            clients.forEach {it.session.outgoing.send(Frame.Text(textToSend)) }

                        }
                    }
                }
            } finally {
                clients -= client
            }
        }
    }
}

suspend fun connectToLobby(outgoing: SendChannel<Frame>, lobbyRequestMessage: LobbyRequestMessage) {
    outgoing.send(Frame.Text(lobbyRequestMessage.toJSON()))
}

suspend fun createLobby(outgoing: SendChannel<Frame>, lobbyRequestMessage: LobbyRequestMessage) {
    outgoing.send(Frame.Text(lobbyRequestMessage.toJSON()))
}

