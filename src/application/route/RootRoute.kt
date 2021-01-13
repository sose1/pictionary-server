package pl.sose1.application.route

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.koin.ktor.ext.inject
import pl.sose1.application.controller.GameController

fun Routing.root() {
    val gameController: GameController by inject()

    get("/game/empty") {
        gameController.getEmptyGame(context)
    }

    get("/game/code/{code}") {
        val code = this.call.parameters["code"] ?: throw Exception()
        gameController.getGameByCode(code, context)
    }

    get("/game/{id}") {
        val id = this.call.parameters["id"] ?: throw Exception()
        gameController.getGameById(id, context)
    }

    get("/") {
        call.respondText("Witaj")
    }
}