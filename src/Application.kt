package pl.sose1

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.websocket.*
import java.time.Duration
import java.util.*
import kotlin.collections.LinkedHashSet

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
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

        webSocket("/ws") {
            val client = Client(this)
            clients += client
            try {
                while (true) {
                    val frame = incoming.receive()
                    when(frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            val textToSend = "${client.name} Napisał: $text"
                            outgoing.send(Frame.Text("Napisałes: $text"))
                            for (client in clients.toList()) {
                                client.session.outgoing.send(Frame.Text(textToSend))
                            }
                        }
                    }
                }
            } finally {
                clients -= client
            }
        }
    }
}


