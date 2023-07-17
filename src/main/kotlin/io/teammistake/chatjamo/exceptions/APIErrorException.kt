package io.teammistake.chatjamo.exceptions

import io.teammistake.chatjamo.dto.APIError
import java.lang.RuntimeException

class APIErrorException(
    val err: APIError
): RuntimeException("API Error: $err") {}