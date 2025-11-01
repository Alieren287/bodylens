package com.progresstracker.bodylens.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.progresstracker.bodylens.BuildConfig
import java.io.File

/**
 * Service for interacting with Google Gemini AI API
 */
class GeminiService {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-flash-latest",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    /**
     * Compare two sets of body progress photos and provide analysis
     *
     * @param olderPhotos List of file paths from the older session
     * @param newerPhotos List of file paths from the newer session
     * @return AI-generated comparison analysis
     */
    suspend fun compareProgressPhotos(
        olderPhotos: List<String>,
        newerPhotos: List<String>
    ): Result<String> {
        return try {
            // Load images as bitmaps
            val olderBitmaps = olderPhotos.mapNotNull { loadBitmap(it) }
            val newerBitmaps = newerPhotos.mapNotNull { loadBitmap(it) }

            if (olderBitmaps.isEmpty() || newerBitmaps.isEmpty()) {
                return Result.failure(Exception("Failed to load photos"))
            }

            // Create prompt for comparison
            val prompt = buildComparisonPrompt()

            // Build content with images
            val content = content {
                text(prompt)
                text("\n\nOlder photos (before):")
                olderBitmaps.forEach { bitmap ->
                    image(bitmap)
                }
                text("\n\nNewer photos (after):")
                newerBitmaps.forEach { bitmap ->
                    image(bitmap)
                }
            }

            // Generate response
            val response = generativeModel.generateContent(content)
            val analysisText = response.text ?: "No analysis available"

            Result.success(analysisText)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Analyze a single photo session and provide feedback
     *
     * @param photos List of file paths from the session
     * @return AI-generated analysis
     */
    suspend fun analyzeSingleSession(photos: List<String>): Result<String> {
        return try {
            val bitmaps = photos.mapNotNull { loadBitmap(it) }

            if (bitmaps.isEmpty()) {
                return Result.failure(Exception("Failed to load photos"))
            }

            val prompt = """
                Analyze these body progress photos. Provide feedback on:
                1. Photo quality (lighting, pose consistency, clarity)
                2. Visible muscle definition
                3. Suggestions for better photo taking

                Keep the response concise and constructive.
                Keep it short and to the point
            """.trimIndent()

            val content = content {
                text(prompt)
                bitmaps.forEach { bitmap ->
                    image(bitmap)
                }
            }

            val response = generativeModel.generateContent(content)
            val analysisText = response.text ?: "No analysis available"

            Result.success(analysisText)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate a progress summary for a date range
     *
     * @param sessionPhotos Map of session date to photo paths
     * @return AI-generated progress summary
     */
    suspend fun generateProgressSummary(
        sessionPhotos: Map<Long, List<String>>
    ): Result<String> {
        return try {
            // Limit to first and last session for comparison
            val sortedSessions = sessionPhotos.entries.sortedBy { it.key }
            if (sortedSessions.size < 2) {
                return Result.failure(Exception("Need at least 2 sessions for progress summary"))
            }

            val firstSession = sortedSessions.first()
            val lastSession = sortedSessions.last()

            val firstBitmaps = firstSession.value.mapNotNull { loadBitmap(it) }
            val lastBitmaps = lastSession.value.mapNotNull { loadBitmap(it) }

            val prompt = """
                Generate a progress summary comparing these photos from different time periods.

                Number of sessions in period: ${sortedSessions.size}
                Time span: ${calculateDaysBetween(firstSession.key, lastSession.key)} days

                Analyze:
                1. Overall progress and changes
                2. Specific areas showing improvement
                3. Motivational insights
                4. Recommendations for continued progress

                Be encouraging and specific.
            """.trimIndent()

            val content = content {
                text(prompt)
                text("\n\nStarting photos:")
                firstBitmaps.forEach { bitmap ->
                    image(bitmap)
                }
                text("\n\nCurrent photos:")
                lastBitmaps.forEach { bitmap ->
                    image(bitmap)
                }
            }

            val response = generativeModel.generateContent(content)
            val summaryText = response.text ?: "No summary available"

            Result.success(summaryText)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Build the comparison prompt for photo analysis
     */
    private fun buildComparisonPrompt(): String {
        return """
            You are a fitness progress analyst. Compare these two sets of body progress photos.

            Analyze and describe:
            1. Visible changes in muscle definition or body composition
            2. Specific areas showing improvement (e.g., shoulders, chest, arms, core, legs)
            3. Any noticeable changes in posture or body shape
            4. Motivational insights based on the progress

            Guidelines:
            - Be specific about which body parts show changes
            - Be encouraging and constructive
            - Keep the tone professional and supportive
            - If changes are minimal, acknowledge the effort and suggest continued consistency
            - Avoid medical advice or specific numbers/measurements

            Format your response in clear sections.
        """.trimIndent()
    }

    /**
     * Load bitmap from file path
     */
    private fun loadBitmap(filePath: String): Bitmap? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                // Scale down large images to save memory and API costs
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2 // Reduce size by half
                }
                BitmapFactory.decodeFile(filePath, options)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculate days between two timestamps
     */
    private fun calculateDaysBetween(startMillis: Long, endMillis: Long): Long {
        return (endMillis - startMillis) / (1000 * 60 * 60 * 24)
    }
}
