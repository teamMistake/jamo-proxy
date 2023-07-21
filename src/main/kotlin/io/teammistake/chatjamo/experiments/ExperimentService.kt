package io.teammistake.chatjamo.experiments

import io.teammistake.chatjamo.database.Experiment
import io.teammistake.chatjamo.dto.APIInferenceRequest
import io.teammistake.chatjamo.dto.ContextPart
import io.teammistake.chatjamo.experiments.configuration.ExperimentConfiguration
import io.teammistake.chatjamo.experiments.configuration.ExperimentProperty
import io.teammistake.chatjamo.service.getUser
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators.Exp
import org.springframework.stereotype.Service
import java.util.Random
import java.util.TreeMap
import java.util.function.Function
import kotlin.reflect.KFunction1

@Service
@EnableConfigurationProperties(value = [ExperimentProperty::class])
class ExperimentService {
    @Autowired
    lateinit var config: ExperimentProperty;

    var max = 0;

    val types: Map<String, KFunction1<ExperimentConfiguration, ExperimentStrategy>> = mapOf(
        "normal" to ::NormalStrategy,
        "samemodel" to ::SameModelStrategy
    );

    val default: ExperimentStrategy by lazy {
        types[config.default.type]?.call(config.default) ?: throw IllegalStateException("Invalid type for default: ${config.default.type}")
    }

    val experiments by lazy {
        var culSum = 0
        val treeMap: TreeMap<Int, ExperimentStrategy> = TreeMap();
        config.config.forEach {
            culSum += it.weight
            treeMap.put(culSum, types[it.type]?.call(it) ?: throw IllegalStateException("Invalid type: ${it.type}"))
        }
        max = culSum
        treeMap
    }

    val random = Random()

    suspend fun generate(req: String, context: List<ContextPart>): Pair<Experiment, List<APIInferenceRequest>> {
        if (getUser() != null) {
            val strategy = experiments.ceilingEntry(random.nextInt(max)+1).value
            return strategy.generateModels(req, context)
        } else {
            val strategy = default
            return strategy.generateModels(req, context)
        }
    }

    suspend fun generateRegenerate(req: String, context: List<ContextPart>): Pair<Experiment, List<APIInferenceRequest>> {
        return default.generateModels(req, context)
    }
}