package com.rakshaai.security.ml.nlp

import android.content.Context
import android.util.JsonReader
import java.io.InputStreamReader

class LinearSvmModel(
    private val coefficients: DoubleArray,
    private val intercept: Double
) {

    companion object {

        fun load(
            context: Context,
            assetPath: String
        ): LinearSvmModel {
            var intercept = 0.0
            var coefficients = DoubleArray(0)

            context.assets.open(assetPath).use { inputStream ->
                JsonReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "intercept" -> intercept = reader.nextDouble()
                            "coefficients" -> {
                                val list = ArrayList<Double>(100000)
                                reader.beginArray()
                                while (reader.hasNext()) {
                                    list.add(reader.nextDouble())
                                }
                                reader.endArray()
                                coefficients = list.toDoubleArray()
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
            }

            return LinearSvmModel(
                coefficients = coefficients,
                intercept = intercept
            )
        }
    }

    fun decisionScore(
        wordFeatures: Map<Int, Double>,
        characterFeatures: Map<Int, Double>
    ): Double {

        var score = intercept

        // Word features: 0 to 99,999
        for ((index, value) in wordFeatures) {
            if (index in coefficients.indices) {
                score += coefficients[index] * value
            }
        }

        // Character features: 100,000 to 199,999
        val characterOffset = 100_000

        for ((index, value) in characterFeatures) {
            val combinedIndex = characterOffset + index
            if (combinedIndex in coefficients.indices) {
                score += coefficients[combinedIndex] * value
            }
        }

        return score
    }

    fun isScam(score: Double): Boolean {
        return score >= 0.0
    }
}
