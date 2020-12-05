package pl.sose1.application

import application.model.user.UserSession
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.sessions.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.Koin
import pl.sose1.application.route.game
import pl.sose1.application.route.root
import pl.sose1.di.mainModule
import java.time.Duration
import java.util.*

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
    install(Koin) {
        modules(
                mainModule
        )
    }
    install(Sessions) {
        cookie<UserSession>("Pictionary-Session")
    }

    intercept(ApplicationCallPipeline.Features) {
        if (call.sessions.get<UserSession>() == null) {
            call.sessions.set(UserSession(UUID.randomUUID().toString()))
        }
    }

    routing {
        game()
        root()
    }
}