package io.teammistake.chatjamo.exceptions

import io.teammistake.chatjamo.dto.APIError
import java.lang.RuntimeException

class APIErrorException(
    val err: APIError,
    val chatId: String,
    val messageId: String
): RuntimeException("API Error: $err") {}