/* Sanitized regression logic selectively extracted from user-authorized source commit 843d93f. */
package org.sakos.camera.safety.opennsfw2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SakosCompatibleScoreMappingTest {
    @Test
    fun nsfwLeadingScoresMapToBlockedResult() {
        val result = SakosCompatibleResultMapper.toCheckResult(
            SakosCompatibleModelScores(
                sfwProbability = 0.120f,
                nsfwProbability = 0.880f,
            ),
        )

        assertEquals(false, result.isSafe)
        assertEquals(0.880f, result.nsfwProbability)
        assertTrue(result.reason.contains("nsfw_prob=0.880"))
    }

    @Test
    fun sfwLeadingScoresMapToSafeResult() {
        val result = SakosCompatibleResultMapper.toCheckResult(
            SakosCompatibleModelScores(
                sfwProbability = 0.920f,
                nsfwProbability = 0.080f,
            ),
        )

        assertEquals(true, result.isSafe)
        assertEquals(0.080f, result.nsfwProbability)
        assertTrue(result.reason.contains("sfw_prob=0.920"))
    }
}
