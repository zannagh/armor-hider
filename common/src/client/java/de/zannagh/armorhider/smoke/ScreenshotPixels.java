//? if fcgt {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.IntPredicate;

/**
 * Pixel inspection for FCGT screenshots - the machine check for rendering defects that call counters
 * cannot see (a wrong tint still increments every counter).
 * <p>
 * {@link #countMatching(Path, IntPredicate)} decodes a PNG written by
 * {@code ClientGameTestContext.takeScreenshot(String)} and counts the pixels whose packed
 * {@code 0xRRGGBB} value satisfies a predicate; {@link #red(int)}, {@link #green(int)} and
 * {@link #blue(int)} unpack the channels for readable predicates at the call site. Test-only code,
 * running in the client JVM on the gametest thread, so plain {@link ImageIO} is enough - no MC or
 * GL API is touched, which keeps this file safe across every stonecutter variant.
 * <p>
 * FCGT writes the file synchronously before {@code takeScreenshot} returns (its gametest thread
 * keeps ticking until the save future completes), so the returned path is readable immediately.
 * We do not rely on that: {@link #countMatching} retries for a bounded window, so a future FCGT
 * that moves the write off-thread degrades to a short wait rather than a spurious red.
 */
public final class ScreenshotPixels {

    /** How long to wait for a screenshot to become readable before giving up. */
    private static final long READ_TIMEOUT_MILLIS = 2_000L;

    /** Poll interval while waiting for a screenshot file to appear and decode. */
    private static final long READ_POLL_MILLIS = 50L;

    private ScreenshotPixels() {
    }

    /** Red channel of a packed {@code 0xRRGGBB} pixel. */
    public static int red(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    /** Green channel of a packed {@code 0xRRGGBB} pixel. */
    public static int green(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    /** Blue channel of a packed {@code 0xRRGGBB} pixel. */
    public static int blue(int rgb) {
        return rgb & 0xFF;
    }

    /**
     * Counts the pixels of {@code screenshot} matching {@code rgbPredicate}, over the whole frame.
     *
     * @param screenshot   the PNG path returned by {@code ClientGameTestContext.takeScreenshot}
     * @param rgbPredicate tested against each pixel's packed {@code 0xRRGGBB} value (alpha stripped)
     * @return the number of matching pixels
     * @throws IllegalStateException if the screenshot never became readable
     */
    public static int countMatching(Path screenshot, IntPredicate rgbPredicate) {
        BufferedImage image = readWithBoundedWait(screenshot);
        int width = image.getWidth();
        int height = image.getHeight();
        int matches = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (rgbPredicate.test(image.getRGB(x, y) & 0xFFFFFF)) {
                    matches++;
                }
            }
        }
        ArmorHider.LOGGER.info("[smoke/fcgt] pixel scan {} ({}x{}): {} matching", screenshot, width, height, matches);
        return matches;
    }

    /**
     * Reads {@code screenshot}, tolerating a not-yet-complete file for up to
     * {@value #READ_TIMEOUT_MILLIS} ms. A partially written PNG makes {@link ImageIO#read} either
     * return {@code null} or throw, so both are treated as "not ready yet" and retried.
     */
    private static BufferedImage readWithBoundedWait(Path screenshot) {
        long deadline = System.currentTimeMillis() + READ_TIMEOUT_MILLIS;
        IOException lastFailure = null;
        while (true) {
            try {
                if (Files.isRegularFile(screenshot) && Files.size(screenshot) > 0L) {
                    BufferedImage image = ImageIO.read(screenshot.toFile());
                    if (image != null) {
                        return image;
                    }
                }
            } catch (IOException e) {
                lastFailure = e;
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new IllegalStateException("[smoke/fcgt] screenshot " + screenshot + " was not readable within "
                        + READ_TIMEOUT_MILLIS + "ms - the frame could not be inspected", lastFailure);
            }
            try {
                Thread.sleep(READ_POLL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("[smoke/fcgt] interrupted while waiting for screenshot " + screenshot, e);
            }
        }
    }
}
//?}
