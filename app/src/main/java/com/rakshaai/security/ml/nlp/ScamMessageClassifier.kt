package com.rakshaai.security.ml.nlp

import android.content.Context

data class ScamClassificationResult(
    val score: Double,
    val isScam: Boolean,
    val label: String
)

class ScamMessageClassifier(
    private val wordVectorizer: TfidfVectorizer,
    private val characterVectorizer: TfidfVectorizer,
    private val svmModel: LinearSvmModel
) {

    companion object {

        fun load(context: Context): ScamMessageClassifier {

            val wordVectorizer =
                TfidfVectorizer.load(
                    context,
                    "model/word_tfidf.json"
                )

            val characterVectorizer =
                TfidfVectorizer.load(
                    context,
                    "model/character_tfidf.json"
                )

            val svmModel =
                LinearSvmModel.load(
                    context,
                    "model/linear_svm.json"
                )

            return ScamMessageClassifier(
                wordVectorizer = wordVectorizer,
                characterVectorizer = characterVectorizer,
                svmModel = svmModel
            )
        }
    }

    fun classify(text: String): ScamClassificationResult {

        val wordFeatures =
            wordVectorizer.transform(text)

        val characterFeatures =
            characterVectorizer.transform(text)

        val score =
            svmModel.decisionScore(
                wordFeatures = wordFeatures,
                characterFeatures = characterFeatures
            )

        val isScam =
            svmModel.isScam(score)

        return ScamClassificationResult(
            score = score,
            isScam = isScam,
            label = if (isScam) {
                "SCAM"
            } else {
                "BENIGN"
            }
        )
    }
}