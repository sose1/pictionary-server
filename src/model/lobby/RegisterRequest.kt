package pl.sose1.model.lobby


data class RegisterRequest(val userName: String, val eventName: RequestEventName, val code: String? = null)
