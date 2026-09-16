import com.sun.net.httpserver.HttpServer
import org.gradle.api.logging.Logging
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Pins the metadata contract: a 429/5xx is retried once (honouring a small Retry-After) and then
 * throws instead of being parsed as a version, and a non-retryable status throws immediately.
 */
class ModrinthClientMetadataTest {

    private fun withServer(statuses: List<Int>, retryAfter: String? = null, body: (Int) -> String,
                           test: (ModrinthClient, AtomicInteger) -> Unit) {
        val hits = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val n = hits.getAndIncrement()
            val status = statuses[minOf(n, statuses.size - 1)]
            val bytes = body(status).toByteArray()
            if (retryAfter != null && status != 200) {
                exchange.responseHeaders.add("Retry-After", retryAfter)
            }
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val client = ModrinthClient(Logging.getLogger("test"), "26.3", "fabric", metadataTimeoutSeconds = 5)
            client.apiBase = "http://127.0.0.1:${server.address.port}/v2"
            test(client, hits)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `500 then 200 succeeds on the retry`() {
        withServer(listOf(500, 200), body = { if (it == 200) """{"id":"abc","project_id":"p"}""" else "boom" }) { client, hits ->
            val json = client.version("abc")
            assertEquals("abc", json.get("id").asString)
            assertEquals(2, hits.get())
        }
    }

    @Test
    fun `429 with Retry-After is honoured then succeeds`() {
        withServer(listOf(429, 200), retryAfter = "1", body = { if (it == 200) """{"id":"abc"}""" else "slow down" }) { client, hits ->
            val started = System.nanoTime()
            val json = client.version("abc")
            val elapsed = (System.nanoTime() - started) / 1_000_000_000.0
            assertEquals("abc", json.get("id").asString)
            assertEquals(2, hits.get())
            assertTrue(elapsed >= 1.0, "Retry-After: 1 must be waited for, took ${elapsed}s")
            assertTrue(elapsed < ModrinthClient.MAX_RETRY_AFTER_SECONDS + 3.0, "took ${elapsed}s")
        }
    }

    @Test
    fun `500 twice throws instead of parsing the error body`() {
        withServer(listOf(500, 500), body = { "boom" }) { client, hits ->
            val e = assertFailsWith<IOException> { client.version("abc") }
            assertTrue(e.message!!.contains("HTTP 500"), e.message)
            assertEquals(2, hits.get())
        }
    }

    @Test
    fun `404 throws immediately without retry`() {
        withServer(listOf(404), body = { "nope" }) { client, hits ->
            val e = assertFailsWith<IOException> { client.version("abc") }
            assertTrue(e.message!!.contains("HTTP 404"), e.message)
            assertEquals(1, hits.get())
        }
    }
}
