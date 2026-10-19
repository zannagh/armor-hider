package de.zannagh.armorhider.smoke.paper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Turns on the plugin's {@code FINE} logging for a smoke server, so per-packet lines such as
 * {@code "Server received combat log packet from"} - demoted below INFO for production - still
 * reach {@code logs/latest.log} where the smoke tests assert on them.
 *
 * <p>Two gates have to be opened, verified against Paper 1.21.4 and 26.2:</p>
 * <ol>
 *     <li><b>JUL</b>: the plugin logger inherits the root's INFO level, so {@code fine(...)} is
 *     dropped before it leaves the plugin. {@code -Darmorhider.debug=true} makes the plugin lower
 *     its own logger to FINE. A {@code java.util.logging.config.file} must NOT be used instead: it
 *     makes the JDK initialise the stock JUL LogManager during paperclip's first-run download,
 *     before Paper installs its own, and the stock manager's shutdown hook then swallows the
 *     plugin's shutdown lines ({@code Disabling ArmorHider}) that the tests assert on.</li>
 *     <li><b>log4j</b>: Paper's {@code ForwardLogHandler} maps JUL {@code FINE} to log4j
 *     {@code TRACE}, and its {@code log4j2.xml} root is INFO. A composite configuration keeps
 *     Paper's own file ({@code classpath:log4j2.xml}) untouched and only adds an
 *     {@code ArmorHider} logger at TRACE.</li>
 * </ol>
 *
 * <p>Paper's console appender stays filtered at INFO, so the lines land in {@code latest.log} only,
 * never on the stdout pipe - read them via {@link PaperServer#getCombinedLogText()} after
 * shutdown.</p>
 */
final class PaperDebugLogging {

    /** The log4j logger name Paper gives the plugin (its {@code name:} in plugin.yml). */
    private static final String PLUGIN_LOGGER = "ArmorHider";

    private static final String LOG4J_FILE = "smoke-log4j2.xml";

    private PaperDebugLogging() {
    }

    /** Writes the log4j overlay into the server directory. */
    static void write(Path serverDirectory) throws IOException {
        Files.writeString(serverDirectory.resolve(LOG4J_FILE), String.join("\n",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<Configuration status=\"WARN\">",
                "    <Loggers>",
                "        <Logger name=\"" + PLUGIN_LOGGER + "\" level=\"trace\"/>",
                "    </Loggers>",
                "</Configuration>",
                ""), StandardCharsets.UTF_8);
    }

    /** The JVM flags that enable the plugin's debug logging and load the {@link #write} overlay. */
    static List<String> jvmArguments(Path serverDirectory) {
        return List.of(
                "-Darmorhider.debug=true",
                "-Dlog4j2.configurationFile=classpath:log4j2.xml,"
                        + serverDirectory.resolve(LOG4J_FILE).toAbsolutePath());
    }
}
