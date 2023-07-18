package io.teammistake.chatjamo.service

import io.teammistake.chatjamo.database.UserRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.kotlin.core.publisher.toFlux

@Service
class LeaderboardService {
    @Autowired lateinit var redisTemplate: ReactiveRedisTemplate<String, String>;
    @Autowired lateinit var userRepository: UserRepository;
    val redisSsetOps by lazy {
        redisTemplate.opsForZSet();
    }
    suspend fun addScore(userId: String, score: Double): Double {
        return redisSsetOps.incrementScore("leaderboard_1", userId, score).awaitSingle();
    }

    data class Position(
        var username: String?,
        val rank: Long?, // is 0 indexed
        val score: Double
    )
    suspend fun getPosition(userId: String): Position {
        val rank = redisSsetOps.reverseRank("leaderboard_1", userId).awaitSingleOrNull();
        val score = redisSsetOps.score("leaderboard_1", userId).awaitSingleOrNull() ?: 0.0
        val user = userRepository.findById(userId).awaitSingleOrNull();

        return Position(user?.name, rank, score);
    }

    suspend fun getLeaderboard(from: Int, cnt: Int): List<Position> {
        if (cnt > 50) throw IllegalArgumentException("Count can not be bigger than 50")
        if (cnt <= 0) throw IllegalArgumentException("Count must be natural number")
        if (from < 0) throw IllegalArgumentException("From index starts from 0")

        val partialList = redisSsetOps.reverseRangeWithScores("leaderboard_1", Range.closed(from.toLong(), from + cnt-1L))
            .asFlow()
            .withIndex()
            .map{
                Position(it.value.value, it.index + from.toLong(), it.value.score ?: -1.0)
            }.toList();
        val map = partialList.map { it.username to it }.toMap();

        userRepository.findAllById(partialList.map { it.username }.filterNotNull())
            .asFlow()
            .map {
                map[it.userId]?.username = it.name
            }.collect()

        return partialList;
    }
}