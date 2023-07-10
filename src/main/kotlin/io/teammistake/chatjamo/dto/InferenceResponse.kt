package io.teammistake.suzume.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy::class)
data class InferenceResponse(
    val respPartial: String?,
    val respFull: String?,
    val eos: Boolean,
    val error: String? = null
)

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy::class)
data class APIResponse(
    val reqId: String,
    val model: String,
    val resp: String?
)

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy::class)
data class APIResponseHeader(
    val reqId: String,
    val model: String
)