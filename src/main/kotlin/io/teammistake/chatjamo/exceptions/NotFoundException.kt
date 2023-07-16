package io.teammistake.suzume.exception

import java.lang.RuntimeException

class NotFoundException(msg: String): RuntimeException(msg) {
}