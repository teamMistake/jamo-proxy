package io.teammistake.chatjamo.database

import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface ChatRepository: ReactiveMongoRepository<Chat, String> {
    @Query
    fun findAllByUserId(userId: String): Flux<LightChat> ;
}