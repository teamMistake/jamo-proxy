package io.teammistake.chatjamo.configuration

import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate


@Configuration
class MongoConfiguration: AbstractReactiveMongoConfiguration() {

    @Value("\${mongo.url}")
    lateinit var mongoUrl: String;

    @Value("\${mongo.database}")
    lateinit var mongoDatabaseName: String;

    @Bean
    override fun reactiveMongoClient(): MongoClient {
        return MongoClients
            .create(mongoUrl)
    }

    override fun getDatabaseName(): String {
        return mongoDatabaseName
    }


    @Bean
    fun reactiveMongoTemplate(mongoClient: MongoClient): ReactiveMongoTemplate? {
        return ReactiveMongoTemplate(mongoClient, mongoDatabaseName)
    }
}