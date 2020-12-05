package pl.sose1.application.utils

import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

suspend inline fun <reified T> SendChannel<Frame>.sendJson(body: T) {
    send(Frame.Text(Json.encodeToString(body)))
}