package com.rakshaai.security.ml.nlp

import android.content.Context
import android.util.JsonReader
import java.io.InputStreamReader
import kotlin.math.ln
import kotlin.math.sqrt

class TfidfVectorizer(
    private val vocabulary: Map<String, Int>,
    private val idf: DoubleArray,
    private val analyzer: String,
    private val ngramMin: Int,
    private val ngramMax: Int
) {

    companion object {

        fun load(
            context: Context,
            assetPath: String
        ): TfidfVectorizer {
            var analyzer = "word"
            var ngramMin = 1
            var ngramMax = 1
            val vocabulary = HashMap<String, Int>(50000)
            var idf = DoubleArray(0)

            context.assets.open(assetPath).use { inputStream ->
                JsonReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "analyzer" -> analyzer = reader.nextString()
                            "ngram_range" -> {
                                reader.beginArray()
                                if (reader.hasNext()) ngramMin = reader.nextInt()
                                if (reader.hasNext()) ngramMax = reader.nextInt()
                                while (reader.hasNext()) reader.skipValue()
                                reader.endArray()
                            }
                            "tokens" -> {
                                reader.beginArray()
                                var idx = 0
                                while (reader.hasNext()) {
                                    vocabulary[reader.nextString()] = idx++
                                }
                                reader.endArray()
                            }
                            "idf" -> {
                                val list = ArrayList<Double>(50000)
                                reader.beginArray()
                                while (reader.hasNext()) {
                                    list.add(reader.nextDouble())
                                }
                                reader.endArray()
                                idf = list.toDoubleArray()
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
            }

            return TfidfVectorizer(
                vocabulary = vocabulary,
                idf = idf,
                analyzer = analyzer,
                ngramMin = ngramMin,
                ngramMax = ngramMax
            )
        }
    }

    fun transform(text: String): Map<Int, Double> {

        val normalized = text
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()

        if (normalized.isEmpty()) {
            return emptyMap()
        }

        val tokens = if (analyzer == "word") {
            wordNgrams(normalized)
        } else {
            characterNgrams(normalized)
        }

        if (tokens.isEmpty()) {
            return emptyMap()
        }

        val counts = HashMap<Int, Int>()

        for (token in tokens) {
            val index = vocabulary[token] ?: continue
            counts[index] = (counts[index] ?: 0) + 1
        }

        if (counts.isEmpty()) {
            return emptyMap()
        }

        val values = HashMap<Int, Double>()
        var squaredSum = 0.0

        for ((index, count) in counts) {
            if (index in idf.indices) {
                val tf = 1.0 + ln(count.toDouble())
                val value = tf * idf[index]
                values[index] = value
                squaredSum += value * value
            }
        }

        val norm = sqrt(squaredSum)

        if (norm == 0.0) {
            return emptyMap()
        }

        val normalizedValues = HashMap<Int, Double>()
        for ((index, value) in values) {
            normalizedValues[index] = value / norm
        }

        return normalizedValues
    }

    private fun wordNgrams(text: String): List<String> {

        val regex = Regex("""\b[\p{L}\p{N}_]+\b""")

        val words = regex
            .findAll(text)
            .map { it.value }
            .toList()

        val result = ArrayList<String>()

        if (ngramMin <= 1) {
            result.addAll(words)
        }

        if (ngramMax >= 2) {
            for (i in 0 until words.size - 1) {
                result.add("${words[i]} ${words[i + 1]}")
            }
        }

        return result
    }

    private fun characterNgrams(text: String): List<String> {

        val result = ArrayList<String>()

        for (n in ngramMin..ngramMax) {

            if (text.length < n) {
                continue
            }

            for (i in 0..text.length - n) {
                result.add(text.substring(i, i + n))
            }
        }

        return result
    }
}
