package io.teammistake.chatjamo.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy::class)
data class JamoAPIError(
    val error: String?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val data: Any? = null
): MessageEventData()

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ResponseGenerationError(
    val chatId: String,
    val messageId: String?,
    val reqId: String?,
    val error: String?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val data: Any? = null
): MessageEventData()