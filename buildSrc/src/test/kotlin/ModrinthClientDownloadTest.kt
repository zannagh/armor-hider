import org.gradle.api.logging.Logging
import java.io.IOException
import java.net.ServerSocket
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the download contract that keeps a smoke launch from hanging or loading a corrupt jar: a stalled
 * body must give up within the per-attempt budget, and no partial file may be left behind.
 */
class ModrinthClientDownloadTest {

    @Test
    fun `stalled body times out within budget and leaves no file`() {
        // A server that accepts the connection and never writes: connect succeeds, headers never arrive.
        ServerSocket(0).use { server ->
            val accepted = Thread {
                try {
                    server.accept().use { Thread.sleep(30_000) }
                } catch (_: Exception) {
                }
            }.apply { isDaemon = true; start() }
            val client = ModrinthClient(Logging.getLogger("test"), "26.3", "fabric", downloadAttemptSeconds = 2)
            val dir = Files.createTempDirectory("modrinth-download-test")
            val out = dir.resolve("mod.jar")
            val started = System.nanoTime()
            var failed = false
            try {
                client.download("http://127.0.0.1:${server.localPort}/mod.jar", out)
            } catch (e: IOException) {
                failed = true
            }
            val elapsedSeconds = (System.nanoTime() - started) / 1_000_000_000.0
            assertTrue(failed, "a stalled download must throw, not hang")
            // Two attempts of 2 s each, plus slack for connect/teardown.
            assertTrue(elapsedSeconds < 10.0, "download must give up within the attempt budget, took ${elapsedSeconds}s")
            assertFalse(Files.exists(out), "no jar may be left behind by a failed download")
            assertFalse(Files.exists(dir.resolve("mod.jar.part")), "no partial file may be left behind")
            accepted.interrupt()
        }
    }
}
