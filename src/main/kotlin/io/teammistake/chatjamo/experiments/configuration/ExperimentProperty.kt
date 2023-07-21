package io.teammistake.chatjamo.experiments.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

@ConfigurationProperties("experiment")
class ExperimentProperty(
    var config: List<ExperimentConfiguration>,
    var default: ExperimentConfiguration
)

data class ExperimentConfiguration(
    var name: String,
    var weight: Int,
    var type: String,
    var data: Map<String, Any>
)