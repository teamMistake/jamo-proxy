package io.teammistake.chatjamo.service

import io.teammistake.suzume.data.APIInferenceRequest
import io.teammistake.suzume.data.SuzumeStreamingResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.exchangeToFlow

class SuzumeService {
    @Autowired
    lateinit var webClientBuilder: WebClient.Builder;

    @Value("#{environments.SUZUME_URL}")
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
                it.bodyToFlow()
            };
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