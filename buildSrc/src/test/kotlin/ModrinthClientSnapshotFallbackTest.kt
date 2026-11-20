import com.sun.net.httpserver.HttpServer
import org.gradle.api.logging.Logging
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the snapshot fallback: an allowlisted MC-agnostic dep with no build tagged for the snapshot resolves
 * to its newest build for the newest release game version; any other project still resolves to nothing.
 */
class ModrinthClientSnapshotFallbackTest {

    private val flk = "Ha28R6CL"

    private fun withServer(test: (ModrinthClient) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val uri = exchange.requestURI.toString()
            val body = when {
                uri.startsWith("/v2/tag/game_version") ->
                    """[{"version":"26.4-snapshot-10","version_type":"snapshot"},{"version":"26.3","version_type":"release"}]"""
                uri.contains("26.3") ->
                    """[{"id":"beta1","version_type":"beta"},{"id":"rel1","version_type":"release"}]"""
                else -> "[]"
            }.toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val client = ModrinthClient(Logging.getLogger("test"), "26.4-snapshot-10", "fabric", metadataTimeoutSeconds = 5)
            client.apiBase = "http://127.0.0.1:${server.address.port}/v2"
            test(client)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `allowlisted dep falls back to the newest release build`() {
        withServer { client ->
            assertEquals("rel1", client.latestForProject(flk, "test"))
            assertTrue(client.isSnapshotFallback("rel1"))
        }
    }

    @Test
    fun `other projects get no fallback`() {
        withServer { client ->
            assertNull(client.latestForProject("someMod", "test"))
            assertFalse(client.isSnapshotFallback("rel1"))
        }
    }
}
