package de.zannagh.armorhider;

import com.google.common.base.Stopwatch;
import com.google.gson.GsonBuilder;
import de.zannagh.armorhider.resources.ServerConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class 
PerformanceBenchmarkTests {
    
    static int ITERATIONS_PER_PLAYER_COUNT = 250;

    @Test
    @DisplayName("Serialization Performance Benchmark - up to 1500 players")
    void testSerializationPerformance() {
        int[] playerCounts = {1, 10, 50, 100, 200, 300, 400, 500, 1000, 1500};
        var gson = new GsonBuilder().setPrettyPrinting().create();

        List<BenchmarkResult> results = new ArrayList<>();

        System.out.println("\n=== SERIALIZATION PERFORMANCE BENCHMARK ===");
        System.out.println("Running " + ITERATIONS_PER_PLAYER_COUNT + " iterations per player count...\n");

        for (int playerCount : playerCounts) {
            long[] reflectionSerializationTimes = new long[ITERATIONS_PER_PLAYER_COUNT];
            long[] regularSerializationTimes = new long[ITERATIONS_PER_PLAYER_COUNT];
            long[] reflectionDeserializationTimes = new long[ITERATIONS_PER_PLAYER_COUNT];
            long[] regularDeserializationTimes = new long[ITERATIONS_PER_PLAYER_COUNT];

            // Warmup run
            var provider = ServerConfigProviderMock.createServerConfigWithPlayers(playerCount);
            ServerConfiguration config = provider.getValue();
            String reflectionJson = config.toJson();
            String regularJson = gson.toJson(config);
            gson.fromJson(regularJson, ServerConfiguration.class);
            ArmorHider.GSON.fromJson(reflectionJson, ServerConfiguration.class);

            // Actual benchmark runs
            for (int i = 0; i < ITERATIONS_PER_PLAYER_COUNT; i++) {
                provider = ServerConfigProviderMock.createServerConfigWithPlayers(playerCount);
                config = provider.getValue();

                Stopwatch reflectionSerializationStopwatch = Stopwatch.createStarted();
                reflectionJson = config.toJson();
                reflectionSerializationStopwatch.stop();
                reflectionSerializationTimes[i] = reflectionSerializationStopwatch.elapsed(TimeUnit.NANOSECONDS);

                Stopwatch regularSerializationStopwatch = Stopwatch.createStarted();
                regularJson = gson.toJson(config);
                regularSerializationStopwatch.stop();
                regularSerializationTimes[i] = regularSerializationStopwatch.elapsed(TimeUnit.NANOSECONDS);

                Stopwatch regularDeserializationStopwatch = Stopwatch.createStarted();
                gson.fromJson(regularJson, ServerConfiguration.class);
                regularDeserializationStopwatch.stop();
                regularDeserializationTimes[i] = regularDeserializationStopwatch.elapsed(TimeUnit.NANOSECONDS);

                Stopwatch reflectionDeserializationStopwatch = Stopwatch.createStarted();
                ArmorHider.GSON.fromJson(reflectionJson, ServerConfiguration.class);
                reflectionDeserializationStopwatch.stop();
                reflectionDeserializationTimes[i] = reflectionDeserializationStopwatch.elapsed(TimeUnit.NANOSECONDS);
            }

            // Calculate statistics
            BenchmarkStats reflectionSerializationStats = calculateStats(reflectionSerializationTimes);
            BenchmarkStats regularSerializationStats = calculateStats(regularSerializationTimes);
            BenchmarkStats reflectionDeserializationStats = calculateStats(reflectionDeserializationTimes);
            BenchmarkStats regularDeserializationStats = calculateStats(regularDeserializationTimes);

            double deserializationRatio = reflectionDeserializationStats.mean / regularDeserializationStats.mean;
            double serializationRatio = reflectionSerializationStats.mean / regularSerializationStats.mean;

            results.add(new BenchmarkResult(
                    playerCount,
                    regularSerializationStats,
                    reflectionSerializationStats,
                    regularDeserializationStats,
                    reflectionDeserializationStats,
                    serializationRatio,
                    deserializationRatio
            ));

            System.out.printf("[%4d players] Deserialization:%n", playerCount);
            System.out.printf("  Regular:    %.2f ± %.2f ns (min: %.2f, max: %.2f)%n",
                    regularDeserializationStats.mean, regularDeserializationStats.stdDev,
                    regularDeserializationStats.min, regularDeserializationStats.max);
            System.out.printf("  Reflection: %.2f ± %.2f ns (min: %.2f, max: %.2f)%n",
                    reflectionDeserializationStats.mean, reflectionDeserializationStats.stdDev,
                    reflectionDeserializationStats.min, reflectionDeserializationStats.max);
            System.out.printf("  Ratio: %.2fx (reflection/regular)%n", deserializationRatio);

            System.out.printf("[%4d players] Serialization:%n", playerCount);
            System.out.printf("  Regular:    %.2f ± %.2f ns (min: %.2f, max: %.2f)%n",
                    regularSerializationStats.mean, regularSerializationStats.stdDev,
                    regularSerializationStats.min, regularSerializationStats.max);
            System.out.printf("  Reflection: %.2f ± %.2f ns (min: %.2f, max: %.2f)%n",
                    reflectionSerializationStats.mean, reflectionSerializationStats.stdDev,
                    reflectionSerializationStats.min, reflectionSerializationStats.max);
            System.out.printf("  Ratio: %.2fx (reflection/regular)%n", serializationRatio);

            assertPerformanceWithinBounds(
                    regularDeserializationStats,
                    reflectionDeserializationStats,
                    deserializationRatio,
                    "Deserialization",
                    playerCount
            );

            assertPerformanceWithinBounds(
                    regularSerializationStats,
                    reflectionSerializationStats,
                    serializationRatio,
                    "Serialization",
                    playerCount
            );

            System.out.println();
        }

        printSummaryTables(results);
    }

    /**
     * Assert performance is within bounds, accounting for statistical variance.
     * <p>
     * This method checks:
     * 1. If CV is too high (>50%), skip assertion as data is too noisy
     * 2. If 95% CV overlaps significantly, the difference is not statistically significant
     * 3. Otherwise, apply the normal ratio threshold check
     * </p>
     */
    private void assertPerformanceWithinBounds(
            BenchmarkStats regularStats,
            BenchmarkStats reflectionStats,
            double ratio,
            String operationType,
            int playerCount) {

        double regularCV = regularStats.stdDev / regularStats.mean;
        double reflectionCV = reflectionStats.stdDev / reflectionStats.mean;

        boolean intervalsOverlap = isIntervalsOverlap(regularStats, reflectionStats);

        if (regularCV > 0.5 || reflectionCV > 0.5) {
            System.out.printf("Warning: High variance for %s at %d players (Regular CV: %.1f%%, Reflection CV: %.1f%%) - skipping assertion%n",
                    operationType, playerCount, regularCV * 100, reflectionCV * 100);
            return;
        }

        if (intervalsOverlap && Math.abs(ratio - 1.0) < 1.0) {
            System.out.printf("  ℹ Info: Confidence intervals overlap for %s at %d players (ratio: %.2fx) - difference not statistically significant%n",
                    operationType, playerCount, ratio);
            return;
        }

        assertTrue(ratio < 2.5,
                String.format("%s ratio %.2fx exceeds threshold %.2fx at %d players (Regular: %.0f±%.0f ns, Reflection: %.0f±%.0f ns)",
                        operationType, ratio, 2.5, playerCount,
                        regularStats.mean, regularStats.stdDev,
                        reflectionStats.mean, reflectionStats.stdDev));
    }

    private static boolean isIntervalsOverlap(BenchmarkStats regularStats, BenchmarkStats reflectionStats) {
        double ciDivisor = Math.sqrt(ITERATIONS_PER_PLAYER_COUNT);
        double regularCI = 1.96 * regularStats.stdDev / ciDivisor;
        double reflectionCI = 1.96 * reflectionStats.stdDev / ciDivisor;

        double regularLower = regularStats.mean - regularCI;
        double regularUpper = regularStats.mean + regularCI;
        double reflectionLower = reflectionStats.mean - reflectionCI;
        double reflectionUpper = reflectionStats.mean + reflectionCI;

        // Confidence interval overlap
        return !(reflectionLower > regularUpper || regularLower > reflectionUpper);
    }

    private void printSummaryTables(List<BenchmarkResult> results) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DESERIALIZATION PERFORMANCE SUMMARY");
        System.out.println("=".repeat(80));
        System.out.printf("%-10s | %-20s | %-20s | %-8s | %-10s%n",
                "Players", "Regular (ns)", "Reflection (ns)", "Ratio", "Overhead");
        System.out.println("-".repeat(80));

        double totalDeserializationOverhead = 0;
        for (BenchmarkResult result : results) {
            double overhead = (result.deserializationRatio - 1) * 100;
            totalDeserializationOverhead += overhead;
            System.out.printf("%-10d | %10.0f ± %7.0f | %10.0f ± %7.0f | %8.2fx | %+9.1f%%%n",
                    result.playerCount,
                    result.regularDeserialization.mean, result.regularDeserialization.stdDev,
                    result.reflectionDeserialization.mean, result.reflectionDeserialization.stdDev,
                    result.deserializationRatio,
                    overhead);
        }
        System.out.println("-".repeat(80));
        System.out.printf("Average overhead: %.1f%%%n", totalDeserializationOverhead / results.size());

        System.out.println("\n" + "=".repeat(80));
        System.out.println("SERIALIZATION PERFORMANCE SUMMARY");
        System.out.println("=".repeat(80));
        System.out.printf("%-10s | %-20s | %-20s | %-8s | %-10s%n",
                "Players", "Regular (ns)", "Reflection (ns)", "Ratio", "Speedup");
        System.out.println("-".repeat(80));

        double totalSerializationSpeedup = 0;
        for (BenchmarkResult result : results) {
            double speedup = (1 - result.serializationRatio) * 100;
            totalSerializationSpeedup += speedup;
            System.out.printf("%-10d | %10.0f ± %7.0f | %10.0f ± %7.0f | %8.2fx | %+9.1f%%%n",
                    result.playerCount,
                    result.regularSerialization.mean, result.regularSerialization.stdDev,
                    result.reflectionSerialization.mean, result.reflectionSerialization.stdDev,
                    result.serializationRatio,
                    speedup);
        }
        System.out.println("-".repeat(80));
        System.out.printf("Average speedup: %.1f%%%n", totalSerializationSpeedup / results.size());
        System.out.println("=".repeat(80) + "\n");
    }

    private static class BenchmarkResult {
        int playerCount;
        BenchmarkStats regularSerialization;
        BenchmarkStats reflectionSerialization;
        BenchmarkStats regularDeserialization;
        BenchmarkStats reflectionDeserialization;
        double serializationRatio;
        double deserializationRatio;

        BenchmarkResult(int playerCount,
                        BenchmarkStats regularSerialization,
                        BenchmarkStats reflectionSerialization,
                        BenchmarkStats regularDeserialization,
                        BenchmarkStats reflectionDeserialization,
                        double serializationRatio,
                        double deserializationRatio) {
            this.playerCount = playerCount;
            this.regularSerialization = regularSerialization;
            this.reflectionSerialization = reflectionSerialization;
            this.regularDeserialization = regularDeserialization;
            this.reflectionDeserialization = reflectionDeserialization;
            this.serializationRatio = serializationRatio;
            this.deserializationRatio = deserializationRatio;
        }
    }

    private static class BenchmarkStats {
        double mean;
        double stdDev;
        double min;
        double max;
    }

    private BenchmarkStats calculateStats(long[] times) {
        BenchmarkStats stats = new BenchmarkStats();

        // Calculate mean
        long sum = 0;
        stats.min = Long.MAX_VALUE;
        stats.max = Long.MIN_VALUE;

        for (long time : times) {
            sum += time;
            if (time < stats.min) stats.min = time;
            if (time > stats.max) stats.max = time;
        }
        stats.mean = (double) sum / times.length;

        // Calculate standard deviation
        double varianceSum = 0;
        for (long time : times) {
            double diff = time - stats.mean;
            varianceSum += diff * diff;
        }
        stats.stdDev = Math.sqrt(varianceSum / times.length);

        return stats;
    }
}
