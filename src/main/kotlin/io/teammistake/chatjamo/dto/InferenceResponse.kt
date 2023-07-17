package io.teammistake.chatjamo.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(name="header", value=APIResponseHeader::class),
    JsonSubTypes.Type(name="response", value=InferenceResponse::class)
)
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