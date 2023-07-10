package io.teammistake.chatjamo.database

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository: ReactiveMongoRepository<User, String> {
}