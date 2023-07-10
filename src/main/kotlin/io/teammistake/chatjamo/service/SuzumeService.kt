package io.teammistake.chatjamo.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.client.WebClient

class SuzumeService {
    @Autowired
    lateinit var webClientBuilder: WebClient.Builder;

    suspend fun generateResponse() {

    }
}