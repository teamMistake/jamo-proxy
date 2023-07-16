package io.teammistake.chatjamo.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

open class SuzumeStreamingResponse;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy::class)
class InferenceResponse(
    val respPartial: String?,
    val respFull: String?,
    val eos: Boolean,
    val error: String? = null
): SuzumeStreamingResponse();

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy::class)
data class APIResponse(
    val reqId: String,
    val model: String,
    val resp: String?
)

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy::class)
class APIResponseHeader(
    val reqId: String,
    val model: String
): SuzumeStreamingResponse();