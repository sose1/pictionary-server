package pl.sose1.di

import org.koin.dsl.module
import pl.sose1.application.controller.GameController
import pl.sose1.application.publisher.SocketEventPublisher
import pl.sose1.domain.`interface`.EventPublisher
import pl.sose1.domain.`interface`.GameRepository
import pl.sose1.domain.`interface`.SessionRepository
import pl.sose1.domain.`interface`.UserRepository
import pl.sose1.domain.service.GameService
import pl.sose1.infrastructure.repository.CacheGameRepository
import pl.sose1.infrastructure.repository.CacheSessionRepository
import pl.sose1.infrastructure.repository.CacheUserRepository

val mainModule = module {
    single<GameRepository> { CacheGameRepository() }
    single<UserRepository> { CacheUserRepository() }
    single<SessionRepository> { CacheSessionRepository() }
    single<EventPublisher> { SocketEventPublisher(get(), get()) }
    single { GameService(get(), get(), get()) }
    single { GameController(get(), get()) }
}