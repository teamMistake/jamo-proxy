package io.teammistake.chatjamo.service

import io.teammistake.chatjamo.dto.APIInferenceRequest
import io.teammistake.chatjamo.dto.SuzumeStreamingResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.exchangeToFlow
import kotlin.time.Duration.Companion.minutes

@Service
class SuzumeService {
    @Autowired
    lateinit var webClientBuilder: WebClient.Builder;

    @Value("#{environment.SUZUME_URL}")
    lateinit var suzume: String;

    fun generateResponse(apiInferenceRequest: APIInferenceRequest): Flow<SuzumeStreamingResponse> {
        return webClientBuilder
            .baseUrl(suzume)
            .build()
            .post()
            .uri("/generate")
            .accept(MediaType.APPLICATION_NDJSON)
            .bodyValue(apiInferenceRequest)
            .exchangeToFlow {
                it.bodyToFlow(SuzumeStreamingResponse::class)
            }
            .timeout(1.minutes)
    }

    suspend fun feedback(reqId: String, score: Double) {
        data class Score(val score: Double);
        val resp = webClientBuilder
            .baseUrl(suzume)
            .build()
            .put()
            .uri("/requests/$reqId/feedback")
            .bodyValue(Score(score))
            .exchangeToMono { it.toBodilessEntity() }
            .awaitSingle();
        if (!resp.statusCode.is2xxSuccessful) throw RuntimeException("Suzeme threw ${resp.statusCode}")
    }
}