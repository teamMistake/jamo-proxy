package io.teammistake.chatjamo.experiments

import io.teammistake.chatjamo.database.Experiment
import io.teammistake.chatjamo.database.Normal
import io.teammistake.chatjamo.database.SameModel
import io.teammistake.chatjamo.dto.APIInferenceRequest
import io.teammistake.chatjamo.dto.ContextPart
import io.teammistake.chatjamo.experiments.configuration.ExperimentConfiguration
import java.util.*

class SameModelStrategy(val experimentConfiguration: ExperimentConfiguration): ExperimentStrategy {
    data class ModelAndWeight(val model: String, val weight: Int) {
        companion object {
            fun parse(map: Map<String, Any>): ModelAndWeight {
                return ModelAndWeight((map["model"] as String), (map["weight"] as Int))
            }
        }
    }

    val models: TreeMap<Int, ModelAndWeight> = TreeMap();
    var max = 0;

    val maxToken = (experimentConfiguration.data["maxToken"] as Int)
    val topK = (experimentConfiguration.data["topK"] as Int)
    val temperature = (experimentConfiguration.data["temperature"] as Double)
    val cnt = (experimentConfiguration.data["count"] as Int)
    init {
        (experimentConfiguration.data["models"] as Map<*,*>)
            .map { it.value as Map<String, Objects> }
            .forEach {
                val weight = ModelAndWeight.parse(it)
                max += weight.weight

                models[max] = weight
            }
    }

    val random = Random()
    override fun generateModels(req: String, context: List<ContextPart>): Pair<Experiment, List<APIInferenceRequest>> {
        val thing = models.ceilingEntry(random.nextInt(max)+1)
        val reqs = mutableListOf<APIInferenceRequest>()

        for (i in 1..cnt) {
            reqs.add(
                APIInferenceRequest(req, context, true, maxToken, thing.value.model, temperature, topK)
            )
        }
        return Pair(SameModel(), reqs)
    }
}