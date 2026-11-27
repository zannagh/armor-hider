import kotlin.test.Test
import kotlin.test.assertEquals

/** Pins how FetchCompatJars treats a pinned version that does not target the build's MC version. */
class PinAutoCorrectTest {

    private val fallbacks = setOf("flk-fallback")

    private fun decide(hash: String, resolved: String?) = PinAutoCorrect.decide(hash, resolved) { it in fallbacks }

    @Test
    fun `pin equal to the snapshot fallback build is kept`() {
        assertEquals(PinAutoCorrect.KEEP, decide("flk-fallback", "flk-fallback"))
    }

    @Test
    fun `stale pin is replaced by a different resolved version`() {
        assertEquals(PinAutoCorrect.REPLACE, decide("stale", "fresh"))
        assertEquals(PinAutoCorrect.REPLACE, decide("stale", "flk-fallback"))
    }

    @Test
    fun `pin without a resolution is skipped`() {
        assertEquals(PinAutoCorrect.SKIP, decide("stale", null))
    }

    @Test
    fun `pin resolving to itself without being a fallback is skipped`() {
        assertEquals(PinAutoCorrect.SKIP, decide("odd", "odd"))
    }
}
