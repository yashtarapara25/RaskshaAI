package com.rakshaai.security.ml.nlp

import android.content.Context
import android.util.Log
import com.rakshaai.security.domain.engine.LocalDetectionEngine

object ModelVerification {

    private const val TAG = "RakshaAI_MODEL"

    suspend fun run(context: Context) {

        Log.d(TAG, "===== MODEL VERIFICATION =====")

        try {

            Log.d(TAG, "Step 1: Initializing LocalDetectionEngine & ML classifier...")

            LocalDetectionEngine.initMlClassifier(context)

            val classifier = ScamMessageClassifier.load(context)

            Log.d(TAG, "Step 2: Classifier loaded successfully")

            val testMessages = listOf(
                "Your bank account will be blocked today. Complete KYC immediately using this link.",
                "Congratulations! You have won a lottery prize. Pay the processing fee to claim it.",
                "Hey, are we still meeting for dinner tonight?",
                "Your OTP is 123456. Do not share it with anyone.",
                "Your electricity bill is due tomorrow. Please pay through the official app.",
                "You have been selected for a work from home job. Pay the registration fee now."
            )

            for ((index, message) in testMessages.withIndex()) {

                Log.d(TAG, "Starting Test ${index + 1}")

                val result = classifier.classify(message)

                Log.d(
                    TAG,
                    "Test ${index + 1}: Score=${result.score}, Label=${result.label}"
                )
            }

            Log.d(TAG, "===== VERIFICATION COMPLETE =====")

        } catch (e: Exception) {

            Log.e(
                TAG,
                "===== MODEL VERIFICATION FAILED =====",
                e
            )
        }
    }
}
