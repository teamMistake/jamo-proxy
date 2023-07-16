package io.teammistake.chatjamo.exceptions

import java.lang.RuntimeException

class NotFoundException(msg: String): RuntimeException(msg) {
}