package pl.sose1.routes

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

fun Routing.root() {
    get("/") {
        call.respondText("HELLO WORLD!", ContentType.Text.Plain)
    }
}