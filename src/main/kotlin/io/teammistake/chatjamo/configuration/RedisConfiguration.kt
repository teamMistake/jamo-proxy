package io.teammistake.chatjamo.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.RedisTemplate


@Configuration
class RedisConfiguration {

    @Value("\${redis.host}")
    lateinit var host: String;

    @Value("\${redis.port}")
    lateinit var port: String;
    @Bean
    fun lettuceConnectionFactory(): LettuceConnectionFactory? {
        return LettuceConnectionFactory(host, port.toInt())
    }
}