package io.teammistake.chatjamo.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

class SwaggerCo

@Configuration
@EnableSwagger2
class SwaggerConfig {
    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
            .useDefaultResponseMessages(false)
            .consumes(consumeContentTypes)
            .produces(produceContentTypes)
            .apiInfo(apiInfo())
            .select()
            .apis(RequestHandlerSelectors.basePackage("io.teammistake.chatjamo"))
            .paths(PathSelectors.any())
            .build()
    }

    private val consumeContentTypes: Set<String>
        private get() {
            val consumeContentTypes: MutableSet<String> = HashSet()
            consumeContentTypes.add("application/json;charset=UTF-8")
            consumeContentTypes.add("application/x-www-form-urlencoded")
            return consumeContentTypes
        }
    private val produceContentTypes: Set<String>
        private get() {
            val produceContentTypes: MutableSet<String> = HashSet()
            produceContentTypes.add("application/json;charset=UTF-8")
            produceContentTypes.add("text/event-stream;charset=UTF-8")
            produceContentTypes.add("application/x-ndjson;charset=UTF-8")
            return produceContentTypes
        }

    private fun apiInfo(): ApiInfo {
        return ApiInfoBuilder()
            .title("Jamo-Proxy")
            .description("A gatekeeper in front of suzume")
            .build()
    }
}