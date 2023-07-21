package io.teammistake.chatjamo.experiments

import io.teammistake.chatjamo.database.Experiment
import io.teammistake.chatjamo.dto.APIInferenceRequest
import io.teammistake.chatjamo.dto.ContextPart

interface ExperimentStrategy {
    fun generateModels(req: String, context: List<ContextPart>): Pair<Experiment, List<APIInferenceRequest>>
}