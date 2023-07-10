package io.teammistake.chatjamo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ChatjamoApplication

fun main(args: Array<String>) {
	runApplication<ChatjamoApplication>(*args)
}
