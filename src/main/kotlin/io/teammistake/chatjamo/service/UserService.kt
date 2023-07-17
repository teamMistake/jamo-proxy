package io.teammistake.chatjamo.service

import io.teammistake.chatjamo.database.User
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service

@Service
class UserService {
    @Autowired
    lateinit var mongoTemplate: ReactiveMongoTemplate;
    suspend fun incrementSentMessages(uid: String, cnt: Int): Boolean {
        val query = Query(Criteria.where("userId").`is`(uid))
        val update = Update().inc("sentMessages",cnt);
        val res = mongoTemplate.updateFirst(query,update, User::class.java).awaitSingle()
        return res.modifiedCount > 0
    }
    suspend fun incrementReceivedMessages(uid: String, cnt: Int): Boolean {
        val query = Query(Criteria.where("userId").`is`(uid))
        val update = Update().inc("receivedMessages",cnt);
        val res = mongoTemplate.updateFirst(query,update, User::class.java).awaitSingle()
        return res.modifiedCount > 0
    }

}